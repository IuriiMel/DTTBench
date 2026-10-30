package com.terminal.controller.tabs;

import com.terminal.utils.ByteUtils;
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
public class VoltageTab implements CommandTab {
    private final TextField channelField;
    private final TextField measureTimeField;

    public VoltageTab() {
        channelField = new TextField("");
        channelField.setPrefWidth(50);
        measureTimeField = new TextField("");
        measureTimeField.setPrefWidth(50);
    }

    /**
     * Replace with the name of your command tab.
     */
    @Override
    public String getTabName() {
        return "Get Voltage";
    }

    /**
     * Replace with the UI controls required for your tab.
     */
    @Override
    public Node getContent() {
        Label title = new Label("Get Voltage Channel ANx");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPrefWidth(120);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setPrefWidth(50);

        ColumnConstraints infoColumn = new ColumnConstraints();
        infoColumn.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labelColumn, fieldColumn, infoColumn);

        grid.add(new Label("Channel:"), 0, 0);
        grid.add(channelField, 1, 0);

        grid.add(new Label("Measure time:"), 0, 1);
        grid.add(measureTimeField, 1, 1);
        grid.add(new Label("ms"), 2, 1);

        VBox content = new VBox(10);
        content.getChildren().addAll(title, grid);

        return content;
    }

    /**
     * Replace with your command generation logic.
     */
    @Override
    public String generateCommand() {
        String command = "01 02";
        byte[] data = ByteUtils.hexToBytes(command);
        byte[] crc = ByteUtils.crc16(data, ByteUtils.CRC16Mode.ARC);
        return command + String.format(" %02X %02X", crc[0], crc[1]);
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
        channelField.clear();
        measureTimeField.clear();
    }
}
