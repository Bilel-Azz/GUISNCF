package org.sncf.gui.model;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class FilterRuleTest {

    @Test
    void constructor_shouldInitializePatternAndColorCorrectly() {
        // Given
        String expectedPattern = "ERROR";
        String hexColor = "#FF0000";

        // When
        FilterRule rule = new FilterRule(expectedPattern, hexColor);

        // Then
        assertEquals(expectedPattern, rule.pattern);
        assertEquals(Color.RED, rule.color);
    }

    @Test
    void constructor_shouldHandleLowercaseHexColor() {
        FilterRule rule = new FilterRule("INFO", "#00ff00");
        assertEquals(new Color(0, 255, 0), rule.color);
    }

    @Test
    void constructor_shouldThrowExceptionForInvalidHexColor() {
        assertThrows(NumberFormatException.class, () -> new FilterRule("DEBUG", "notAColor"));
    }
}
