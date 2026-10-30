package com.terminal.port;

import com.fazecast.jSerialComm.SerialPort;

import java.util.*;

/**
 * Registry and factory for {@link PortSession} instances.
 * Also provides utilities for discovering available system COM ports.
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. All methods must be called
 * from the JavaFX Application Thread.</p>
 */
public class PortManager {
    private final Map<PortID, PortSession> sessions = new HashMap<>();
    private final SessionListener globalListener;

    public PortManager(SessionListener globalListener) {
        this.globalListener = globalListener;
    }

    /**
     * @return an unmodifiable copy of the current sessions map.
     */
    public Map<PortID, PortSession> getSessions(){
        return Map.copyOf(sessions);
    }

    /**
     * Returns the session for the given port ID, creating it if it does not exist.
     */
    public PortSession getSession(PortID id) {
        return sessions.computeIfAbsent(id, portID -> new PortSession(portID, globalListener));
    }

    /**
     * Closes and removes the session for the given port ID.
     *
     * @param id the port identifier
     */
    public boolean removeSession(PortID id){
        PortSession session = sessions.remove(id);
        if (session != null) {
            return session.close();
        }
        return false;
    }

    public SessionListener getSessionListener(){
        return globalListener;
    }

    /**
     * Returns available COM port names sorted by their numeric port number,
     * excluding ports associated with com0com.
     */
    public List<String> getComPorts() {
        List<String> portNames = new ArrayList<>();

        for(SerialPort serialPort : SerialPort.getCommPorts()){
            String name = serialPort.getSystemPortName();
            String description = serialPort.getPortDescription();

            if(!description.toLowerCase(Locale.ROOT).contains("com0com")){
                portNames.add(name);
            }
        }
        portNames.sort((a,b) -> {
            int A = Integer.parseInt(a.replaceAll("[^0-9]", ""));
            int B = Integer.parseInt(b.replaceAll("[^0-9]", ""));
            return Integer.compare(A, B);
        });

        return portNames;
    }

    /**
     * Opens a serial port for the script's {@code -open} command.
     *
     * <p>If a session is already associated with the specified port, that session
     * is reused. Otherwise, the session identified by the specified {@code portID}
     * is used, or an available additional session is created.</p>
     *
     * @param portID the preferred session identifier
     * @param portName the name of the serial port to open
     * @param baudRate the communication baud rate
     * @return the session associated with the opened port, or {@code null} if
     *         the port is unavailable, cannot be opened, or no session is available
     */
    public PortSession findAndOpen(PortID portID, String portName, int baudRate) {
        String targetPort = portName.toUpperCase();

        if(!getComPorts().contains(targetPort)) return null;

        if(sessions.isEmpty()) {
            PortSession session = getSession(portID);
            if (session.open(targetPort, baudRate)){
                return session;
            }
            removeSession(portID);
        }

        for (PortSession session : sessions.values()) {
            if (session.getPortName().equalsIgnoreCase(targetPort)) {
                if(session.open(targetPort, baudRate)){
                    return session;
                }
                removeSession(portID);
            }
        }

        PortSession targetSession = getSession(portID);
        if (!targetSession.isOpen()) {
            if (targetSession.open(targetPort, baudRate)) {
                return targetSession;
            }
            removeSession(portID);
        }

        for (PortID id : PortID.values()) {
            if (id.name().startsWith("ADDITIONAL_")) {
                PortSession session = getSession(id);
                if (!session.isOpen()) {
                    if (session.open(targetPort, baudRate)) {
                        return session;
                    }
                    removeSession(id);
                }
            }
        }
        return null;
    }

    /**
     * Closes a serial port for the script {@code -close} command.
     *
     * @return {@code true} if the port was found and closed successfully;
     *         {@code false} otherwise
     */
    public boolean findAndClose(String portName) {
        String targetPort = portName.toUpperCase();
        for (Map.Entry<PortID, PortSession> entry : sessions.entrySet()) {
            PortSession session = entry.getValue();
            if (session.getPortName().equalsIgnoreCase(targetPort)) {
                return removeSession(session.getId());
            }
        }
        return false;
    }

    public void closeAll() {
        sessions.values().forEach(PortSession::close);
        sessions.clear();
    }
}
