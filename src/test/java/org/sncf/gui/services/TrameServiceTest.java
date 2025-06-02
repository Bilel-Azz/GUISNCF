package org.sncf.gui.services;

import org.junit.jupiter.api.Test;
import org.sncf.gui.model.FilterRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrameServiceTest {

    // === convertBitsToHex ===

    @Test
    void convertBitsToHex_shouldConvertBinaryToHex() {
        TrameService service = new TrameService(null, null);
        String bits = "0100101000101111"; // 0x4A 0x2F
        String result = service.convertBitsToHex(bits);
        assertEquals("4A 2F", result);
    }

    @Test
    void convertBitsToHex_shouldReturnEmptyForInvalidLength() {
        TrameService service = new TrameService(null, null);
        assertEquals("", service.convertBitsToHex("0100101")); // 7 bits
    }

    @Test
    void convertBitsToHex_shouldReturnEmptyForNull() {
        TrameService service = new TrameService(null, null);
        assertEquals("", service.convertBitsToHex(null));
    }

    // === processBits ===

    @Test
    void processBits_shouldGenerateTrameEntryUsingDictionary() {
        DictionaryService mockDictionary = new DictionaryService(null) {
            @Override
            public String convertHexToText(String hexLine) {
                return "TEXT_" + hexLine;
            }
        };

        TrameService service = new TrameService(null, mockDictionary);
        String inputBits = "0100101000101111"; // 4A 2F
        TrameService.TrameEntry entry = service.processBits(inputBits);

        assertEquals("0100101000101111", entry.bits);
        assertEquals("4A 2F", entry.hex);
        assertEquals("TEXT_4A 2F", entry.text);
    }

    // === matchesFilter ===

    @Test
    void matchesFilter_shouldReturnTrueIfAnyFieldMatches() {
        TrameService.TrameEntry entry = new TrameService.TrameEntry("101010", "AA BB", "Hello");
        List<FilterRule> filters = List.of(
                new FilterRule("AA", "#FF0000"),
                new FilterRule("Nothing", "#00FF00")
        );

        TrameService service = new TrameService(null, null);
        assertTrue(service.matchesFilter(entry, filters));
    }

    @Test
    void matchesFilter_shouldReturnFalseIfNoMatch() {
        TrameService.TrameEntry entry = new TrameService.TrameEntry("101010", "AA BB", "Hello");
        List<FilterRule> filters = List.of(
                new FilterRule("ZZ", "#FF0000")
        );

        TrameService service = new TrameService(null, null);
        assertFalse(service.matchesFilter(entry, filters));
    }

    @Test
    void matchesFilter_shouldReturnTrueIfFiltersNull() {
        TrameService.TrameEntry entry = new TrameService.TrameEntry("bits", "hex", "text");
        TrameService service = new TrameService(null, null);
        assertTrue(service.matchesFilter(entry, null));
    }

    // === filterTrames ===

    @Test
    void filterTrames_shouldReturnOnlyMatchingTrames() {
        TrameService.TrameEntry match = new TrameService.TrameEntry("bits123", "4A", "Hello");
        TrameService.TrameEntry noMatch = new TrameService.TrameEntry("zzz", "99", "Nope");

        List<TrameService.TrameEntry> source = List.of(match, noMatch);
        List<FilterRule> filters = List.of(new FilterRule("4A", "#00FF00"));

        TrameService service = new TrameService(null, null);
        List<TrameService.TrameEntry> result = service.filterTrames(source, filters);

        assertEquals(1, result.size());
        assertEquals("bits123", result.get(0).bits);
    }

    @Test
    void filterTrames_shouldReturnAllIfNoFilters() {
        TrameService.TrameEntry a = new TrameService.TrameEntry("1", "A", "X");
        TrameService.TrameEntry b = new TrameService.TrameEntry("2", "B", "Y");
        List<TrameService.TrameEntry> source = List.of(a, b);

        TrameService service = new TrameService(null, null);
        List<TrameService.TrameEntry> result = service.filterTrames(source, null);

        assertEquals(2, result.size());
    }

    // === TrameEntry ===

    @Test
    void trameEntry_shouldStoreValuesCorrectly() {
        TrameService.TrameEntry entry = new TrameService.TrameEntry("bits", "hex", "text");
        assertEquals("bits", entry.bits);
        assertEquals("hex", entry.hex);
        assertEquals("text", entry.text);
    }
}
