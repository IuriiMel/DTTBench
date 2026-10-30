package com.terminal.handlers;

    /**
     * Defines the properties of a command used for matching and handling.
     */
public interface CommandDefinition {

    /**
     * @return the command prefix used to identify the command.
     */
    String getPrefix();

    /**
     * @return true if the command expects a response from the device.
     */
    boolean expectsResponse();

    /**
     * Higher values indicate more specific commands.
     * @return a numeric specificity score for the command.
     */
    int getSpecificity();
}
