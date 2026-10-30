package com.terminal.handlers;

import com.terminal.handlers.devices.SC18IMHandler;
import com.terminal.port.SessionListener.LogType;
import com.terminal.utils.ByteUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Reference implementation of a binary protocol response handler.
 *
 * <p><b>Note:</b> This class serves as an <b>example idea</b> of how to parse
 * a custom binary protocol. The packet layout,
 * field lengths, and command codes are placeholders and must be adapted
 * to the actual device protocol.</p>
 */
public class DefaultCommandHandler implements CommandHandler {

    private static final int HEADER_LENGTH = 5;
    private static final int BODY_SIZE = 4;
    private static final int CRC_LENGTH = 2;

    @Override
    public boolean supports(String command) {
        return true;
    }

    @Override
    public int getResponseDelayMs() {
        return 50;
    }

    @Override
    public boolean expectsResponse(String command) {
        return true;
    }

    /**
     * Parses a binary response and extracts the numeric value based on the
     * reference protocol structure.
     *
     * <p><b>Example packet structure:</b></p>
     * <pre>
     * [HEADER: 5 bytes][BODY: 4 bytes][CRC: 2 bytes]
     * </pre>
     * <p>Extracts bytes 5..N-2, interprets the first 4 bytes as a
     * little-endian 32-bit integer, applies the divider from args (default 1.0),
     * and formats the result.</p>
     *
     * <p><b>Supported arguments:</b></p>
     * <ul>
     *   <li>{@code -d <value>} — divider for the raw integer</li>
     *   <li>{@code -u <unit>} — override the unit string</li>
     *   <li>{@code -c <comment>} — append a comment to the formatted output</li>
     *   <li>{@code -l [min-max]} — hard limits; appends "ERROR" if value is out of range</li>
     *   <li>{@code -lw [min-max]} — warning limits; appends "WARNING" if out of range</li>
     * </ul>
     *
     * <p><b>Special case:</b> Commands starting with {@link SC18IMHandler.Command#I2C_START} skip numeric formatting
     * and only append the comment.</p>
     *
     * @return parsed result with formatted string and plot value, or {@code null} if response is empty
     */
    @Override
    public CommandResult process(byte[] readMessage, String command, Map<String, String> args) {
        CommandResult result = new CommandResult();
        result.setType(LogType.RX);
        if (readMessage == null || readMessage.length == 0) {
            return null;
        }

        String rxData;
        if (command.startsWith("AT")) {
            rxData = ByteUtils.bytesToAscii(readMessage);
        } else {
            rxData = ByteUtils.bytesToHex(readMessage);
        }

        double calc = 0;
        String units = "";
        String comments = null;
        String limits = null;
        String limitsWarnings = null;

        if (readMessage.length > HEADER_LENGTH) {
            byte[] resultBytes = Arrays.copyOfRange(readMessage, HEADER_LENGTH, readMessage.length - CRC_LENGTH);
            int resultVoltage = 0;
            if (resultBytes.length >= BODY_SIZE) {
                resultVoltage = ByteBuffer.wrap(resultBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            }

            String key = rxData.substring(9, 11);
            units = CommandType.getUnits(CommandType.fromCode(key));

            double divider = 1;
            double lLimit = 0;
            double hLimit = 0;

            if (args != null) {
                divider = Double.parseDouble(args.getOrDefault("d", String.valueOf(divider)));
                units = args.getOrDefault("u", units);
                comments = args.getOrDefault("c", null);

                limits = args.getOrDefault("l", limits);
                if(limits != null) {
                    limits = limits.substring(limits.indexOf("[") + 1, limits.indexOf(']'));
                    lLimit = Double.parseDouble(limits.substring(0, limits.indexOf("-")));
                    hLimit = Double.parseDouble(limits.substring(limits.indexOf("-") + 1));
                }
                limitsWarnings = args.getOrDefault("lw", limitsWarnings);
                if(limitsWarnings != null) {
                    limitsWarnings = limitsWarnings.substring(limitsWarnings.indexOf("[") + 1, limitsWarnings.indexOf(']'));
                    lLimit = Double.parseDouble(limitsWarnings.substring(0, limitsWarnings.indexOf("-")));
                    hLimit = Double.parseDouble(limitsWarnings.substring(limitsWarnings.indexOf("-") + 1));
                }
            }

            calc = resultVoltage / divider;
            DecimalFormat format = new DecimalFormat("#.##");
            String message = format.format(calc).replace(",", ".");

            if (comments != null && !command.startsWith(SC18IMHandler.Command.I2C_START.name())) {
                rxData += " (" + message + " " + units + " " + comments + ") ";
            } else if (command.startsWith(SC18IMHandler.Command.I2C_START.name())) {
                rxData += " (" + comments + ") ";
            } else {
                rxData += " (" + message + " " + units + ") ";
            }

            if((limits != null) && (calc < lLimit || calc > hLimit)){
                rxData += "ERROR - values not in range";
                result.setType(LogType.ERROR);
            }
            if((limitsWarnings != null) && (calc < lLimit || calc > hLimit)){
                rxData += "WARNING - values not in range";
                result.setType(LogType.WARNING);
            }

            result.setPlotValue(calc);
            result.setPlotUnit(units);
            result.setComment(comments);
        }
        result.setFormattedData(rxData);
        return result;
    }

    /**
     * Maps response byte codes to measurement units.
     *
     * <p>Used to determine the unit based on the command type byte
     * in the response payload.</p>
     */
    public enum CommandType {
        INFORMATION(new String[]{"XX"}, ""),
        VOLTAGE(new String[]{"XX", "XX"}, "mV"),
        UNKNOWN(new String[]{}, "");

        private final List<String> codes;
        private final String units;

        CommandType(String[] codes, String units){
            this.codes = new ArrayList<>(Arrays.asList(codes));
            this.units = units;
        }

        public static CommandType fromCode(String code) {
            for (CommandType type : values()) {
                if (type.codes.contains(code.toUpperCase())) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        public static String getUnits(CommandType type){
            return type.units;
        }
    }
}