package com.terminal.controller.tabs;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import java.util.stream.Collectors;
import java.util.Arrays;

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
public class RelayTab implements CommandTab {
    private final ToggleButton[] relays = new ToggleButton[8];

    public RelayTab() {
        for (int i = 0; i < 8; i++) {
            relays[i] = new ToggleButton("Btn" + (i + 1));
            relays[i].setPrefWidth(60);
        }
    }

    /**
     * Replace with the name of your command tab.
     */
    @Override
    public String getTabName() {
        return "Relays Control";
    }

    /**
     * Replace with the UI controls required for your tab.
     */
    @Override
    public Node getContent() {
        VBox mainBox = new VBox(10);
        HBox row1 = new HBox(10);
        HBox row2 = new HBox(10);

        row1.getChildren().addAll(relays[0], relays[1], relays[2], relays[3]);
        row2.getChildren().addAll(relays[4], relays[5], relays[6], relays[7]);

        mainBox.getChildren().addAll(new Label("Select relays:"), row1, row2);
        return mainBox;
    }

    /**
     * Replace with your command generation logic.
     */
    @Override
    public String generateCommand() {
        int minor = 0;
        int major = 0;

        if (relays[0].isSelected()) major |= 1;
        if (relays[1].isSelected()) major |= 2;
        if (relays[2].isSelected()) major |= 4;
        if (relays[3].isSelected()) major |= 8;

        if (relays[4].isSelected()) minor |= 1;
        if (relays[5].isSelected()) minor |= 2;
        if (relays[6].isSelected()) minor |= 4;
        if (relays[7].isSelected()) minor |= 8;

        return String.format("%X%X", minor, major);
    }

    /**
     * Replace with your command description generation method.
     */
    @Override
    public String generateDescription() {
        return Arrays.stream(relays)
                .filter(ToggleButton::isSelected)
                .map(ToggleButton::getText)
                .collect(Collectors.joining(" + "));
    }

    @Override
    public void clear(){
        for(ToggleButton button : relays) button.setSelected(false);
    }
}
