package com.terminal.utils;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class ByteUtils {

    private static final HexFormat HEX = HexFormat.ofDelimiter(" ");

    public static byte[] hexToBytes(String string){
        return HexFormat.of().parseHex(string.replaceAll("\\s+", ""));
    }

    public static String bytesToHex(byte[] bytes) {
        return HEX.formatHex(bytes).toUpperCase();
    }

    public static String bytesToAscii(byte[] readMessage){
        return new String(readMessage, StandardCharsets.US_ASCII);
    }

    public static byte[] asciiToBytes(String string){
        return (string + "\r\n").getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] crc16(byte[] data, CRC16Mode mode){
        int crc = mode.getInitialValue();
        int poly = mode.getPolynomial();
        int xorOut = mode.getXorOut();

        for (byte b : data) {
            int val = b & 0xFF;
            if (mode.isRefIn()) {
                val = Integer.reverse(val) >>> 24;
            }
            crc ^= (val << 8);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ poly;
                } else {
                    crc <<= 1;
                }
            }
            crc &= 0xFFFF;
        }
        if (mode.isRefOut()) {
            crc = Integer.reverse(crc) >>> 16;
        }
        crc ^= xorOut;
        crc &= 0xFFFF;

        byte[] result = {0, 0};
        result[0] = (byte) (crc & 0xFF);
        result[1] = (byte) ((crc >> 8) & 0xFF);

        return result;
    }

    public enum CRC16Mode{
        ARC(0x8005, 0x0000, true, true, 0x0000),
        IBM_SDLC(0x1021, 0xFFFF, true, true, 0xFFFF);

        private final int polynomial;
        private final int initialValue;
        private final boolean refIn;
        private final boolean refOut;
        private final int xorOut;

        CRC16Mode(int polynomial, int initialValue, boolean refIn, boolean refOut, int xorOut) {
            this.polynomial = polynomial;
            this.initialValue = initialValue;
            this.refIn = refIn;
            this.refOut = refOut;
            this.xorOut = xorOut;
        }

        public int getPolynomial() {
            return polynomial;
        }

        public int getInitialValue() {
            return initialValue;
        }

        public boolean isRefIn() {
            return refIn;
        }

        public boolean isRefOut() {
            return refOut;
        }

        public int getXorOut() {
            return xorOut;
        }
    }
}
