package com.terminal.utils;

import com.terminal.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UtilsRunner {

    private static final Map<Path, RunningUtils> runningUtils = new ConcurrentHashMap<>();
    private static final String[] EXTENSIONS = {".shortcut", ".lnk", ".exe", ".bat", ".cmd", ".jar"};
    private static final Logger log = LoggerFactory.getLogger(UtilsRunner.class);

    public static class RunningUtils{
        final ProcessHandle process;
        final String name;
        final String extension;

        RunningUtils(ProcessHandle process, String name, String extension){
            this.process = process;
            this.name = name;
            this.extension = extension;
        }
        public ProcessHandle getProcess(){
            return process;
        }
        public String getName(){
            return name;
        }
        public String getExtension(){
            return extension;
        }
    }

    public static boolean run(String name){
        Path path = resolveUtilityPath(name);
        if(path == null) return false;
        if(!Files.exists(path)) return false;

        String filename = path.getFileName().toString();
        String extension = filename.substring(filename.lastIndexOf('.'));

        try{
            ProcessBuilder builder;
            Process process;
            String title = "\"" + name + extension + "\"";
            if(path.toString().endsWith(".lnk") || path.toString().endsWith(".cmd") || path.toString().endsWith(".bat")){
                builder = new ProcessBuilder("cmd.exe", "/c", "start", title, path.toString());
            }
            else if(path.toString().endsWith(".jar")){
                builder = new ProcessBuilder("cmd.exe", "/c", "start", title, "java", "-jar", path.toString());
            }
            else{
                builder = new ProcessBuilder(path.toString());
            }
            process = builder.start();
            runningUtils.put(path, new RunningUtils(process.toHandle(), name, extension));
            return true;
        }
        catch(Exception e){
            String message = "Filed to run util " + name;
            log.error(message, e);
            return false;
        }
    }

    public static boolean stop(String name){
        Path file = resolveUtilityPath(name);

        if(file == null) return false;
        if(runningUtils.get(file) == null) return false;

        ProcessHandle handle = runningUtils.remove(file).getProcess();

        handle.descendants().forEach(processHandle -> {
            if(processHandle.isAlive()) processHandle.destroyForcibly();
        });

        if(handle.isAlive()) handle.destroyForcibly();
        return true;
    }

    private static Path resolveUtilityPath(String name) {
        Path dir = Config.getUtilsDirectory();
        Path direct = dir.resolve(name);

        if (Files.exists(direct))
            return direct;

        for (String extension : EXTENSIONS) {
            Path path = dir.resolve(name + extension);
            if (Files.exists(path))
                return path;
        }

        return null;
    }
}
