package com.aimp.processor;

final class AimpLogFormatter {
    private AimpLogFormatter() {
    }

    static String field(String name, Object value) {
        return name + "=" + value;
    }

    static String format(String event, String... fields) {
        StringBuilder formatted = new StringBuilder("[AIMP][").append(event).append(']');
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            formatted.append(System.lineSeparator())
                .append("  ")
                .append(field);
        }
        return formatted.toString();
    }
}
