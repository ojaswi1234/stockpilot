package com.stockpilot.service;

import org.springframework.stereotype.Component;

@Component
public class CsvSanitizer {

    /**
     * Sanitizes input against CSV / Formula Injection attacks (CWE-1236).
     * If a cell value starts with '=', '+', '-', '@', '\t', or '\r',
     * spreadsheet applications (Excel, Calc) may execute it as a formula or shell command.
     * We neutralize this by prefixing with a single quote (').
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        char firstChar = input.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
            return "'" + input.replace("\"", "\"\"");
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // Standard CSV quotes escaping
        if (trimmed.contains(",") || trimmed.contains("\"") || trimmed.contains("\n")) {
            return "\"" + trimmed.replace("\"", "\"\"") + "\"";
        }
        return trimmed;
    }
}
