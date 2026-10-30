package com.terminal.controller;

/**
 * Represents the available UI operating modes of the application.
 *
 * <p>Used by {@link com.terminal.controller.ViewModeManager} to switch between
 * different controller implementations and their corresponding UI layouts.</p>
 * Add here your new modes.
 */
public enum ViewMode {
    SEPARATE("SeparateMode"),
    COORDINATED("CoordinatedMode");

    public static final ViewMode DEFAULT = SEPARATE;
    private final String text;

    ViewMode(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
