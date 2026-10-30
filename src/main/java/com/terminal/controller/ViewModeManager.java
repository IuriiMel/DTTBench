package com.terminal.controller;

import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the mapping between {@link ViewMode} and their corresponding
 * UI controllers and content nodes.
 *
 * <p>Tracks the currently active controller for event routing and content display.</p>
 */
public class ViewModeManager {
    private final Map<ViewMode, ModeController> controllers = new HashMap<>();
    private final Map<ViewMode, Node> contents = new HashMap<>();

    private ModeController activeController;

    public void addController(ViewMode mode, ModeController controller) {
        controllers.put(mode, controller);
    }

    public void addContent(ViewMode mode, Node content) {
        contents.put(mode, content);
    }

    public void setActiveController(ViewMode mode) {
        activeController = controllers.get(mode);
    }

    public Node getContent(ViewMode mode) {
        return contents.get(mode);
    }

    public ModeController getActiveController() {
        return activeController;
    }
}
