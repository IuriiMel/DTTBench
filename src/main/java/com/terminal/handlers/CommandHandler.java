package com.terminal.handlers;

import java.util.Map;

/**
 * Contract for parsing and processing device responses.
 */

public interface CommandHandler {

    /**
     * @return true if this handler can process the given command.
     */
    boolean supports(String command);

    /**
     * @return expected delay in milliseconds before reading the response.
     */
    int getResponseDelayMs();

    /**
     * Returns a score indicating how specifically this handler matches
     * the given command.
     *
     * <p>Higher scores take precedence over lower scores. A negative score
     * indicates that the handler does not support the command.
     * The default implementation assigns the same score to all supported
     * commands.</p>
     *
     * @param command the command to evaluate
     * @return the match score, or a negative value if the command is not supported
     */
    default int specificity(String command) {
        return supports(command) ? 0 : -1;
    }

    /**
     * @return true if the command expects a response from the device.
     */
    boolean expectsResponse(String command);

    /**
     * Processes raw response bytes and extracts meaningful data.
     *
     * @return result object containing formatted data, plot values, and error status.
     */
    CommandResult process(byte[] readMessage, String command, Map<String, String> args);
}
