package com.terminal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Provides access to application build metadata.
 *
 * <p>Loads properties from {@code build-info.properties} on the classpath
 * during class initialization. Methods return safe fallback values if the
 * resource is missing or cannot be read.</p>
 */
public class BuildInfo {

    private static final Properties props = new Properties();
    private static final Logger log = LoggerFactory.getLogger(BuildInfo.class);

    static {
        try (InputStream stream = BuildInfo.class.getClassLoader().getResourceAsStream("build-info.properties")) {
            if (stream != null) props.load(stream);
        }
        catch (Exception e) {
            String message = "Error to load build info";
            log.error(message, e);
        }
    }

    public static String getVersion() {
        return props.getProperty("app.version", "unknown");
    }

    public static String getBuildTime() {
        return props.getProperty("build.time", "unknown");
    }

    public static String getAppName() {
        return props.getProperty("app.name", "DTTBench");
    }
}