package com.terminal.controller;

import com.terminal.config.Config;
import com.terminal.model.*;
import com.terminal.ui.NewWindow;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root controller for the main application window.
 *
 * <p>Coordinates the view mode switching, lazy initialization of secondary windows
 * (Command List).</p>
 */
public class MainController {

    @FXML private ComboBox<ViewMode> mode;

    private NewWindow commandList;
    private ViewModeManager controllerManager;
    private BorderPane pane;

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    /**
     * Populates the mode selector with all available {@link ViewMode} values
     * and sets the first mode as default.
     */
    @FXML
    public void initialize()  {
        mode.getItems().addAll(ViewMode.values());
        mode.setValue(ViewMode.DEFAULT);
    }

    public void setPane(BorderPane pane){
        this.pane = pane;
    }

    public void setControllerManager(ViewModeManager controllerManager){
        this.controllerManager = controllerManager;
    }

    /**
     * Attaches a listener to the mode selector that updates the central pane content
     * and notifies {@link ViewModeManager} about the new active controller.
     */
    public void modeListener(){
        try{
            mode.valueProperty().addListener((observable, oldValue, newValue) -> {
                pane.setCenter(controllerManager.getContent(newValue));
                controllerManager.setActiveController(newValue);
            });
        }
        catch(Exception e){
            String message = "Failed to initialize CoordinatedMode";
            log.error(message, e);
            AlertService.alertError(e, "CoordinatedMode error", message);
        }
    }

    /**
     * Opens the Command List window, creating it lazily on first invocation.
     *
     * <p>On window close, resets all command tabs to their default state via
     * {@link CommandListController#clear()}.</p>
     */
    @FXML
    public void commandList(){
        if(commandList == null) {
            try{
                commandList = new NewWindow("/com/terminal/view/commandList.fxml", "Command list", false, true);
                CommandListController controller = commandList.getLoader().getController();
                controller.setControllerManager(controllerManager);
                commandList.getStage().setOnCloseRequest(event -> {
                    controller.clear();
                });
            }
            catch(Exception e){
                String message = "Failed to create the Command List window.";
                log.error(message, e);
                AlertService.alertError(e, "Initialization Error", message);
                return;
            }
        }
        commandList.getStage().show();
    }

    @FXML
    public void config() {
        Config.showConfig();
    }

    public void syntax() {
        Config.showSyntax();
    }

    @FXML
    public void about() {
        About.showAboutDialog();
    }
}
