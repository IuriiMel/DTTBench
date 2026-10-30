package com.terminal.controller.tabs;

import javafx.scene.Node;

/**
 * Contract for command generation tabs.
 *
 * <p>All tabs must be registered during {@link com.terminal.controller.CommandListController#initialize()} via
 * {@link com.terminal.controller.CommandListController#registerTab(CommandTab)}.</p>
 */

public interface CommandTab {

    String getTabName();

    /**
     * @return a node containing the tab's UI elements.
     */
    Node getContent();

    /**
     * @return formatted command string based on current UI state.
     */
    String generateCommand();

    /**
     * @return human-readable description of the current command.
     */
    String generateDescription();

    /**
     * Resets all UI controls to their default state.
     */
    void clear();
}
