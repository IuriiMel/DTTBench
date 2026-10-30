package com.terminal.handlers.devices;

import com.terminal.handlers.CommandHandler;
import com.terminal.handlers.CommandDefinition;
import com.terminal.handlers.CommandResult;
import com.terminal.handlers.EnumCommandMatcher;
import com.terminal.port.SessionListener.LogType;
import com.terminal.utils.ByteUtils;

import java.util.Map;

/**
 * BH1745NUC - Digital 16bit Serial Output Type Color Sensor IC.
 */
public class BH1745NUCHandler implements CommandHandler {

    private final EnumCommandMatcher<Command> matcher = new EnumCommandMatcher<>(Command.values());

    @Override
    public boolean supports(String command) {
        return matcher.supports(command);
    }

    @Override
    public int getResponseDelayMs() {
        return 200;
    }

    @Override
    public int specificity(String command) {
        return matcher.specificity(command);
    }

    @Override
    public boolean expectsResponse(String command){
        return matcher.expectsResponse(command);
    }

    @Override
    public CommandResult process(byte[] readMessage, String command, Map<String, String> args) {
        CommandResult result = new CommandResult();
        result.setType(LogType.RX);
        String rxData = ByteUtils.bytesToHex(readMessage);

        result.setFormattedData(rxData);
        return result;
    }

    public enum Command implements CommandDefinition {
        I2C_38_WRITE("70", false),
        I2C_38_READ("71", true),
        I2C_39_WRITE("72", false),
        I2C_39_READ("73", true);

        private final String prefix;
        private final boolean expectsResponse;
        private final int specificity;

        Command(String prefix, boolean expectsResponse) {
            this.prefix = prefix;
            this.expectsResponse = expectsResponse;
            this.specificity = prefix.split(" ").length;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public boolean expectsResponse() {
            return expectsResponse;
        }

        @Override
        public int getSpecificity() {
            return specificity;
        }
    }
}
