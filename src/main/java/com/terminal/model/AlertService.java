package com.terminal.model;

import com.terminal.Main;
import com.terminal.utils.FileUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * Displays error alerts with exception details for inspection and copying.
 * Highlights stack trace entries belonging to the project.
 */
public class AlertService {

    public static void alertError(Throwable exception, String title, String text){
        String error = exception.getClass().getSimpleName();
        if (exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            error += ": " + exception.getMessage();
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.setContentText(error);
        alert.getDialogPane().setStyle( "-fx-font-family: 'Consolas';" + "-fx-font-size: 12px;");

        TextFlow textFlow = new TextFlow();
        MenuItem copy = new MenuItem("Copy");
        ContextMenu contextMenu = new ContextMenu(copy);
        textFlow.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10;");
        textFlow.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.SECONDARY) contextMenu.show(textFlow, event.getScreenX(), event.getScreenY());
            else contextMenu.hide();
        });
        copy.setOnAction(event -> {
            FileUtils.copy(textFlow);
        });

        StackTraceElement[] stackTrace = exception.getStackTrace();
        for(StackTraceElement element : stackTrace){
            String line = element.toString() + "\n";
            Text textNode = new Text(line);
            textNode.setFont(Font.font("Consolas", 12));

            if (isProjectClass(element.getClassName())) {
                textNode.setFill(javafx.scene.paint.Color.LIGHTBLUE);
            } else {
                textNode.setFill(javafx.scene.paint.Color.GRAY);
            }
            textFlow.getChildren().add(textNode);
        }

        ScrollPane scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle("-fx-background: #2b2b2b;");

        VBox expandable = new VBox(scrollPane);
        expandable.setPadding(new Insets(10));
        expandable.setFillWidth(true);

        alert.getDialogPane().setExpandableContent(expandable);
        alert.getDialogPane().setExpanded(false);

        alert.setOnShown(event -> {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(Main.getIco());
            stage.setAlwaysOnTop(true);
        });
        alert.showAndWait();
    }

    private static boolean isProjectClass(String className) {
        return className.startsWith("com.terminal.");
    }
}
