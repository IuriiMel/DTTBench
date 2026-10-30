package com.terminal.config;

import com.terminal.model.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Manages application configuration, directory initialization, and dynamic logging setup.
 *
 * <p>On startup, ensures required directories exist and loads {@code config.txt}.
 * If a custom {@code logback.xml} is present in the config directory, it dynamically
 * reconfigures the global SLF4J/Logback context to apply external logging rules.</p>
 */
public final class Config {

    private static final String CONFIG_DIR_NAME = "Config";
    private static final String APP_LOGS_DIR_NAME = "AppLogs";
    private static final String CONFIG_FILE_NAME = "config.txt";
    private static final String SYNTAX_FILE_NAME = "syntax.txt";
    private static final String LOGBACK_FILE_NAME = "logback.xml";

    private static final String DEFAULT_SCRIPTS_DIR = "Scripts";
    private static final String DEFAULT_LOGS_DIR = "Logs";
    private static final String DEFAULT_UTILS_DIR = "Utils";

    private static Path exeDirectory;
    private static Path configDirectory;
    private static Path appLogsDirectory;
    private static Path configFile;
    private static Path syntaxFile;

    private static Path scriptsDirectory;
    private static Path logsDirectory;
    private static Path utilsDirectory;

    private static Logger log;

    /**
     * Initializes the application environment and configuration paths.
     *
     * <p><b>Side effects:</b></p>
     * <ul>
     *   <li>Creates default directories ({@code Config}, {@code AppLogs}, etc.) if missing.</li>
     *   <li>Generates {@code config.txt} with default values if it does not exist.</li>
     *   <li>Dynamically resets and reconfigures the global Logback {@link LoggerContext}
     *       if {@code logback.xml} is found, overriding default logging behavior.</li>
     * </ul>
     *
     * <p>Must be called once during application startup before any logging or file operations occur.</p>
     */
    public static void load() {
        try {
            exeDirectory = Path.of(System.getProperty("user.dir"));
            configDirectory = exeDirectory.resolve(CONFIG_DIR_NAME);
            configFile = configDirectory.resolve(CONFIG_FILE_NAME);
            syntaxFile = exeDirectory.resolve(SYNTAX_FILE_NAME);
            appLogsDirectory = exeDirectory.resolve(APP_LOGS_DIR_NAME);

            Files.createDirectories(configDirectory);
            Files.createDirectories(appLogsDirectory);

            Path logbackFile = configDirectory.resolve(LOGBACK_FILE_NAME);
            if (Files.exists(logbackFile)) {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.reset();

                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                configurator.doConfigure(logbackFile.toFile());
                log = LoggerFactory.getLogger(Config.class);
                log.info("External logging configuration loaded from: {}", logbackFile.toAbsolutePath());
            }
            else {
                log = LoggerFactory.getLogger(Config.class);
                log.info("No external logback.xml found. Using default logging settings.");
            }

            Properties properties = new Properties();
            if (Files.notExists(configFile)) {
                setDefaultProperties(properties);
                try(Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)){
                    properties.store(writer, "DTTBench configuration");
                }
            }
            else{
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)){
                    properties.load(reader);
                }
            }

            scriptsDirectory = exeDirectory.resolve(properties.getProperty("ScriptsDIR", DEFAULT_SCRIPTS_DIR));
            logsDirectory = exeDirectory.resolve(properties.getProperty("LogsDIR", DEFAULT_LOGS_DIR));
            utilsDirectory = exeDirectory.resolve(properties.getProperty("UtilsDIR", DEFAULT_UTILS_DIR));

            Files.createDirectories(scriptsDirectory);
            Files.createDirectories(logsDirectory);
            Files.createDirectories(utilsDirectory);
        }
        catch (Exception e){
            String message = "Failed to initialize configuration.";
            log.error(message, e);
            AlertService.alertError(e, "Configuration error", message);
        }
    }

    private static void setDefaultProperties(Properties properties) {
        properties.setProperty("ScriptsDIR", DEFAULT_SCRIPTS_DIR);
        properties.setProperty("LogsDIR", DEFAULT_LOGS_DIR);
        properties.setProperty("UtilsDIR", DEFAULT_UTILS_DIR);
    }

    /**
     * Opens the configuration file in the default system text editor.
     *
     * <p><b>Note:</b> Currently uses Windows-specific {@code cmd /c start} command.</p>
     */
    public static void showConfig() {
        try {
            new ProcessBuilder( "cmd", "/c", "start", "", configFile.toString()).start();
        }
        catch(Exception e){
            String message = "Failed to open the configuration file.";
            log.error(message, e);
            AlertService.alertError(e, "Configuration error", message);
        }
    }

    /**
     * Opens the syntax definition file in the default system text editor.
     *
     * <p><b>Note:</b> Currently uses Windows-specific {@code cmd /c start} command.</p>
     */
    public static void showSyntax() {
        try {
            new ProcessBuilder( "cmd", "/c", "start", "", syntaxFile.toString()).start();
        }
        catch(Exception e){
            String message = "Failed to open the syntax file.";
            log.error(message, e);
            AlertService.alertError(e, "Open file error", message);
        }
    }

    public static Path getScriptsDirectory() {
        return scriptsDirectory;
    }

    public static Path getLogsDirectory() {
        return logsDirectory;
    }

    public static Path getUtilsDirectory() {
        return utilsDirectory;
    }
}