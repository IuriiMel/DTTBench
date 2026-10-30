package com.terminal.handlers;

import java.util.Locale;

/**
 * Helper class for enum-based command matching.
 *
 * <p>Used by handlers that have an enum implementing {@link CommandDefinition}.</p>
 */
public class EnumCommandMatcher<Command extends Enum<Command> & CommandDefinition> {

    private final Command[] commands;

    public EnumCommandMatcher(Command[] commands) {
        this.commands = commands;
    }

    public boolean supports(String command) {
        return findCommand(command) != null;
    }

    public int specificity(String command) {
        Command cmd = findCommand(command);
        return cmd == null ? -1 : cmd.getSpecificity();
    }

    public boolean expectsResponse(String command) {
        Command cmd = findCommand(command);
        return cmd != null && cmd.expectsResponse();
    }

    /**
     * Finds the most specific command matching the given input.
     *
     * @return the matching command with the highest specificity,
     *         or {@code null} if no command matches
     */
    public Command findCommand(String command) {
        String input = normalize(command);
        Command best = null;
        int bestSpecificity = -1;

        for (Command cmd : commands) {
            if (matches(input, cmd.getPrefix())) {
                int specificity = cmd.getSpecificity();
                if (specificity > bestSpecificity) {
                    best = cmd;
                    bestSpecificity = specificity;
                }
            }
        }
        return best;
    }

    /**
     * Determines whether a command belongs to the given prefix.
     *
     * @return {@code true} if the command matches the prefix,
     *         {@code false} otherwise
     */
    public static boolean matches(String command, String prefix) {
        String code = normalize(prefix);
        return command.equals(code) || command.startsWith(code + " ");
    }

    /**
     *  Prepares a command for consistent comparison.
     *
     * @return the normalized command
     */
    private static String normalize(String command) {
        return command.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
