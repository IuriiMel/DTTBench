package com.terminal.handlers;

import com.terminal.handlers.devices.SC18IMHandler;
import com.terminal.handlers.devices.BH1745NUCHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry of {@link CommandHandler} implementations.
 *
 * <p>Queries handlers in registration order and returns the first one
 * that supports the given command. Falls back to {@link DefaultCommandHandler}
 * if no specialized handler matches.</p>
 */
public class HandlerRegistry {
    private final List<CommandHandler> handlers = new ArrayList<>();

    public HandlerRegistry() {
        handlers.add(new SC18IMHandler());
        handlers.add(new BH1745NUCHandler());
    }

    /**
     * Returns the handler with the highest specificity for the given command.
     * If multiple handlers have the same specificity, the first registered handler
     * takes priority.
     * Falls back to {@link DefaultCommandHandler} if no handler supports the command.
     *
     * @param command the command to find a handler for
     * @return the best matching handler, or a default {@link DefaultCommandHandler}
     *         if no match is found
     */
    public CommandHandler getHandler(String command) {
        CommandHandler bestHandler = null;
        int bestSpecificity = -1;
        for (CommandHandler handler : handlers) {
            int specificity = handler.specificity(command);
            if (specificity > bestSpecificity) {
                bestSpecificity = specificity;
                bestHandler = handler;
            }
        }
        return bestHandler != null ? bestHandler : new DefaultCommandHandler();
    }
}
