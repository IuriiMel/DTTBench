package com.terminal.script;

import com.terminal.handlers.CommandHandler;
import com.terminal.handlers.CommandResult;
import com.terminal.handlers.HandlerRegistry;
import com.terminal.utils.ByteUtils;
import com.terminal.utils.UtilsRunner;
import com.terminal.plotter.PlotterManager;
import com.terminal.port.PortID;
import com.terminal.port.PortManager;
import com.terminal.port.PortSession;
import com.terminal.port.SessionListener;
import com.terminal.port.SessionListener.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes command scripts in a dedicated background thread.
 *
 * <p>Supports batch execution (with repetitions and intervals), step-by-step execution,
 * and custom script syntax (e.g., signal control, utility management, loops).</p>
 *
 * <p><b>Thread Safety:</b> This class operates on a background thread.
 * It uses {@link AtomicBoolean} for safe cancellation. All events are transmitted
 * via {@link SessionListener}, which expects to be called from this background thread.</p>
 */
public class ScriptEngine {
    private final PortManager portManager;
    private final SessionListener listener;
    private final HandlerRegistry registry;
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
    private static final Logger log = LoggerFactory.getLogger(ScriptEngine.class);

    private final String nameRTS = SessionListener.ControlSignal.RTS.name();
    private final String nameDTR = SessionListener.ControlSignal.DTR.name();

    private Thread executionThread;

    /**
     * Script execution state flag. Allows the UI thread to signal cancellation to the background thread.
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public ScriptEngine(PortManager portManager, SessionListener listener, HandlerRegistry registry) {
        this.portManager = portManager;
        this.listener = listener;
        this.registry = registry;
    }

    /**
     * Executes a list of commands in a background thread.
     *
     * @param commands the list of command strings to execute
     * @param times    number of repetitions
     * @param interval delay in milliseconds between repetitions
     * @param portID   the target port for execution
     */
    public void execute(List<String> commands, int times, int interval, PortID portID) {
        if (isRunning.get()) return;
        isRunning.set(true);
        listener.scriptStateChanged(portID, true);

        executionThread = new Thread(() -> {
            try {
                int tempTimes = times;
                for (int i = 0; i < times; i++) {
                    if (!isRunning.get()) break;

                    if (times > 1) {
                        listener.logMessage(portID, "Block started: " + (times - tempTimes + 1), LogType.BLOCK_END, LocalDateTime.now());
                        listener.counterUpdate(portID, tempTimes - 1);
                    }
                    tempTimes--;

                    executeBlock(commands, portID);

                    if (interval > 0 && i < times - 1) {
                        listener.logMessage(portID, "Interval: " + interval + "ms\n", LogType.BLOCK_END, LocalDateTime.now());
                        Thread.sleep(interval);
                    }
                }
            }
            catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch(Exception e){
                log.error("Error executing script", e);
            }
            finally {
                isRunning.set(false);
                listener.scriptStateChanged(portID, false);
                if (times > 1) listener.counterUpdate(portID, 0);
            }
        });
        executionThread.start();
    }

    /**
     * Executes a single command in a new background thread.
     * Ignores empty lines and comments.
     *
     * @param command the command string to execute
     * @param portID  the target port for execution
     */
    public void executeStep(String command, PortID portID) {
        if (command.trim().isEmpty() || command.startsWith("//") || command.startsWith("(") || command.startsWith(")")) return;
        new Thread(() -> {
            try {
                processCommand(command, portID);
            }
            catch (Exception e) {
                String message = "Step error: ";
                log.error(message, e);
                listener.logMessage(portID, message + e.getMessage(), LogType.ERROR, LocalDateTime.now());
            }
        }).start();
    }

    /**
     * Requests termination of the currently running script.
     */
    public void stop() {
        isRunning.set(false);
        if (executionThread != null) {
            executionThread.interrupt();
        }
    }

    /**
     * Processes a list of commands, supporting custom block syntax:
     * <ul>
     *    <li>{@code //} for line comments</li>
     *   <li>{@code /* ... * /} for block comments</li>
     *   <li>{@code ( ...  )-r N} for repeating a block of commands N times</li>
     * </ul>
     */
    private void executeBlock(List<String> commands, PortID portID) throws InterruptedException {
        List<String> repeatedCommands = new ArrayList<>();
        AtomicBoolean blockStart = new AtomicBoolean(false);
        int repTimesBlock = 1;
        boolean excludeBlock = false;

        for (String cmd : commands) {
            if (!isRunning.get()) return;
            if (cmd.trim().isEmpty() || cmd.startsWith("//")) continue;

            if (cmd.startsWith("/*")) excludeBlock = true;

            if (!excludeBlock) {
                if (cmd.startsWith("(")) {
                    blockStart.set(true);
                    continue;
                }
                if (cmd.startsWith(")")) {
                    blockStart.set(false);
                    String[] parts = cmd.split("\\s+");
                    if (parts.length > 1) repTimesBlock = Integer.parseInt(parts[1]);

                    for (int i = 0; i < repTimesBlock; i++) {
                        for (String blockCmd : repeatedCommands) {
                            processCommand(blockCmd, portID);
                        }
                    }
                    repeatedCommands.clear();
                    repTimesBlock = 1;
                    continue;
                }
                if (blockStart.get()) {
                    repeatedCommands.add(cmd);
                } else {
                    processCommand(cmd, portID);
                }
            }
            if (cmd.endsWith("*/")) excludeBlock = false;
        }
    }

    /**
     * Processes custom script commands. Uses syntax described in <b>syntax.txt</b>
     * and delegates to the appropriate execution logic.
     */
    private void processCommand(String command, PortID portID) throws InterruptedException {
        command = command.trim();
        Map<String, String> args = parseArguments(command);
        LocalDateTime now = LocalDateTime.now();
        String time = "[" + FORMATTER.format(now) + "] ";

        if (command.toUpperCase().startsWith("COM")) {
            int space = command.indexOf(" ");
            int lBracket = command.indexOf("(");
            int rBracket = command.indexOf(")");
            int separator = command.indexOf(":");
            String com = command.substring(0, separator).trim();

            PortID targetId = findPortID(com);
            if (targetId == null) {
                listener.logMessage(portID, com + " not found or not open", LogType.ERROR, now);
                return;
            }

            now = LocalDateTime.now();
            time = "[" + FORMATTER.format(now) + "] ";

            if (lBracket > 0 && rBracket > lBracket && space > 0 && space < lBracket) {
                String signal = command.substring(space + 1, lBracket);
                String inside = command.substring(lBracket + 1, rBracket);

                if (signal.equalsIgnoreCase(nameRTS)) {
                    setSignal("RTS(" + inside + ")", targetId, time);
                    return;
                } else if (signal.equalsIgnoreCase(nameDTR)) {
                    setSignal("DTR(" + inside + ")", targetId, time);
                    return;
                }
                command = inside.trim();
            }
            else command = command.substring(separator + 1).trim();

            sendCommand(command, targetId, time, args);
        }
        else if (regionMatches(command, "Send")) {
            String inside = command.substring(command.indexOf('(') + 1, command.indexOf(')'));
            sendCommand(inside, portID, time, args);
        }
        else if (regionMatches(command, "Sleep")) {
            int delay = Integer.parseInt(command.substring(command.indexOf('(') + 1, command.indexOf(')')));
            listener.logMessage(portID, "Sleep " + delay + "ms", LogType.SYSTEM, now);
            Thread.sleep(delay);
        }
        else if (command.startsWith("RTS") || command.startsWith("DTR")) {
            setSignal(command, portID, time);
        }
        else if (command.startsWith("-c ")) {
            listener.logMessage(portID, "COMMENT: " + command.substring(3), LogType.SYSTEM, now);
        }
        else if (command.startsWith("-space")) {
            listener.logMessage(portID, "\n", LogType.SPACE, now);
        }
        else if (command.startsWith("-run")) {
            manageUtil(portID, command.substring(command.indexOf(' ') + 1).trim(), true, now);
        }
        else if (command.startsWith("-stop")) {
            manageUtil(portID, command.substring(command.indexOf(' ') + 1).trim(), false, now);
        }
        else if (command.startsWith("-open")) {
            String[] parts = command.trim().split("\\s+");
            String portName = "COM" + parts[1].toUpperCase();
            int baud = parts.length > 2 ? Integer.parseInt(parts[2]) : 9600;
            PortSession session = portManager.findAndOpen(portID, portName, baud);

            if(session == null) listener.logMessage(portID, portName + " does not exist", LogType.SYSTEM, now);
        }
        else if (command.startsWith("-close")){
            String[] parts = command.trim().split("\\s+");
            String portName = "COM" + parts[1].toUpperCase();
            boolean result = portManager.findAndClose(portName);
            if(!result) listener.logMessage(portID, portName + " does not exist", LogType.SYSTEM, now);
        }
        else {
            sendCommand(command, portID, time, args);
        }
    }

    private void sendCommand(String command, PortID portID, String time, Map<String, String> args) throws InterruptedException {
        PortSession session = portManager.getSession(portID);
        if (!session.isOpen()) {
            listener.logMessage(portID, "Port not open", LogType.ERROR, LocalDateTime.now());
            stop();
            return;
        }

        String cmd = command.contains("-") ? command.substring(0, command.indexOf("-")) : command;

        byte[] writeMessage;
        if (cmd.toUpperCase().startsWith("AT")) {
            writeMessage = ByteUtils.asciiToBytes(cmd);
        } else {
            writeMessage = ByteUtils.hexToBytes(cmd);
        }

        int reps = 1;
        if (args != null && args.containsKey("r")) {
            reps = Integer.parseInt(args.get("r"));
        }

        while (reps > 0) {
            if (!isRunning.get()) return;
            listener.logMessage(portID, "TX: " + cmd, LogType.TX, LocalDateTime.now());

            session.send(writeMessage);

            CommandHandler handler = registry.getHandler(cmd);

            if (handler != null && handler.expectsResponse(command)) {
                if (handler.getResponseDelayMs() > 0) {
                    Thread.sleep(handler.getResponseDelayMs());
                }

                CommandResult result = handler.process(session.receive(), cmd, args);

                if (result != null) {
                    if (result.getPlotValue() != null) {
                        PlotterManager.addData(portID, result.getComment(), result.getPlotValue(), result.getPlotUnit());
                    }
                    listener.logMessage(portID, "RX: " + result.getFormattedData(), result.getType(), LocalDateTime.now());
                }
            }
            reps--;
        }
    }

    private void setSignal(String cmd, PortID portId, String time) {
        String signal = cmd.startsWith(nameRTS) ? nameRTS : nameDTR;
        String valStr = cmd.substring(cmd.indexOf('(') + 1, cmd.indexOf(')'));
        boolean state = valStr.equals("1");

        PortSession session = portManager.getSession(portId);
        if (signal.equals(nameRTS)) session.setRTS(state);
        else session.setDTR(state);

        listener.logMessage(portId, "TX: " + signal + " " + valStr, LogType.TX, LocalDateTime.now());
        listener.setControlSignal(portId, signal, state);
    }

    private void manageUtil(PortID portID, String name, boolean start, LocalDateTime time) {
        boolean success = start ? UtilsRunner.run(name) : UtilsRunner.stop(name);
        String message = (start ? "RUNNING" : "STOPPING") + " the Util: " + name + (success ? "" : " (not found)");
        LogType type = start ? LogType.UTIL_STARTED : LogType.UTIL_STOPPING;
        type = success ? type : LogType.ERROR;
        listener.utilEvent(portID, message, type, time);
    }

    private PortID findPortID(String portName) {
        String targetName = portName.toUpperCase();
        for (Map.Entry<PortID, PortSession> entry : portManager.getSessions().entrySet()) {
            if (entry.getValue().isOpen() && entry.getValue().getPortName().equalsIgnoreCase(targetName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Map<String, String> parseArguments(String cmd) {
        Map<String, String> argv = new HashMap<>();
        int index = cmd.indexOf("-");
        int start = cmd.indexOf("(");
        int end = cmd.indexOf(")");
        if (cmd.contains("-")) {
            String command;
            if (start >= 0 && end > start) {
                command = cmd.substring(start + 1, end);
            } else {
                command = cmd.substring(index).trim();
            }
            String[] args = command.split("\\s+");
            String currentKey = null;
            StringBuilder currentValue = new StringBuilder();
            for (String arg : args) {
                if (arg.startsWith("-")) {
                    if (currentKey != null) argv.put(currentKey, currentValue.toString().trim());
                    currentKey = arg.substring(1);
                    currentValue.setLength(0);
                } else {
                    currentValue.append(arg).append(" ");
                }
            }
            if (currentKey != null) argv.put(currentKey, currentValue.toString().trim());
            return argv;
        }
        return null;
    }

    public boolean regionMatches(String command, String key){
        return command.regionMatches(true, 0, key, 0, key.length());
    }
}
