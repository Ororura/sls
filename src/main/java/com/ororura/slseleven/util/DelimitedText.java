package com.ororura.slseleven.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DelimitedText {

    private DelimitedText() {}

    public static List<List<String>> parse(String text, List<String> errors) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            errors.add("Текст для импорта пуст.");
            return List.of();
        }

        String[] lines = normalized.split("\\r?\\n");
        char delimiter = detectDelimiter(lines);
        if (delimiter == 0) {
            errors.add("Не удалось определить разделитель (таб, ';' или ',').");
            return List.of();
        }

        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            rows.add(parseLine(line, delimiter));
        }
        return rows;
    }

    private static char detectDelimiter(String[] lines) {
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (line.indexOf('\t') >= 0) {
                return '\t';
            }
            if (line.indexOf(';') >= 0) {
                return ';';
            }
            if (line.indexOf(',') >= 0) {
                return ',';
            }
        }
        return 0;
    }

    private static List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (
                    inQuotes &&
                    i + 1 < line.length() &&
                    line.charAt(i + 1) == '"'
                ) {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    public static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.replace("\uFEFF", "").trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[\\s_-]+", "");
    }
}
