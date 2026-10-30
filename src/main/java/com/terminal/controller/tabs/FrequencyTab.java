package com.terminal.controller.tabs;

import com.terminal.utils.ByteUtils;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.Node;

/**
 * Example implementation of a command tab.
 *
 * <p>This class is provided as a template. Use it as a reference when creating
 * a command tab for your device. Replace the example UI, protocol commands,
 * command generation logic, and descriptions with your device-specific implementation.
 *
 * <p>The example demonstrates conditional UI logic, dynamic command generation,
 * and CRC calculation.
 */

public class FrequencyTab implements CommandTab {
    private final ComboBox<FrequencyMode> modeBox;
    private final ComboBox<FrequencyChannel> channelBox;
    private final TextField measureTimeField;

    public FrequencyTab() {
        modeBox = new ComboBox<>(FXCollections.observableArrayList(FrequencyMode.values()));
        modeBox.setValue(FrequencyMode.MODE_1);

        channelBox = new ComboBox<>(FXCollections.observableArrayList(FrequencyChannel.values()));
        channelBox.setValue(FrequencyChannel.CHANNEL1);

        measureTimeField = new TextField();
        measureTimeField.setPrefWidth(50);
        measureTimeField.setDisable(true);

        modeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != FrequencyMode.MODE_3) measureTimeField.setDisable(true);
            else measureTimeField.setDisable(false);
        });
    }

    /**
     * Replace with the name of your command tab.
     */
    @Override
    public String getTabName() {
        return "Get Frequency";
    }

    /**
     * Replace with the UI controls required for your tab.
     */
    @Override
    public Node getContent() {
        Label title = new Label("Get Frequency Channel RBx");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPrefWidth(120);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);

        ColumnConstraints infoColumn = new ColumnConstraints();
        infoColumn.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labelColumn, fieldColumn, infoColumn);

        grid.add(new Label("Mode:"), 0, 0);
        grid.add(modeBox, 1, 0);

        grid.add(new Label("Channel:"), 0, 1);
        grid.add(channelBox, 1, 1);

        grid.add(new Label("Measure time:"), 0, 2);
        grid.add(measureTimeField, 1, 2);
        grid.add(new Label("s"), 2, 2);

        return new VBox(10, title, grid);
    }

    /**
     * Replace with your command generation logic.
     */
    @Override
    public String generateCommand() {
        FrequencyChannel ch = channelBox.getValue();
        boolean hasTime = !measureTimeField.isDisabled() && !measureTimeField.getText().isEmpty();
        String command = "01 " + ch.getCommand();

        if (hasTime) {
            int time = Integer.parseInt(measureTimeField.getText());
            command += String.format(" %02X", time);
        }

        byte[] data = ByteUtils.hexToBytes(command);
        byte[] crc = ByteUtils.crc16(data, ByteUtils.CRC16Mode.ARC);
        return String.format("%s %02X %02X", command, crc[0], crc[1]);
    }

    /**
     * Replace with your command description generation method.
     */
    @Override
    public String generateDescription(){
        return "";
    }

    @Override
    public void clear(){
        modeBox.getSelectionModel().selectFirst();
        channelBox.getSelectionModel().selectFirst();
        measureTimeField.clear();
    }

    /**
     * Example frequency measurement modes with corresponding protocol commands.
     * Replace with your device-specific modes and command codes.
     */
    public enum FrequencyMode {
        MODE_1("High frequency", "01 00 01"),
        MODE_2("Medium frequency", "01 00 02"),
        MODE_3("Low frequency", "01 00 03");

        private final String label;
        private final String command;

        FrequencyMode(String label, String command) {
            this.label = label;
            this.command = command;
        }
        public String getLabel(){
            return label;
        }
        public String getCommand() {
            return command;
        }
        @Override
        public String toString(){
            return label;
        }
    }

    /**
     * Example frequency measurement channels with corresponding protocol bytes.
     * Replace with your device-specific channel identifiers.
     */
    public enum FrequencyChannel {
        CHANNEL1("00"),
        CHANNEL2("01"),
        CHANNEL3("02");

        private final String command;

        FrequencyChannel(String command) {
            this.command = command;
        }
        public String getCommand() {
            return command;
        }
        @Override
        public String toString(){
            return name();
        }
    }
}
