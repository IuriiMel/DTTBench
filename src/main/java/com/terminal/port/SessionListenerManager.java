package com.terminal.port;

import com.terminal.controller.ModeController;

import java.time.LocalDateTime;

/**
 * Dispatches port events to the active {@link ModeController}.
 *
 * <p><b>Thread Safety:</b> Methods may be invoked from background threads
 * (e.g., {@link PortSession}, {@link com.terminal.script.ScriptEngine}).
 * Implementations of {@link ModeController} must ensure that UI updates
 * are performed on the JavaFX Application Thread, e.g. via
 * {@link javafx.application.Platform#runLater(Runnable)}.</p>
 */
public class SessionListenerManager implements SessionListener {
    private final ActiveControllerProvider provider;

    /**
     * Contract for resolving the currently active UI controller.
     */
    public interface ActiveControllerProvider {
        ModeController getActiveController();
    }

    public SessionListenerManager(ActiveControllerProvider provider) {
        this.provider = provider;
    }

    @Override
    public void portStateChanged(PortID portID, boolean isOpen) {
        provider.getActiveController().updatePortState(portID, isOpen);
    }

    @Override
    public void logMessage(PortID portID, String message, LogType type, LocalDateTime time) {
        provider.getActiveController().appendLog(portID, message, type, time);
    }

    @Override
    public void setControlSignal(PortID portID, String signal, boolean state) {
        provider.getActiveController().writeControlSignal(portID, signal, state);
    }

    @Override
    public void getControlSignal(PortID portID, ControlSignal signal, boolean state) {
        provider.getActiveController().readControlSignal(portID, signal, state);
    }

    @Override
    public void utilEvent(PortID portID, String message, LogType type, LocalDateTime time) {
        provider.getActiveController().appendUtilLog(portID, message, type, time);
    }

    @Override
    public void counterUpdate(PortID portID, int remaining) {
        provider.getActiveController().updateCounter(portID, remaining);
    }

    @Override
    public void scriptStateChanged(PortID portID, boolean isBusy) {
        provider.getActiveController().updateBusyState(portID, isBusy);
    }
}