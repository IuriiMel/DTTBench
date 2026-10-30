package com.terminal.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.terminal.controller.tabs.*;
import com.terminal.model.AlertService;
import com.terminal.model.Command;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manages the command list window and command generation tabs.
 *
 * <p>Acts as a bridge between command generation tabs ({@link CommandTab})
 * and the active mode controller ({@link ModeController}) via {@link ViewModeManager}.
 * Provides UI for creating, storing, and inserting commands into the active terminal.</p>
 */
public class CommandListController {

    @FXML private TableView<Command> commandsTable;
    @FXML private TableColumn<Command, String> descriptionColumn;
    @FXML private TableColumn<Command, String> commandsColumn;
    @FXML private TabPane tabPane;

    private ViewModeManager controllerManager;
    private final ObservableList<Command> commandList = FXCollections.observableArrayList();
    private BaseTabContent activeTabContent;

    private static final String FILE_NAME = "commands.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger log = LoggerFactory.getLogger(CommandListController.class);

    /**
     * Initializes the UI, loads saved commands, and registers tabs.
     *
     * <p><b>To add a new command tab:</b> Call {@link #registerTab(CommandTab)} with
     * your custom implementation at the end of this method.</p>
     */
    @FXML
    public void initialize() {
        loadCommands();

        descriptionColumn.setCellValueFactory(cellData -> cellData.getValue().getDescriptionProperty());
        commandsColumn.setCellValueFactory(cellData -> cellData.getValue().getCommandProperty());
        commandsTable.setItems(commandList);

        commandsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) addCommand();
        });
        commandsTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) addCommand();
            else if (event.getCode() == KeyCode.DELETE) deleteCommand();
        });
        tabPane.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            activeTabContent = (BaseTabContent) newValue.getContent();
        }));

        //Register new tab;
        registerTab(new VoltageTab());
        registerTab(new FrequencyTab());
        registerTab(new RelayTab());

        tabPane.getSelectionModel().selectFirst();
    }

    public void setControllerManager(ViewModeManager manager) {
        controllerManager = manager;
    }

    /**
     * Registers a command generation tab in the UI.
     *
     * <p>Wraps the provided {@link CommandTab} in a {@link BaseTabContent} with standard actions (Add, Save, Copy)
     * and adds it to the tab pane.</p>
     *
     * @param commandTab the tab implementation providing UI and command generation logic.
     */
    private void registerTab(CommandTab commandTab) {
        BaseTabContent content = new BaseTabContent(commandTab, this::add, this::save);

        Tab tab = new Tab(commandTab.getTabName());
        tab.setContent(content);
        tab.setClosable(false);

        tabPane.getTabs().add(tab);
    }

    /**
     * Inserts the generated command into the active editor.
     */
    private void add() {
        String command = activeTabContent.getCommand();
        if (command.isEmpty()) return;
        controllerManager.getActiveController().insertCommandIntoCaret(command);
    }

    /**
     * Saves the command and description from the currently active tab to the persistent command list.
     */
    private void save() {
        String command = activeTabContent.getCommand();
        String description = activeTabContent.getDescription();
        if (command.isEmpty()) return;
        commandList.add(new Command(description, command));
        saveCommands();
    }

    /**
     * Inserts the selected command from the table into the active editor.
     * Triggered by double-click or Enter key on the table.
     */
    private void addCommand() {
        String command = commandsTable.getSelectionModel().getSelectedItem().getCommand();
        controllerManager.getActiveController().insertCommandIntoCaret(command);
    }

    @FXML
    private void deleteCommand() {
        commandsTable.getItems().remove(commandsTable.getSelectionModel().getSelectedItem());
        saveCommands();
    }

    /**
     * Loads example commands into the list. Replace with device-specific defaults.
     */
    @FXML
    private void loadDefaultCommands() {
        commandList.setAll(
                new Command("Get device information", "XX XX XX"),
                new Command("Read voltage on channel X", "XX XX XX"),
                new Command("Read low-frequency value", "XX XX XX")
        );
        saveCommands();
    }

    /**
     * Saves the current command list to a JSON file.
     *
     * <p><b>Note:</b> JavaFX {@code StringProperty} fields are transient and are not
     * included in the serialized JSON.</p>
     */
    private void saveCommands() {
        try (Writer writer = new FileWriter(FILE_NAME, StandardCharsets.UTF_8)) {
            gson.toJson(commandList, writer);
        }
        catch (IOException e) {
            String message = "Failed to save commands to \"" + FILE_NAME + "\".";
            log.error(message, e);
            AlertService.alertError(e, "Save error", message);
        }
    }

    /**
     * Loads commands from a JSON file. If the file does not exist, loads default commands.
     *
     * <p><b>Important:</b> After deserialization, transient JavaFX {@code StringProperty}
     * fields are restored via {@link Command#initProperties()} before the commands are
     * added to the UI table.</p>
     */
    private void loadCommands() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            loadDefaultCommands();
            return;
        }
        try (Reader reader = new FileReader(FILE_NAME, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<Command>>() {}.getType();
            List<Command> loaded = gson.fromJson(reader, listType);
            for (Command c : loaded) c.initProperties();
            commandList.clear();
            commandList.addAll(loaded);
        }
        catch (IOException e) {
            String message = "Failed to load commands from \"" + FILE_NAME + "\".";
            log.error(message, e);
            AlertService.alertError(e, "Load error", message);
        }
    }

    public void clear() {
        for (Tab tab : tabPane.getTabs()) ((BaseTabContent) tab.getContent()).clear();
        tabPane.getSelectionModel().selectFirst();
    }
}
