package com.terminal.port;

/**
 * Logical slot identifier for serial port sessions {@link com.terminal.port.PortSession}.
 *
 * <p>Each {@code SEPARATE_*} and {@code COORDINATED_*} value identifies a specific
 * port panel in the corresponding UI mode.</p>
 *
 * <p>Enum values are grouped by UI mode:</p>
 * <ul>
 *   <li>{@code SEPARATE_*} — used by SeparateMode controller</li>
 *   <li>{@code COORDINATED_*} — used by DualMode controller</li>
 *   <li>{@code ADDITIONAL_*} — dynamically added via {@link com.terminal.controller.CoordinatedModeController#createRow()}
 *       and managed by {@code additionalPortsBox} in the UI</li>
 * </ul>
 *
 * <p>UI components and business logic reference these enum values instead of physical
 * port names. {@link com.terminal.port.PortSession} binds each {@code PortID} to an actual
 * COM port, while {@link com.terminal.port.PortManager} manages the lifecycle of these sessions.</p>
 */
public enum PortID {
    SEPARATE_1, SEPARATE_2, COORDINATED_1, COORDINATED_2,
    ADDITIONAL_1, ADDITIONAL_2, ADDITIONAL_3, ADDITIONAL_4,
    ADDITIONAL_5, ADDITIONAL_6, ADDITIONAL_7, ADDITIONAL_8,
    ADDITIONAL_9, ADDITIONAL_10;

    public static final int ADDITIONAL_PORTS_COUNT = countAdditionalPorts();
    public static final int MAIN_PORTS_COUNT = PortID.values().length - ADDITIONAL_PORTS_COUNT;

    private static int countAdditionalPorts(){
        int ports = 0;
        for(PortID portID : PortID.values()) if(portID.name().contains("ADDITIONAL")) ports++;
        return ports;
    }
}
