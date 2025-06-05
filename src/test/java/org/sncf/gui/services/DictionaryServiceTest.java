package org.sncf.gui.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryServiceTest {

    @Test
    void convertHexToText_shouldUseDictionaryMatchFirst() {
        DictionaryService service = new DictionaryService(null) {
            @Override
            public List<DictionaryEntry> getAllEntries() {
                return List.of(
                        new DictionaryEntry("48 45", "Salut"),
                        new DictionaryEntry("4C 4C", "Double L")
                );
            }
        };

        String input = "48 45 4C 4C 4F";
        String result = service.convertHexToText(input);

        // "48 45" → "Salut", "4C 4C" → "Double L", "4F" → "O"
        assertEquals("SalutDouble LO", result);
    }

    @Test
    void convertHexToText_shouldFallbackToAsciiForNonMatchingHex() {
        DictionaryService service = new DictionaryService(null) {
            @Override
            public List<DictionaryEntry> getAllEntries() {
                return List.of(); // dictionnaire vide
            }
        };

        String input = "48 65 6C 6C 6F"; // "Hello"
        String result = service.convertHexToText(input);
        assertEquals("Hello", result);
    }

    @Test
    void convertHexToText_shouldHandleNonPrintableCharacters() {
        DictionaryService service = new DictionaryService(null) {
            @Override
            public List<DictionaryEntry> getAllEntries() {
                return List.of();
            }
        };

        String input = "01 02 20"; // 1, 2 = non-imprimables, 32 = espace ASCII
        String result = service.convertHexToText(input);

        // Vérification exacte basée sur comportement réel
        assertEquals("..", result.trim()); // On ignore les espaces finaux
    }

    @Test
    void convertHexToText_shouldHandleInvalidHexGracefully() {
        DictionaryService service = new DictionaryService(null) {
            @Override
            public List<DictionaryEntry> getAllEntries() {
                return List.of();
            }
        };

        String input = "GG ZZ"; // invalid hex
        String result = service.convertHexToText(input);
        assertEquals("..", result);
    }

    @Test
    void convertHexToText_shouldReturnEmptyOnNullOrEmptyInput() {
        DictionaryService service = new DictionaryService(null) {
            @Override
            public List<DictionaryEntry> getAllEntries() {
                return List.of();
            }
        };

        assertEquals("", service.convertHexToText(null));
        assertEquals("", service.convertHexToText(""));
        assertEquals(".", service.convertHexToText("   ")); // token unique vide → traité comme invalide
    }

    @Test
    void dictionaryEntry_shouldStoreValuesCorrectly() {
        DictionaryService.DictionaryEntry entry = new DictionaryService.DictionaryEntry("4A", "Commande A");
        assertEquals("4A", entry.hexPattern);
        assertEquals("Commande A", entry.traduction);
    }
}
