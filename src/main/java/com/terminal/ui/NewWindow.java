package com.terminal.ui;

import com.terminal.Main;
import com.terminal.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NewWindow {

    private Stage stage;
    private FXMLLoader loader;
    private Parent rootLayout;
    private MainController mainController;

    // Constructor for main window
    public NewWindow(Stage stage, String url, String title, double minWidth, double minHeight) throws IOException {
        loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(url));
        rootLayout = loader.load();

        mainController = loader.getController();

        stage.setScene(new Scene(rootLayout));
        stage.setTitle(title);
        stage.setWidth(minWidth);
        stage.setMinWidth(minWidth);
        stage.setHeight(minHeight);
        stage.setMinHeight(minHeight);
        stage.setResizable(true);
        stage.show();

        this.stage = stage;
    }

    // Basic constructor
    public NewWindow(String url, String title, boolean resizable, boolean onTop) throws IOException {
        stage = new Stage();
        loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(url));
        rootLayout = loader.load();

        stage.setScene(new Scene(rootLayout));
        stage.setTitle(title);
        stage.setResizable(resizable);
        stage.setAlwaysOnTop(onTop);
        stage.getIcons().add(Main.getIco());

        stage.show();
        Main.setOnCenter(stage);
    }

    public Stage getStage() {
        return stage;
    }

    public FXMLLoader getLoader() {
        return loader;
    }

    public Parent getRootLayout(){return rootLayout;}

    public MainController getMainController(){return mainController;}
}
