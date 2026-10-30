package com.terminal.utils;

import com.terminal.config.Config;
import com.terminal.controller.FindReplace;
import com.terminal.model.AlertService;
import com.terminal.plotter.PlotterManager;
import com.terminal.port.PortID;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    private static final Path defaultScriptDirection = Config.getScriptsDirectory();
    private static final Path defaultLogDirection = Config.getLogsDirectory();
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static ContextMenu contextMenu(TextFlow textFlow){
        MenuItem save = new MenuItem("Save");
        MenuItem errors = new MenuItem("Save errors");
        MenuItem plot = new MenuItem("Plot a graph");
        MenuItem clear = new MenuItem("Clear");

        ContextMenu contextMenu = new ContextMenu(save, errors, plot, clear);

        save.setOnAction(event -> {
            saveToFile(textFlow);
        });
        errors.setOnAction(event -> {
            saveErrors(textFlow);
        });
        plot.setOnAction(event -> {
            PlotterManager.plot((PortID) textFlow.getUserData());
        });
        clear.setOnAction(event -> {
            textFlow.getChildren().clear();
            PlotterManager.clear((PortID) textFlow.getUserData());
        });

        textFlow.addEventFilter(MouseEvent.MOUSE_DRAGGED, Event::consume);
        textFlow.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isPrimaryButtonDown()) {
                e.consume();
            }
        });
        textFlow.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            contextMenu.show(textFlow, event.getScreenX(), event.getScreenY());
        });
        textFlow.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() != MouseButton.SECONDARY) contextMenu.hide();
        });
        return contextMenu;
    }

    public static void contextMenu(TextArea textArea){
        MenuItem save = new MenuItem("Save");
        MenuItem load = new MenuItem("Load");
        MenuItem find = new MenuItem("Find & Replace");
        MenuItem clear = new MenuItem("Clear");
        ContextMenu contextMenu = new ContextMenu(save, load, find, clear);

        save.setOnAction(event -> {
            saveToFile(textArea);
        });
        load.setOnAction(event -> {
            loadFromFile(textArea);
        });
        find.setOnAction(event -> {
            findReplace(textArea);
        });
        clear.setOnAction(event -> {
            textArea.clear();
        });

        // Only custom menu
        textArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            event.consume();    // disable default menu
            contextMenu.show(textArea, event.getScreenX(), event.getScreenY());
        });
        textArea.setOnMouseClicked(mouseEvent -> {
            if(!mouseEvent.getButton().name().equals("SECONDARY")) contextMenu.hide();
        });
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(event.isControlDown() && event.getCode() == KeyCode.S){
                event.consume();
                saveToFile(textArea);
            }
        });
    }

    public static void saveToFile(TextFlow textFlow){
        File file = chooseFile("Save log", defaultLogDirection);
        if(file == null) return;

        try(BufferedWriter writer = Files.newBufferedWriter(file.toPath())){
            for (Node node : textFlow.getChildren()){
                if(node instanceof Text){
                    Text text = (Text) node;
                    LocalDate date = (LocalDate) text.getProperties().get("date");
                    if (date != null) {
                        writer.write(date.toString() + " " + text.getText());
                    }
                    else writer.write(text.getText());
                }
            }
        }
        catch(IOException e){
            String message = "Failed to save log to file";
            log.error(message, e);
            AlertService.alertError(e, "Save error", message);
        }
    }

    public static void saveToFile(TextArea textArea){
        File file = chooseFile("Save script", defaultScriptDirection);
        if(file == null) return;

        try{
            Files.writeString(file.toPath(), textArea.getText());
        }
        catch(IOException e){
            String message = "Failed to save script to file";
            log.error(message, e);
            AlertService.alertError(e, "Save error", message);
        }
    }

    public static void saveErrors(TextFlow textFlow){
        ObservableList<Node> lines = textFlow.getChildren();
        List<String> list = new ArrayList<>();
        LocalDate date;

        for(int i = 0; i < lines.size(); i++){
            Text text = (Text) lines.get(i);
            Text previousText = new Text();
            if(i > 0) previousText = (Text) lines.get(i - 1);
            if(text.getText().contains("ERROR") || text.getText().contains("WARNING")){
                date = (LocalDate) previousText.getProperties().get("date");
                list.add(date + previousText.getText().replace("\n", ""));
                list.add(date + text.getText().replace("\n", ""));
                list.add("...");
            }
        }

        File file = chooseFile("Save error log", defaultLogDirection);
        if(file == null) return;

        try {
            Files.write(file.toPath(), list);
        }
        catch (IOException e){
            String message = "Failed to save script to file";
            log.error(message, e);
            AlertService.alertError(e, "Save error", message);
        }
    }

    public static void copy(TextFlow textFlow){
        StringBuilder stringBuilder = new StringBuilder();

        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                stringBuilder.append(((Text) node).getText());
            }
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(stringBuilder.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public static void loadFromFile(TextArea textArea){
        File file = chooseFile("Load script", defaultScriptDirection);
        if (file == null) return;
        try{
            String content = Files.readString(file.toPath());
            textArea.setText(content);
        }
        catch (IOException e){
            String message = "Failed to load script from file";
            log.error(message, e);
            AlertService.alertError(e, "Load error", message);
        }
    }

    private static File chooseFile(String title, Path direction){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text File", "*.txt"));

        if(Files.exists(direction)) fileChooser.setInitialDirectory(direction.toFile());
        else fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file;
        if(title.contains("Save")) {
            file = fileChooser.showSaveDialog(null);
        }
        else file = fileChooser.showOpenDialog(null);
        return file;
    }

    public static void findReplace(TextArea textArea){
        try{
            new FindReplace(textArea);
        }
        catch (Exception e) {
            String message = "Failed to create the Find & Replace window.";
            log.error(message, e);
            AlertService.alertError(e, "Initialization Error", message);
        }
    }
}
