package com.terminal.controller;

import com.terminal.handlers.HandlerRegistry;
import com.terminal.plotter.PlotterManager;
import com.terminal.port.PortID;
import com.terminal.port.PortManager;
import com.terminal.port.SessionListener.LogType;
import com.terminal.port.SessionListener.ControlSignal;
import com.terminal.script.ScriptEngine;
import com.terminal.utils.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the "Separate Mode" UI, managing two independent serial port sessions.
 *
 * <p>Implements {@link ModeController} to receive asynchronous events from background threads.
 * All UI updates triggered by port events are safely wrapped in {@code Platform.runLater()}.</p>
 */
public class SeparateModeController implements ModeController {

    @FXML private Label firstStatusLabel;
    @FXML private Label secondStatusLabel;
    @FXML private Button firstPreviousStepButton;
    @FXML private Button firstNextStepButton;
    @FXML private Button firstRepeatStepButton;
    @FXML private Button firstResetStepButton;
    @FXML private Button firstStopStepButton;
    @FXML private Button secondPreviousStepButton;
    @FXML private Button secondNextStepButton;
    @FXML private Button secondRepeatStepButton;
    @FXML private Button secondResetStepButton;
    @FXML private Button secondStopStepButton;
    @FXML private RadioButton firstRTS;
    @FXML private RadioButton firstCTS;
    @FXML private RadioButton firstDTR;
    @FXML private RadioButton firstDSR;
    @FXML private RadioButton secondRTS;
    @FXML private RadioButton secondCTS;
    @FXML private RadioButton secondDTR;
    @FXML private RadioButton secondDSR;
    @FXML private TextFlow firstTextFlow;
    @FXML private ComboBox<String> firstPortList;
    @FXML private ComboBox<String> firstBaudRate;
    @FXML private Button firstOpen;
    @FXML private TextArea firstTextArea;
    @FXML private TextField  firstTimes;
    @FXML private Text firstCounterRemainingTimes;
    @FXML private TextField firstInterval;
    @FXML private ScrollPane firstScrollPane;
    @FXML private TextFlow secondTextFlow;
    @FXML private ComboBox<String> secondPortList;
    @FXML private ComboBox<String> secondBaudRate;
    @FXML private Button secondOpen;
    @FXML private TextArea secondTextArea;
    @FXML private TextField secondTimes;
    @FXML private Text secondCounterRemainingTimes;
    @FXML private TextField secondInterval;
    @FXML private ScrollPane secondScrollPane;
    @FXML private Text firstStepMode;
    @FXML private Text secondStepMode;

    private PortManager portManager;
    private PlotterManager plotterManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");

    private final HandlerRegistry registry = new HandlerRegistry();
    private ScriptEngine firstScriptEngine;
    private ScriptEngine secondScriptEngine;

    private final ObservableList<String> portNames = FXCollections.observableArrayList();
    private final ObservableList<String> baudRates = FXCollections.observableArrayList("9600", "19200", "38400", "57600", "115200");
    private final List<String> initCommand = new ArrayList<>(List.of("Send(XX)\n"));
    private boolean firstTextAreaClicked = true;        //Selected by default
    private boolean secondTextAreaClicked = false;

    private int firstCommandIndex = -1;
    private int secondCommandIndex = -1;
    private final BooleanProperty firstIsSendBusy = new SimpleBooleanProperty(false);
    private final BooleanProperty secondIsSendBusy = new SimpleBooleanProperty(false);

    /**
     * Initializes UI bindings, context menus, and auto-scroll behavior for the text flows.
     * Disables step mode buttons during script execution.
     */
    @FXML
    public void initialize()  {
        firstCTS.setDisable(true);
        firstDSR.setDisable(true);
        secondCTS.setDisable(true);
        secondDSR.setDisable(true);
        ContextMenu firstContext = FileUtils.contextMenu(firstTextFlow);
        ContextMenu secondContext = FileUtils.contextMenu(secondTextFlow);
        FileUtils.contextMenu(firstTextArea);
        FileUtils.contextMenu(secondTextArea);
        firstTextFlow.setUserData(PortID.SEPARATE_1);
        secondTextFlow.setUserData(PortID.SEPARATE_2);

        firstPreviousStepButton.disableProperty().bind(firstIsSendBusy);
        firstNextStepButton.disableProperty().bind(firstIsSendBusy);
        firstRepeatStepButton.disableProperty().bind(firstIsSendBusy);
        firstResetStepButton.disableProperty().bind(firstIsSendBusy);
        firstStopStepButton.disableProperty().bind(firstIsSendBusy);
        secondPreviousStepButton.disableProperty().bind(secondIsSendBusy);
        secondNextStepButton.disableProperty().bind(secondIsSendBusy);
        secondRepeatStepButton.disableProperty().bind(secondIsSendBusy);
        secondResetStepButton.disableProperty().bind(secondIsSendBusy);
        secondStopStepButton.disableProperty().bind(secondIsSendBusy);

        firstTextArea.setOnMouseClicked(event -> {
            firstTextAreaClicked = true;
            secondTextAreaClicked = false;
        });
        secondTextArea.setOnMouseClicked(event -> {
            secondTextAreaClicked = true;
            firstTextAreaClicked = false;
        });
        firstTextFlow.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> firstScrollPane.setVvalue(1.0));
        });
        secondTextFlow.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> secondScrollPane.setVvalue(1.0));
        });
        firstScrollPane.setOnContextMenuRequested(event -> {
            firstContext.show(firstScrollPane, event.getScreenX(), event.getScreenY());
        });
        secondScrollPane.setOnContextMenuRequested(event -> {
            secondContext.show(secondScrollPane, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Initializes the {@link ScriptEngine} instances for both ports and populates
     * the port and baud rate selection lists.
     */
    public void init(){
        firstScriptEngine = new ScriptEngine(portManager, portManager.getSessionListener(), registry);
        secondScriptEngine = new ScriptEngine(portManager, portManager.getSessionListener(), registry);

        getComPorts();
        fillPortList(firstPortList);
        fillPortList(secondPortList);
        fillBaudRate();

        if(!portNames.isEmpty()) {
            firstPortList.setValue(portNames.getFirst());
            secondPortList.setValue(portNames.getFirst());
        }

        firstBaudRate.setValue(baudRates.getFirst());
        secondBaudRate.setValue(baudRates.getFirst());

        firstPortList.setOnMouseClicked(mouseEvent -> {
            mouseEvent(firstPortList);
        });
        secondPortList.setOnMouseClicked(mouseEvent -> {
            mouseEvent(secondPortList);
        });
    }

    private void getComPorts(){
        portNames.clear();
        portNames.setAll(portManager.getComPorts());
    }

    private void fillPortList(ComboBox<String> portList) {
        if (portList == null || portNames.isEmpty()) return;
        portList.getItems().clear();
        if(portList.getItems().isEmpty() && !portNames.isEmpty()) {
            portList.getItems().addAll(portNames);
        }
    }

    private void fillBaudRate() {
        if(firstBaudRate.getItems().isEmpty()){
            firstBaudRate.getItems().addAll(baudRates);
            secondBaudRate.getItems().addAll(baudRates);
        }
    }

    public void mouseEvent(ComboBox<String> portList){
        String selected = portList.getSelectionModel().getSelectedItem();
        getComPorts();
        fillPortList(portList);
        portList.setValue(selected);
        int size = portList.getItems().size();
        portList.setVisibleRowCount(Math.min(size, 10));
    }

    @FXML
    public void firstOpen(){
        openPort(PortID.SEPARATE_1, firstOpen, firstPortList, firstBaudRate);
    }
    @FXML
    public void secondOpen() {
        openPort(PortID.SEPARATE_2, secondOpen, secondPortList, secondBaudRate);
    }

    /**
     * Toggles the connection state of a port.
     * If the button text is "Open", attempts to open the selected port.
     * Otherwise, closes the active session.
     */
    private void openPort(PortID portID, Button openButton, ComboBox<String> portList, ComboBox<String> baudList) {
        if(portList.getValue() == null) return;
        if (openButton.getText().equals("Open")) {
            portManager.getSession(portID).open(portList.getValue(), Integer.parseInt(baudList.getValue()));
        } else {
            portManager.getSession(portID).close();
        }
    }

    @FXML
    public void initFirstPort() {
        initPort(PortID.SEPARATE_1, firstScriptEngine);
    }
    @FXML
    public void initSecondPort() {
        initPort(PortID.SEPARATE_2, secondScriptEngine);
    }
    public void initPort(PortID portID, ScriptEngine scriptEngine){
        scriptEngine.execute(initCommand, 1, 0, portID);
    }

    @FXML
    public void firstSend() {
        send(PortID.SEPARATE_1, firstScriptEngine, firstTextArea, firstTimes, firstInterval);
    }
    @FXML
    public void secondSend(){
        send(PortID.SEPARATE_2, secondScriptEngine, secondTextArea, secondTimes, secondInterval);
    }

    /**
     * Executes the commands from the text area for the specified port.
     *
     * <p>Uses {@code 1} execution and {@code 0} interval when the corresponding
     * fields are empty.</p>
     */
    private void send(PortID portID, ScriptEngine scriptEngine, TextArea textArea, TextField timesField, TextField intervalField){
        int times = timesField.getText().isEmpty() ? 1 : Integer.parseInt(timesField.getText());
        int interval = intervalField.getText().isEmpty() ? 0 : Integer.parseInt(intervalField.getText());

        scriptEngine.execute(getCommandsList(textArea), times, interval, portID);
    }

    @FXML
    public void firstNextStep() {
        firstCommandIndex = nextStep(PortID.SEPARATE_1, firstScriptEngine, firstTextArea, firstStepMode, firstCommandIndex);
    }
    @FXML
    public void secondNextStep() {
        secondCommandIndex = nextStep(PortID.SEPARATE_2, secondScriptEngine, secondTextArea, secondStepMode, secondCommandIndex);
    }

    /**
     * Starts step-mode execution or, if it is already running, advances to the next command.
     * Highlights the command in the text area and triggers {@link ScriptEngine#executeStep}.
     *
     * @return the updated command index
     */
    private int nextStep(PortID portID, ScriptEngine scriptEngine, TextArea textArea, Text stepMode, int commandIndex){
        List<String> commands = getCommandsList(textArea);
        stepMode.setFill(Color.GREEN);
        if(commandIndex < commands.size() - 1) {
            commandIndex++;
            highlightCommand(textArea, commands, commandIndex);
            scriptEngine.executeStep(commands.get(commandIndex), portID);
        }
        return commandIndex;
    }

    @FXML
    public void firstPreviousStep() {
        firstCommandIndex = previousStep(PortID.SEPARATE_1, firstScriptEngine, firstTextArea, firstCommandIndex);
    }
    @FXML
    public void secondPreviousStep() {
        secondCommandIndex = previousStep(PortID.SEPARATE_2, secondScriptEngine, secondTextArea, secondCommandIndex);
    }

    /**
     * Reverts the step-mode execution to the previous command and highlights it.
     *
     * @return the updated command index
     */
    private int previousStep(PortID portID, ScriptEngine scriptEngine, TextArea textArea, int commandIndex){
        List<String> commands = getCommandsList(textArea);
        if(commandIndex > 0) {
            commandIndex--;
            highlightCommand(textArea, commands, commandIndex);
            scriptEngine.executeStep(commands.get(commandIndex), portID);
        }
        return commandIndex;
    }

    @FXML
    public void firstRepeatCommand() {
        repeatCommand(PortID.SEPARATE_1, firstScriptEngine, getCommandsList(firstTextArea), firstCommandIndex);
    }
    @FXML
    public void secondRepeatCommand() {
        repeatCommand(PortID.SEPARATE_2, secondScriptEngine, getCommandsList(secondTextArea), secondCommandIndex);
    }

    /**
     * Repeats the highlighted command.
     */
    public void repeatCommand(PortID portID, ScriptEngine scriptEngine, List<String> commands, int commandIndex){
        if(commandIndex == -1) return;
        scriptEngine.executeStep(commands.get(commandIndex), portID);
    }

    @FXML
    public void firstRestartCommand() {
        firstStopStepMode();
        firstNextStep();
    }
    @FXML
    public void secondRestartCommand() {
        secondStopStepMode();
        secondNextStep();
    }
    @FXML
    public void firstStopStepMode() {
        stopStepMode(PortID.SEPARATE_1);
    }
    @FXML
    public void secondStopStepMode() {
        stopStepMode(PortID.SEPARATE_2);
    }

    public void stopStepMode(PortID portID){
        if(portID.equals(PortID.SEPARATE_1)){
            firstStepMode.setFill(Color.BLACK);
            firstCommandIndex = -1;
            highlightCommand(firstTextArea, getCommandsList(firstTextArea), firstCommandIndex);
        }
        else if(portID.equals(PortID.SEPARATE_2)){
            secondStepMode.setFill(Color.BLACK);
            secondCommandIndex = -1;
            highlightCommand(secondTextArea, getCommandsList(secondTextArea), secondCommandIndex);
        }
    }

    @FXML
    public void setFirstRTS() {
        portManager.getSession(PortID.SEPARATE_1).setRTS(firstRTS.isSelected());
    }
    @FXML
    public void setFirstDTR() {
        portManager.getSession(PortID.SEPARATE_1).setDTR(firstDTR.isSelected());
    }
    @FXML
    public void setSecondRTS() {
        portManager.getSession(PortID.SEPARATE_2).setRTS(secondRTS.isSelected());
    }
    @FXML
    public void setSecondDTR() {
        portManager.getSession(PortID.SEPARATE_2).setDTR(secondDTR.isSelected());
    }

    public List<String> getCommandsList(TextArea textArea){
        return new ArrayList<>(Arrays.asList(textArea.getText().split("\\r?\\n")));
    }

    @FXML
    public void firstStop() {
        firstScriptEngine.stop();
    }
    @FXML
    public void secondStop() {
        secondScriptEngine.stop();
    }

    public BooleanProperty getFirstIsSendBusy() {
        return firstIsSendBusy;
    }
    public BooleanProperty getSecondIsSendBusy() {
        return secondIsSendBusy;
    }

    public void setPortManager(PortManager manager){
        portManager = manager;
    }

    public void setPlotterManager(PlotterManager manager){
        plotterManager = manager;
    }

    public boolean isSeparateMode(PortID portID){
        return portID == PortID.SEPARATE_1 || portID == PortID.SEPARATE_2;
    }

    public void updateStatusLabel(Labeled label, boolean isConnected){
        if (isConnected) {
            label.setText("Connected");
            label.setTextFill(Paint.valueOf("GREEN"));
        } else {
            label.setText("Disconnected");
            label.setTextFill(Paint.valueOf("RED"));
        }
    }

    public void highlightCommand(TextArea textArea, List<String> stepModeCommands, int commandIndex){
        int beginIndex = 0;
        int endIndex = 0;

        for(int i = 0; i <= commandIndex; i++){
            beginIndex += i > 0 ? stepModeCommands.get(i - 1).length() + 1 : 0;
            endIndex += stepModeCommands.get(i).length() + 1;

        }
        textArea.selectRange(beginIndex, endIndex);
    }

    public TextArea selectedTextArea(){
        if(firstTextAreaClicked) return firstTextArea;
        else if (secondTextAreaClicked) return secondTextArea;
        return firstTextArea;
    }

    @Override
    public void insertCommandIntoCaret(String command) {
        TextArea textArea = selectedTextArea();
        int caret = textArea.getCaretPosition();

        textArea.insertText(caret, command);

        int nextPosition = caret + command.length();
        String text = textArea.getText();

        int finalPosition = text.indexOf(")", nextPosition);
        if (finalPosition != -1) {
            textArea.positionCaret(finalPosition + 1);
            textArea.insertText(finalPosition + 1, "\n");
            textArea.positionCaret(finalPosition + 2);
        } else {
            textArea.positionCaret(nextPosition);
            textArea.insertText(nextPosition, "\n");
            textArea.positionCaret(nextPosition + 1);
        }
    }

    @Override
    public void updatePortState(PortID portID, boolean isOpen) {
        Platform.runLater(() -> {
            if (portID == PortID.SEPARATE_1) {
                updateStatusLabel(firstStatusLabel, isOpen);
                firstPortList.setDisable(isOpen);
                firstBaudRate.setDisable(isOpen);
                firstOpen.setText(isOpen ? "Close" : "Open");
            } else if (portID == PortID.SEPARATE_2) {
                updateStatusLabel(secondStatusLabel, isOpen);
                secondPortList.setDisable(isOpen);
                secondBaudRate.setDisable(isOpen);
                secondOpen.setText(isOpen ? "Close" : "Open");
            }
        });
    }

    @Override
    public void appendLog(PortID portID, String message, LogType type, LocalDateTime time) {
        if(!isSeparateMode(portID)) return;
        Platform.runLater(() -> {
            TextFlow textFlow = (portID == PortID.SEPARATE_1) ? firstTextFlow : secondTextFlow;
            String content = type == LogType.SPACE ? "\n" : "[" + FORMATTER.format(time) + "] " + message + "\n";
            Text text = new Text(content);
            if(type != LogType.SPACE) text.getProperties().put("date", time.toLocalDate());
            text.setFill(type.getColor());
            textFlow.getChildren().add(text);
        });
    }

    @Override
    public void writeControlSignal(PortID portID, String signal, boolean state) {
        Platform.runLater(() -> {
            if (portID == PortID.SEPARATE_1) {
                if (signal.equals(ControlSignal.RTS.name())) firstRTS.setSelected(state);
                if (signal.equals(ControlSignal.DTR.name())) firstDTR.setSelected(state);
            } else if (portID == PortID.SEPARATE_2) {
                if (signal.equals(ControlSignal.RTS.name())) secondRTS.setSelected(state);
                if (signal.equals(ControlSignal.DTR.name())) secondDTR.setSelected(state);
            }
        });
    }

    @Override
    public void readControlSignal(PortID portID, ControlSignal signal, boolean state) {
        Platform.runLater(() -> {
            if (portID == PortID.SEPARATE_1) {
                if (signal == ControlSignal.CTS) firstCTS.setSelected(state);
                if (signal == ControlSignal.DSR) firstDSR.setSelected(state);
            } else if (portID == PortID.SEPARATE_2) {
                if (signal == ControlSignal.CTS) secondCTS.setSelected(state);
                if (signal == ControlSignal.DSR) secondDSR.setSelected(state);
            }
        });
    }

    @Override
    public void appendUtilLog(PortID portID, String message, LogType type, LocalDateTime time) {
        if(!isSeparateMode(portID)) return;
        Platform.runLater(() -> {
            if(portID == PortID.SEPARATE_1) {
                Text text = new Text("[" + FORMATTER.format(time) + "] " + message + "\n");
                text.getProperties().put("date", time.toLocalDate());
                text.setFill(type.getColor());
                firstTextFlow.getChildren().add(text);
            } else if(portID == PortID.SEPARATE_2){
                Text text = new Text("[" + FORMATTER.format(time) + "] " + message + "\n");
                text.getProperties().put("date", time.toLocalDate());
                text.setFill(type.getColor());
                secondTextFlow.getChildren().add(text);
            }
        });
    }

    @Override
    public void updateCounter(PortID portID, int remainingTimes) {
        Platform.runLater(() -> {
            if (portID == PortID.SEPARATE_1) {
                firstCounterRemainingTimes.setText(remainingTimes > 0 ? "Remaining times: " + remainingTimes : "");
            } else if (portID == PortID.SEPARATE_2) {
                secondCounterRemainingTimes.setText(remainingTimes > 0 ? "Remaining times: " + remainingTimes : "");
            }
        });
    }

    @Override
    public void updateBusyState(PortID portID, boolean isBusy) {
        Platform.runLater(() -> {
            if (portID == PortID.SEPARATE_1) {
                getFirstIsSendBusy().set(isBusy);
            } else if (portID == PortID.SEPARATE_2) {
                getSecondIsSendBusy().set(isBusy);
            }
        });
    }
}
