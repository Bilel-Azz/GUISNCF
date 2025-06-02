package org.sncf.gui.serial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerialTransmitterTest {

    @Test
    void convertBitsToHex_shouldConvertBinaryToHex() {
        String bits = "0100101000111111"; // 0x4A 0x3F
        String expectedHex = "4A 3F";
        String actualHex = invokeConvertBitsToHex(bits);
        assertEquals(expectedHex, actualHex);
    }

    @Test
    void convertBitsToHex_shouldHandleIncompleteFinalByteAsPartialByte() {
        String bits = "0100101"; // 7 bits => decimal = 37
        String expectedHex = "25"; // 0x25 = 37
        String actualHex = invokeConvertBitsToHex(bits);
        assertEquals(expectedHex, actualHex);
    }

    @Test
    void convertBitsToText_shouldConvertBinaryToText() {
        String bits = "01001000011001010110110001101100"; // "Hell"
        String expectedText = "Hell";
        String actualText = invokeConvertBitsToText(bits);
        assertEquals(expectedText, actualText);
    }

    @Test
    void convertBitsToText_shouldReplaceNonPrintableCharactersWithDot() {
        String bits = "0000000100000010"; // ASCII 1 and 2 (non-printables)
        String actualText = invokeConvertBitsToText(bits);
        assertEquals("..", actualText);
    }

    private String invokeConvertBitsToHex(String bits) {
        try {
            var method = SerialTransmitter.class.getDeclaredMethod("convertBitsToHex", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, bits);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeConvertBitsToText(String bits) {
        try {
            var method = SerialTransmitter.class.getDeclaredMethod("convertBitsToText", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, bits);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
