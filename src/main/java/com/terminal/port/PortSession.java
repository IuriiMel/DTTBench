package com.terminal.port;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import com.terminal.port.SessionListener.LogType;

/**
 * Manages the lifecycle and I/O operations of a single serial port session.
 *
 * <p>Wraps {@link SerialPort} to provide blocking read operations with configurable
 * timeouts, and dispatches hardware signal changes (CTS/DSR) to the {@link SessionListener}.</p>
 *
 * <p><b>Thread Safety:</b> The open state can be safely accessed from multiple threads.
 *  * I/O operations are designed to be called sequentially from a single background thread.</p>
 */
public class PortSession {
    /** Maximum time to wait for the first byte of a response. */
    private static final int RECEIVE_TIMEOUT_MS = 30000;
    /** Polling interval while waiting for data to arrive. */
    private static final int WAIT_STEP_MS = 10;
    /** Delay between reads to ensure fragmented packets are fully assembled. */
    private static final int INTER_PACKET_DELAY_MS = 20;

    private final PortID id;
    private final SessionListener listener;
    private SerialPort serialPort;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);

    public PortSession(PortID id, SessionListener listener) {
        this.id = id;
        this.listener = listener;
    }

    /**
     * Opens the specified port.
     *
     * <p>If the session is already open on the same port name, returns {@code true} immediately.
     * If it is open on a different port, closes the current port before opening the new one.</p>
     *
     * @return {@code true} if the port was successfully opened or was already open on this target
     */
    public boolean open(String portName, int baudRate) {
        if(portName == null) return false;
        String targetName = portName.toUpperCase();

        if (isOpen.get() && serialPort != null && serialPort.getSystemPortName().equalsIgnoreCase(targetName)) return true;
        if (isOpen.get() && serialPort != null) close();

        serialPort = SerialPort.getCommPort(targetName);
        serialPort.clearRTS();
        serialPort.clearDTR();
        serialPort.setComPortParameters(baudRate, 8, 1, 0);

        boolean success = serialPort.openPort();
        if (success) {
            isOpen.set(true);
            setupSignalListener();
            notifyStateChange(true);
            log("Serial port " + targetName + " opened", LogType.PORT_OPENED);
        } else {
            log("Serial port " + targetName + " opening error", LogType.ERROR);
        }
        return success;
    }

    public boolean close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.removeDataListener();
            boolean success = serialPort.closePort();
            if(success) {
                isOpen.set(false);
                notifyStateChange(false);
                log("Serial port " + serialPort.getSystemPortName() + " closed", LogType.PORT_CLOSED);
            }
            return success;
        }
        return false;
    }

    /**
     * @return {@code true} if the data was fully written to the port buffer
     */
    public boolean send(byte[] data) {
        if (!isOpen.get() || serialPort == null) {
            log("Port not open", LogType.ERROR);
            return false;
        }
        int written = serialPort.writeBytes(data, data.length);
        return written == data.length;
    }

    /**
     * Reads all available bytes from the port, blocking until data arrives or timeout occurs.
     *
     * <p><b>Important:</b> Uses a custom polling loop with {@link #INTER_PACKET_DELAY_MS}
     * to ensure complete packet reception, rather than relying on jSerialComm's event-driven reading.</p>
     *
     * @return the received byte array, or an empty array if timeout occurs
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public byte[] receive() throws InterruptedException {
        waitForData();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int available;

        while ((available = serialPort.bytesAvailable()) > 0) {
            Thread.sleep(INTER_PACKET_DELAY_MS);
            byte[] buffer = new byte[available];
            int read = serialPort.readBytes(buffer, buffer.length);
            if (read > 0) out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private void waitForData() throws InterruptedException {
        int waited = 0;
        while (serialPort.bytesAvailable() == 0 && waited < RECEIVE_TIMEOUT_MS) {
            Thread.sleep(WAIT_STEP_MS);
            waited += WAIT_STEP_MS;
        }
    }

    /**
     * Attaches a listener exclusively for hardware flow control signals (CTS/DSR).
     * Data reception is handled synchronously in {@link #receive()}.
     */
    private void setupSignalListener() {
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_CTS | SerialPort.LISTENING_EVENT_DSR;
            }
            @Override
            public void serialEvent(SerialPortEvent event) {
                if (listener != null) {
                    listener.getControlSignal(id, SessionListener.ControlSignal.CTS, serialPort.getCTS());
                    listener.getControlSignal(id, SessionListener.ControlSignal.DSR, serialPort.getDSR());
                }
            }
        });
    }

    private void notifyStateChange(boolean isOpen) {
        if (listener != null) listener.portStateChanged(id, isOpen);
    }

    private void log(String message, LogType type) {
        if (listener != null) listener.logMessage(id, message, type, LocalDateTime.now());
    }

    public PortID getId() {
        return id;
    }
    public SerialPort getSerialPort() {
        return serialPort;
    }
    public boolean isOpen() {
        return isOpen.get();
    }
    public String getPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : "UNKNOWN";
    }
    public void setRTS(boolean state) {
        if (serialPort != null) {
            if(state) serialPort.setRTS();
            else serialPort.clearRTS();
        }
    }
    public void setDTR(boolean state) {
        if (serialPort != null) {
            if(state) serialPort.setDTR();
            else serialPort.clearDTR();
        }
    }
}
