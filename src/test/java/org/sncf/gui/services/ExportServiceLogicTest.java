package org.sncf.gui.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceLogicTest {

    static class TestableExportService extends ExportService {
        public TestableExportService() {
            super(null);
        }

        public String escapePublic(String input) {
            return super.escape(input);
        }
    }

    @Test
    void escape_shouldEscapeDoubleQuotesAndBackslashes() {
        TestableExportService service = new TestableExportService();

        String input = "Hello \"world\" and \\ path";
        String escaped = service.escapePublic(input);

        // Expected: Hello \"world\" and \\ path
        assertEquals("Hello \\\"world\\\" and \\\\ path", escaped);
    }

    @Test
    void escape_shouldHandleEmptyString() {
        TestableExportService service = new TestableExportService();

        String input = "";
        String escaped = service.escapePublic(input);

        assertEquals("", escaped);
    }

    @Test
    void escape_shouldLeaveSafeCharactersUnchanged() {
        TestableExportService service = new TestableExportService();

        String input = "ABC123";
        String escaped = service.escapePublic(input);

        assertEquals("ABC123", escaped);
    }
}
