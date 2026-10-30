package com.terminal.controller;

import com.terminal.handlers.HandlerRegistry;
import com.terminal.plotter.PlotterManager;
import com.terminal.utils.FileUtils;
import com.terminal.port.PortID;
import com.terminal.port.PortManager;
import com.terminal.port.SessionListener.LogType;
import com.terminal.port.SessionListener.ControlSignal;
import com.terminal.script.ScriptEngine;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the "Coordinated Mode" UI, coordinating multiple serial ports through a shared workflow.
 *
 * <p>Implements {@link ModeController} to receive asynchronous events from background threads.
 * All UI updates triggered by port events are safely wrapped in {@code Platform.runLater()}.</p>
 */
public class CoordinatedModeController implements ModeController {

    @FXML private Label firstStatusLabel;
    @FXML private Label secondStatusLabel;
    @FXML private Button previousStepButton;
    @FXML private Button nextStepButton;
    @FXML private Button repeatStepButton;
    @FXML private Button resetStepButton;
    @FXML private Button stopButton;
    @FXML private ScrollPane dualScrollPane;
    @FXML private TextFlow dualTextFlow;
    @FXML private ComboBox<String> firstPortList;
    @FXML private ComboBox<String> firstBaudRate;
    @FXML private Button firstOpenDual;
    @FXML private RadioButton firstRTS;
    @FXML private RadioButton firstCTS;
    @FXML private RadioButton firstDTR;
    @FXML private RadioButton firstDSR;
    @FXML private ComboBox<String> secondPortList;
    @FXML private ComboBox<String> secondBaudRate;
    @FXML private Button secondOpenDual;
    @FXML private RadioButton secondRTS;
    @FXML private RadioButton secondCTS;
    @FXML private RadioButton secondDTR;
    @FXML private RadioButton secondDSR;
    @FXML private TextArea dualTextArea;
    @FXML private TextField dualTimes;
    @FXML private Text dualCounterRemainingTimes;
    @FXML private TextField dualIntervals;
    @FXML private Text dualStepMode;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox additionalPortsBox;

    private PortManager portManager;
    private PlotterManager plotterManager;
    private final HandlerRegistry registry = new HandlerRegistry();
    private ScriptEngine dualScriptEngine;
    private static final int INITIAL_ADDITIONAL_ROWS = 4;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");

    private final List<ComboBox<String>> additionalCombos = new ArrayList<>();
    private final List<Button> additionalOpenButtons = new ArrayList<>();
    private final List<Button> additionalAddButtons = new ArrayList<>();
    private int additionalPortIndex = 0;
    private final ObservableList<String> portNames = FXCollections.observableArrayList();
    private final ObservableList<String> baudRates = FXCollections.observableArrayList("9600", "19200", "38400", "57600", "115200");
    private final List<String> initCommand = new ArrayList<>(List.of("Send(XX)\n"));

    private List<String> dualStepModeCommands = new ArrayList<>();
    private int dualCommandIndex = -1;
    private BooleanProperty isSendBusy = new SimpleBooleanProperty(false);

    /**
     * Initializes UI bindings, context menus, additional ports managing, and auto-scroll behavior for the text flows.
     * Disables step mode buttons during script execution.
     */
    @FXML
    public void initialize(){
        Button appendButton = new Button("Append");
        Button removeButton = new Button("Remove");

        appendButton.setOnAction(e -> appendRow());
        removeButton.setOnAction(e -> removeRow());

        HBox buttons = new HBox(85, appendButton, removeButton);
        additionalPortsBox.getChildren().addAll(buttons);

        firstCTS.setDisable(true);
        firstDSR.setDisable(true);
        secondCTS.setDisable(true);
        secondDSR.setDisable(true);
        ContextMenu dualContext = FileUtils.contextMenu(dualTextFlow);
        FileUtils.contextMenu(dualTextArea);
        dualTextFlow.setUserData(PortID.COORDINATED_1);

        previousStepButton.disableProperty().bind(isSendBusy);
        nextStepButton.disableProperty().bind(isSendBusy);
        repeatStepButton.disableProperty().bind(isSendBusy);
        resetStepButton.disableProperty().bind(isSendBusy);
        stopButton.disableProperty().bind(isSendBusy);

        dualTextFlow.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> dualScrollPane.setVvalue(1.0));
        });
        additionalPortsBox.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
        dualScrollPane.setOnContextMenuRequested(event -> {
            dualContext.show(dualScrollPane, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Initializes the {@link ScriptEngine} instances for both ports and populates
     * the port and baud rate selection lists. Append initial rows for additional ports.
     */
    public void init() {
        dualScriptEngine = new ScriptEngine(portManager, portManager.getSessionListener(), registry);

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

        for(int i = 0; i < INITIAL_ADDITIONAL_ROWS; i++) appendRow();
    }

    private void getComPorts(){
        portNames.clear();
        portNames.addAll(portManager.getComPorts());
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
    public void firstOpen() {
        openPort(PortID.COORDINATED_1, firstOpenDual, firstPortList, firstBaudRate);
    }
    @FXML
    public void secondOpen() {
        openPort(PortID.COORDINATED_2, secondOpenDual, secondPortList, secondBaudRate);
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
        initPort(PortID.COORDINATED_1, dualScriptEngine);
    }
    @FXML
    public void initSecondPort() {
        initPort(PortID.COORDINATED_2, dualScriptEngine);
    }
    public void initPort(PortID portID, ScriptEngine scriptEngine){
        scriptEngine.execute(initCommand, 1, 0, portID);
    }

    /**
     * Executes the commands from the text area for the specified port.
     *
     * <p>Uses {@code 1} execution and {@code 0} interval when the corresponding
     * fields are empty.</p>
     */
    @FXML
    public void dualSend() {
        List<String> commands = new ArrayList<>(Arrays.asList(dualTextArea.getText().split("\\r?\\n")));
        int times = dualTimes.getText().isEmpty() ? 1 : Integer.parseInt(dualTimes.getText());
        int interval = dualIntervals.getText().isEmpty() ? 0 : Integer.parseInt(dualIntervals.getText());

        dualScriptEngine.execute(commands, times, interval, PortID.COORDINATED_1);
    }

    /**
     * Starts step-mode execution or, if it is already running, advances to the next command.
     * Highlights the command in the text area and triggers {@link ScriptEngine#executeStep}.
     *
     * @return the updated command index
     */
    public void dualNextStep() {
        dualStepModeCommands = getCommandsList(dualTextArea);
        dualStepMode.setFill(Color.GREEN);

        if(dualCommandIndex < dualStepModeCommands.size() - 1) {
            dualCommandIndex++;
            highlightCommand(dualTextArea, dualStepModeCommands, dualCommandIndex);
            dualScriptEngine.executeStep(dualStepModeCommands.get(dualCommandIndex), PortID.COORDINATED_1);
        }
    }

    /**
     * Reverts the step-mode execution to the previous command and highlights it.
     *
     * @return the updated command index
     */
    public void dualPreviousStep() {
        dualStepModeCommands = getCommandsList(dualTextArea);

        if(dualCommandIndex > 0) {
            dualCommandIndex--;
            highlightCommand(dualTextArea, dualStepModeCommands, dualCommandIndex);
            dualScriptEngine.executeStep(dualStepModeCommands.get(dualCommandIndex), PortID.COORDINATED_1);
        }
    }

    /**
     * Repeats the highlighted command.
     */
    public void dualRepeatCommand() {
        if(dualCommandIndex == -1) return;
        dualScriptEngine.executeStep(dualStepModeCommands.get(dualCommandIndex), PortID.COORDINATED_1);
    }

    public void dualRestartCommand() {
        dualStopStepMode();
        dualNextStep();
    }

    public void dualStopStepMode() {
        dualStepModeCommands.clear();
        dualStepMode.setFill(Color.BLACK);
        dualCommandIndex = -1;
        highlightCommand(dualTextArea, dualStepModeCommands, dualCommandIndex);

    }

    public void addPrefix(PortID id){
        String command = "Send()";
        insertCommandTemplate(id, command);
    }

    public void addFirstPrefix() {
        String command = "Send()";
        insertCommandTemplate(PortID.COORDINATED_1, command);
    }

    public void addSecondPrefix() {
        String command = "Send()";
        insertCommandTemplate(PortID.COORDINATED_2, command);
    }

    public void addTimeout() {
        String template = "Sleep()";
        TextArea textArea = dualTextArea;
        int position = textArea.getCaretPosition();
        textArea.insertText(position, template);

        int inside = position + template.indexOf("(") + 1;
        if(inside >= 0 && inside <= textArea.getText().length()) textArea.positionCaret(inside);
        textArea.requestFocus();
    }

    public void appendText(String message, Color color){
        Platform.runLater(() -> {
            Text text = new Text(message);
            text.setFill(color);
            dualTextFlow.getChildren().add(text);
        });
    }

    @FXML
    public void addFirstRTS() {
        String command = "RTS()";
        insertCommandTemplate(PortID.COORDINATED_1, command);
    }
    @FXML
    public void addFirstDTR() {
        String command = "DTR()";
        insertCommandTemplate(PortID.COORDINATED_1, command);
    }
    @FXML
    public void addSecondRTS() {
        String command = "RTS()";
        insertCommandTemplate(PortID.COORDINATED_2, command);
    }
    @FXML
    public void addSecondDTR() {
        String command = "DTR()";
        insertCommandTemplate(PortID.COORDINATED_2, command);
    }

    @FXML
    public void setFirstRTS() {
        portManager.getSession(PortID.COORDINATED_1).setRTS(firstRTS.isSelected());
    }
    @FXML
    public void setFirstDTR() {
        portManager.getSession(PortID.COORDINATED_1).setDTR(firstDTR.isSelected());
    }
    @FXML
    public void setSecondRTS() {
        portManager.getSession(PortID.COORDINATED_2).setRTS(secondRTS.isSelected());
    }
    @FXML
    public void setSecondDTR() {
        portManager.getSession(PortID.COORDINATED_2).setDTR(secondDTR.isSelected());
    }

    //========================================== Additional ports ==========================================
    /**
     * Adds a new dynamic port row to the UI if the ADDITIONAL port limit has not been reached.
     */
    public void appendRow() {
        int limit = PortID.ADDITIONAL_PORTS_COUNT;
        if(additionalPortIndex < limit){
            HBox row = createRow();
            int index = additionalPortsBox.getChildren().size() - 1;
            additionalPortsBox.getChildren().add(index, row);
        }
        else appendText("Additional ports limit is " + limit + "\n", Color.RED);
    }

    /**
     * Removes the last dynamic port row from the UI and closes its session.
     */
    public void removeRow() {
        int rowCount = additionalPortsBox.getChildren().size() - 1;
        if(rowCount == 0) return;

        PortID id = PortID.values()[additionalPortsBox.getChildren().size()];
        portManager.removeSession(id);

        additionalPortsBox.getChildren().remove(rowCount - 1);
        getAdditionalCombos().remove(rowCount - 1);
        getAdditionalOpenButtons().remove(rowCount - 1);
        getAdditionalAddButtons().remove(rowCount - 1);

        additionalPortIndex--;
    }

    /**
     * Creates a UI row for a dynamic port with selector, open/close, and prefix controls.
     *
     * <p>Assigns the next available {@code ADDITIONAL_*} {@link PortID} and binds button
     * actions to {@link com.terminal.port.PortManager} session lifecycle.</p>
     */
    public HBox createRow(){
        HBox row = new HBox(5);

        ComboBox<String> ports = new ComboBox<>();
        ports.setPrefWidth(80);
        ports.getItems().addAll(portNames);
        if(!portNames.isEmpty()){
            ports.setValue(portNames.getFirst());
        }

        ports.setOnMouseClicked(event -> {
            mouseEvent(ports);
        });

        Button openButton = new Button("Open");
        openButton.setPrefWidth(50);
        Button addButton = new Button("Prefix");

        getAdditionalCombos().add(ports);
        getAdditionalOpenButtons().add(openButton);
        getAdditionalAddButtons().add(addButton);

        PortID id = additionalPortID();

        openButton.setOnMouseClicked(event -> {
            if (openButton.getText().equals("Open")) {
                boolean isOpen = portManager.getSession(id).open(ports.getSelectionModel().getSelectedItem(), 9600);
                ports.setDisable(isOpen);
                openButton.setText(isOpen ? "Close" : "Open");
            } else {
                portManager.getSession(id).close();
                ports.setDisable(false);
                openButton.setText("Open");
            }
        });
        addButton.setOnMouseClicked(event -> {
            addPrefix(id);
        });

        row.getChildren().addAll(ports, openButton, addButton);
        return row;
    }

    /**
     * Returns the next available {@code ADDITIONAL_*} {@link PortID} and increments the index.
     */
    private PortID additionalPortID(){
        int index = additionalPortIndex++;
        return PortID.values()[PortID.ADDITIONAL_1.ordinal() + index];
    }
    //======================================================================================================

    public List<String> getCommandsList(TextArea textArea){
        return new ArrayList<>(Arrays.asList(textArea.getText().split("\\r?\\n")));
    }

    public void dualStop() {
        dualScriptEngine.stop();
    }

    public BooleanProperty getIsSendBusy(){
        return isSendBusy;
    }

    public void setPortManager(PortManager manager){
        portManager = manager;
    }

    public void setPlotterManager(PlotterManager manager){
        plotterManager = manager;
    }

    public List<ComboBox<String>> getAdditionalCombos(){
        return additionalCombos;
    }

    public List<Button> getAdditionalOpenButtons(){
        return additionalOpenButtons;
    }

    public List<Button> getAdditionalAddButtons(){
        return additionalAddButtons;
    }

    public boolean isDualMode(PortID portID){
        return !(portID == PortID.SEPARATE_1 || portID == PortID.SEPARATE_2);
    }

    private boolean needsPortName(LogType type) {
        return type == LogType.TX
                || type == LogType.RX
                || type == LogType.WARNING
                || type == LogType.ERROR;
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

    /**
     * Inserts a command template for the specified port at the current caret position.
     * After insertion, places the caret after the opening parenthesis {@code (}
     * and returns focus to the text area.</p>
     */
    public void insertCommandTemplate(PortID id, String command){
        ComboBox<String> combo;

        if(id.equals(PortID.COORDINATED_1)) combo = firstPortList;
        else if(id.equals(PortID.COORDINATED_2)) combo = secondPortList;
        else{
            int index = id.ordinal() - PortID.MAIN_PORTS_COUNT;
            HBox box = (HBox) additionalPortsBox.getChildren().get(index);
            combo = (ComboBox<String>) box.getChildren().getFirst();
        }
        String portName = combo.getSelectionModel().getSelectedItem();
        String template = portName + ": " + command;
        TextArea textArea = dualTextArea;

        int position = textArea.getCaretPosition();
        textArea.insertText(position, template);

        int inside = position + template.indexOf("(") + 1;
        if(inside >= 0 && inside <= textArea.getText().length()) textArea.positionCaret(inside);
        dualTextArea.requestFocus();
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

    @Override
    public void insertCommandIntoCaret(String command) {
        int caret = dualTextArea.getCaretPosition();

        dualTextArea.insertText(caret, command);

        int nextPosition = caret + command.length();
        String text = dualTextArea.getText();

        int finalPosition = text.indexOf(")", nextPosition);
        if (finalPosition != -1) {
            dualTextArea.positionCaret(finalPosition + 1);
            dualTextArea.insertText(finalPosition + 1, "\n");
            dualTextArea.positionCaret(finalPosition + 2);
        } else {
            dualTextArea.positionCaret(nextPosition);
            dualTextArea.insertText(nextPosition, "\n");
            dualTextArea.positionCaret(nextPosition + 1);
        }
    }

    @Override
    public void updatePortState(PortID portID, boolean isOpen) {
        Platform.runLater(() -> {
            if (portID == PortID.COORDINATED_1) {
                updateStatusLabel(firstStatusLabel, isOpen);
                firstPortList.setDisable(isOpen);
                firstBaudRate.setDisable(isOpen);
                firstOpenDual.setText(isOpen ? "Close" : "Open");
            } else if (portID == PortID.COORDINATED_2) {
                updateStatusLabel(secondStatusLabel, isOpen);
                secondPortList.setDisable(isOpen);
                secondBaudRate.setDisable(isOpen);
                secondOpenDual.setText(isOpen ? "Close" : "Open");
            }
        });
    }

    @Override
    public void appendLog(PortID portID, String message, LogType type, LocalDateTime time) {
        if(!isDualMode(portID)) return;
        String com;
        if (needsPortName(type)) {
            com = "(" + portManager.getSession(portID).getPortName() + ") ";
        } else {
            com = "";
        }
        Platform.runLater(() -> {
            TextFlow textFlow = dualTextFlow;
            String content = type == LogType.SPACE ? "\n" : "[" + FORMATTER.format(time) + "] " + com + message + "\n";
            Text text = new Text(content);
            if(type != LogType.SPACE) text.getProperties().put("date", time.toLocalDate());
            text.setFill(type.getColor());
            textFlow.getChildren().add(text);
        });
    }

    @Override
    public void writeControlSignal(PortID portID, String signal, boolean state) {
        Platform.runLater(() -> {
            if (portID == PortID.COORDINATED_1) {
                if (signal.equals(ControlSignal.RTS.name())) firstRTS.setSelected(state);
                if (signal.equals(ControlSignal.DTR.name())) firstDTR.setSelected(state);
            } else if (portID == PortID.COORDINATED_2) {
                if (signal.equals(ControlSignal.RTS.name())) secondRTS.setSelected(state);
                if (signal.equals(ControlSignal.DTR.name())) secondDTR.setSelected(state);
            }
        });
    }

    @Override
    public void readControlSignal(PortID portID, ControlSignal signal, boolean state) {
        Platform.runLater(() -> {
            if (portID == PortID.COORDINATED_1) {
                if (signal == ControlSignal.CTS) firstCTS.setSelected(state);
                if (signal == ControlSignal.DSR) firstDSR.setSelected(state);
            } else if (portID == PortID.COORDINATED_2) {
                if (signal == ControlSignal.CTS) secondCTS.setSelected(state);
                if (signal == ControlSignal.DSR) secondDSR.setSelected(state);
            }
        });
    }

    @Override
    public void appendUtilLog(PortID portID, String message, LogType type, LocalDateTime time) {
        if(!isDualMode(portID)) return;
        Platform.runLater(() -> {
            Text text = new Text("[" + FORMATTER.format(time) + "] " + message + "\n");
            text.getProperties().put("date", time.toLocalDate());
            text.setFill(type.getColor());
            dualTextFlow.getChildren().add(text);
        });
    }

    @Override
    public void updateCounter(PortID portID, int remainingTimes) {
        if(!isDualMode(portID)) return;
        Platform.runLater(() -> {
            dualCounterRemainingTimes.setText(remainingTimes > 0 ? "Remaining times: " + remainingTimes : "");
        });
    }

    @Override
    public void updateBusyState(PortID portID, boolean isBusy) {
        if(!isDualMode(portID)) return;
        Platform.runLater(() -> {
            getIsSendBusy().set(isBusy);
        });
    }
}
