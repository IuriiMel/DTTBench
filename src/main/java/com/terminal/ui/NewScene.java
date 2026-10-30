package com.terminal.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class NewScene {

    private final FXMLLoader loader;
    private final Parent rootLayout;

    public NewScene(String url) throws IOException {
        loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(url));
        rootLayout = loader.load();
    }

    public FXMLLoader getLoader() {
        return loader;
    }

    public Parent getRootLayout(){
        return rootLayout;
    }
}
