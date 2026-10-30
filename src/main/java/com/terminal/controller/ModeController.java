package com.terminal.controller;

import com.terminal.port.PortID;
import com.terminal.port.SessionListener;
import com.terminal.port.SessionListener.LogType;
import com.terminal.port.SessionListener.ControlSignal;
import javafx.beans.property.BooleanProperty;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;

/**
 * Contract for UI controllers to receive asynchronous events from the application core.
 *
 * <p><b>Thread Safety:</b> Methods are invoked from background threads.
 * Implementations MUST wrap any JavaFX UI updates in {@code Platform.runLater()}.</p>
 */

public interface ModeController {

    /**
     * Inserts a command at the current caret position in the active text area.
     *
     * <p>After insertion, places the caret after the first closing parenthesis
     * {@code )} following the inserted command and adds a newline. If no closing
     * parenthesis is found, places the caret after the inserted command and adds
     * a newline.</p>
     */
    void insertCommandIntoCaret(String command);

    /**
     * Updates the UI to reflect the port's connection state.
     *
     * <p>Changes the status label color, disables/enables port selection controls,
     * and toggles the open/close button text based on the {@code isOpen} flag.</p>
     */
    void updatePortState(PortID portID, boolean isOpen);

    /**
     * Appends a formatted log message to the corresponding port's {@link TextFlow}.
     *
     * <p>Applies color-coding based on {@link SessionListener.LogType} and message content
     * (e.g., green for "opened", red for errors).</p>
     */
    void appendLog(PortID portID, String message, LogType type, LocalDateTime time);

    /**
     * Updates the UI toggle state for output control signals (RTS/DTR).
     *
     * <p>Reflects the state that was just written to the hardware port by the user or script.</p>
     */
    void writeControlSignal(PortID portID, String signal, boolean state);

    /**
     * Updates the UI toggle state for input control signals (CTS/DSR).
     *
     * <p>Reflects the current hardware state read asynchronously from the serial port.</p>
     */
    void readControlSignal(PortID portID, ControlSignal signal, boolean state);

    /**
     * Appends an external utility run/stop log to the corresponding port's {@link TextFlow}.
     *
     * <p>Uses distinct colors (dark cyan for running, dark blue for closing) to differentiate
     * from standard serial port communication logs.</p>
     */
    void appendUtilLog(PortID portID, String message, LogType type, LocalDateTime time);

    /**
     * Updates the visual counter displaying remaining script repetitions.
     *
     * <p>Clears the text if {@code remainingTimes} is 0.</p>
     */
    void updateCounter(PortID portID, int remaining);

    /**
     * Updates the {@link BooleanProperty} controlling the enabled state of step-mode buttons.
     */
    void updateBusyState(PortID portID, boolean isBusy);
}
