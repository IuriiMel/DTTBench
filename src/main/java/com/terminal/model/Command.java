package com.terminal.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a saved command with description and command string.
 *
 * <p>Uses JavaFX {@link StringProperty} for UI binding in {@code TableView}.
 * Property fields are marked {@code transient} to exclude them from JSON serialization.
 * Call {@link #initProperties()} after deserialization to restore them.</p>
 */
public class Command {
    private final String description;
    private final String command;
    private transient StringProperty descriptionProperty;
    private transient StringProperty commandProperty;

    public Command(String description, String command){
        this.description = description;
        this.command = command;
        descriptionProperty = new SimpleStringProperty(description);
        commandProperty = new SimpleStringProperty(command);
    }

    public String getDescription(){
        return description;
    }

    public String getCommand(){
        return command;
    }

    public StringProperty getDescriptionProperty(){
        return descriptionProperty;
    }

    public StringProperty getCommandProperty(){
        return commandProperty;
    }

    /**
     * Initializes JavaFX properties from the stored command data.
     */
    public void initProperties() {
        if (descriptionProperty == null) descriptionProperty = new SimpleStringProperty(description);
        if (commandProperty == null) commandProperty = new SimpleStringProperty(command);
    }
}
