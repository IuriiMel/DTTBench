package com.terminal.handlers.devices;

import com.terminal.handlers.CommandHandler;
import com.terminal.handlers.CommandDefinition;
import com.terminal.handlers.CommandResult;
import com.terminal.handlers.EnumCommandMatcher;
import com.terminal.port.SessionListener.LogType;
import com.terminal.utils.ByteUtils;

import java.util.Map;

/**
 * SC18IM700 - Master I2C-bus controller with UART interface.
 */
public class SC18IMHandler implements CommandHandler {

    private final EnumCommandMatcher<Command> matcher = new EnumCommandMatcher<>(Command.values());

    @Override
    public boolean supports(String command) {
        return matcher.supports(command);
    }

    @Override
    public int getResponseDelayMs() {
        return 100;
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
        I2C_START("53", false),
        I2C_STOP("50", false),
        READ_REGISTER("52", true),
        WRITE_REGISTER("57", false),
        READ_GPIO("49", true),
        WRITE_GPIO("4F", false),
        POWER_DOWN("5A", false);

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
