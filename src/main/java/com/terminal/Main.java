package com.terminal;

import com.terminal.controller.*;
import com.terminal.model.AlertService;
import com.terminal.config.Config;
import com.terminal.port.PortManager;
import com.terminal.port.SessionListenerManager;
import com.terminal.ui.NewScene;
import com.terminal.ui.NewWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends Application {

    private static Stage pStage;
    private static Image ico;
    private static String launchTime;
    private static Logger log;

    public static void main(String[] args) {
        launchTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Config.load();
        log = LoggerFactory.getLogger(Main.class);
        log.info("=== DTTBench Starting ===");
        launch();
    }

    public void start(Stage primaryStage) {
        try {
            ico = new Image("/com/terminal/images/ico.png");
            primaryStage.getIcons().add(ico);
            NewWindow mainWindow = new NewWindow(primaryStage, "/com/terminal/view/mainWindow.fxml", "DTTBench",1250,700);
            MainController mainController = mainWindow.getMainController();

            ViewModeManager viewModeManager = new ViewModeManager();
            SessionListenerManager sessionManager = new SessionListenerManager(viewModeManager::getActiveController);
            PortManager portManager = new PortManager(sessionManager);

            NewScene separateModeScene = new NewScene("/com/terminal/view/separateModeScene.fxml");
            NewScene coordinatedModeScene = new NewScene("/com/terminal/view/coordinatedModeScene.fxml");
            SeparateModeController separateModeController = separateModeScene.getLoader().getController();
            CoordinatedModeController coordinatedModeController = coordinatedModeScene.getLoader().getController();

            separateModeController.setPortManager(portManager);
            coordinatedModeController.setPortManager(portManager);
            separateModeController.init();
            coordinatedModeController.init();

            viewModeManager.addContent(ViewMode.SEPARATE, separateModeScene.getLoader().getRoot());
            viewModeManager.addContent(ViewMode.COORDINATED, coordinatedModeScene.getLoader().getRoot());
            viewModeManager.addController(ViewMode.SEPARATE, separateModeController);
            viewModeManager.addController(ViewMode.COORDINATED, coordinatedModeController);
            viewModeManager.setActiveController(ViewMode.SEPARATE);

            BorderPane pane = (BorderPane) primaryStage.getScene().getRoot();
            mainController.setControllerManager(viewModeManager);
            mainController.setPane(pane);
            mainController.modeListener();

            pane.setCenter(separateModeScene.getRootLayout());

            pStage = primaryStage;

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                String message = "Uncaught exception in thread " + thread.getName();
                log.error(message, throwable);
                AlertService.alertError(throwable, "Exception", message);
            });

            primaryStage.setOnCloseRequest(event -> {
                portManager.closeAll();
                Platform.exit();
                log.info("Application closed.");
            });

            // CSS
//            mainWindow.getStage().getScene().getStylesheets().add(
//                    getClass().getResource("/com/terminal/css/.css").toExternalForm()
//            );
        }
        catch (Exception e) {
            String message = "Failed to start the application";
            log.error(message, e);
            AlertService.alertError(e, "Launching error", message);
            Platform.exit();
        }
    }

    public static void setOnCenter(Stage stage){
        stage.setX(pStage.getX() + pStage.getWidth()/2 - stage.getWidth()/2);
        stage.setY(pStage.getY() + pStage.getHeight()/2 - stage.getHeight()/2);
    }
    public static Image getIco(){
        return ico;
    }
    public static String getLaunchTime(){
        return launchTime;
    }
}
