package com.stockpilot;

import com.stockpilot.dto.InventorySummaryDto;
import com.stockpilot.service.CsvSanitizer;
import com.stockpilot.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CsvExportSanitizationTest {

    private final CsvSanitizer csvSanitizer = new CsvSanitizer();

    @ParameterizedTest
    @ValueSource(strings = {
            "=cmd|' /C calc'!A0",
            "+SUM(A1:A10)",
            "-2+3*cmd",
            "@SUM(B1:B5)",
            "\tHYPERLINK(\"http://evil.com\")",
            "\rcalc"
    })
    @DisplayName("CSV Sanitizer: Dangerous formula injection prefixes are neutralized with a leading single quote")
    void testDangerousFormulaInjectionNeutralization(String dangerousPayload) {
        String sanitized = csvSanitizer.sanitize(dangerousPayload);
        assertTrue(sanitized.startsWith("'"), "Sanitized string must start with a single quote (') to force literal interpretation in spreadsheets: " + sanitized);
    }

    @Test
    @DisplayName("CSV Sanitizer: Standard safe strings and commas are correctly escaped")
    void testStandardCsvEscaping() {
        assertEquals("Standard Product", csvSanitizer.sanitize("Standard Product"));
        assertEquals("\"Product, with comma\"", csvSanitizer.sanitize("Product, with comma"));
        assertEquals("\"Product with \"\"quotes\"\"\"", csvSanitizer.sanitize("Product with \"quotes\""));
        assertEquals("", csvSanitizer.sanitize(null));
        assertEquals("", csvSanitizer.sanitize("   "));
    }
}
