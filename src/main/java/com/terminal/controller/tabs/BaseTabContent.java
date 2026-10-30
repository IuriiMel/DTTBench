package com.terminal.controller.tabs;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

/**
 * UI wrapper that standardizes the layout and controls for any {@link CommandTab} implementation.
 *
 * <p>Provides common actions (Calculate, Copy, Add, Save) and injects behavior via {@link Runnable}
 * callbacks.</p>
 */
public final class BaseTabContent extends VBox {
    private final TextField commandField;
    private final TextField descriptionField;
    private final CommandTab tab;

    /**
     * Constructs the standardized UI panel for a specific command tab.
     *
     * @param tab  the specific command generation logic and custom UI content
     * @param add  callback executed when the "Add" button is clicked
     * @param save callback executed when the "Save to List" button is clicked
     */
    public BaseTabContent(CommandTab tab, Runnable add, Runnable save) {
        this.tab = tab;
        setSpacing(10);
        setPadding(new Insets(10));
        setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        commandField = new TextField();
        commandField.setEditable(false);
        GridPane.setHgrow(commandField, Priority.ALWAYS);

        descriptionField = new TextField();
        GridPane.setHgrow(descriptionField, Priority.ALWAYS);

        Button calcButton = new Button("Calculate");

        calcButton.setOnAction(e -> {
            commandField.setText(tab.generateCommand());
            descriptionField.setText(tab.generateDescription());
        });

        HBox actionBox = new HBox(10);

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> add.run());

        Button copyButton = new Button("Copy");
        copyButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(commandField.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });

        actionBox.getChildren().addAll(addButton, copyButton);

        Button saveButton = new Button("Save to List");
        saveButton.setOnAction(e -> save.run());

        GridPane resultGrid = new GridPane();
        resultGrid.setHgap(10);
        resultGrid.setVgap(10);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPrefWidth(90);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);

        resultGrid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        resultGrid.add(calcButton, 1, 0);
        resultGrid.add(new Label("Command:"), 0, 1);
        resultGrid.add(commandField, 1, 1);
        resultGrid.add(actionBox, 1, 2);
        resultGrid.add(new Label("Description:"), 0, 3);
        resultGrid.add(descriptionField, 1, 3);
        resultGrid.add(saveButton, 1, 4);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(tab.getContent(), spacer, resultGrid);
    }

    public String getCommand() {
        return commandField.getText();
    }

    public String getDescription() {
        return descriptionField.getText();
    }

    /**
     * Resets the local text fields and delegates the clear operation to the {@link CommandTab}.
     */
    public void clear(){
        commandField.clear();
        descriptionField.clear();
        tab.clear();
    }
}
