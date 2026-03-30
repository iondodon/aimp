package com.aimp.core;

public final class JavaSourceWriter {
    private static final String INDENT = "    ";

    private final StringBuilder builder = new StringBuilder();
    private int indentLevel;

    public void line(String value) {
        if (!value.isEmpty()) {
            builder.append(INDENT.repeat(indentLevel));
            builder.append(value);
        }
        builder.append('\n');
    }

    public void blankLine() {
        if (builder.isEmpty() || builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
            return;
        }
        if (builder.length() < 2 || builder.charAt(builder.length() - 2) != '\n') {
            builder.append('\n');
        }
    }

    public void openBlock(String header) {
        line(header + " {");
        indentLevel++;
    }

    public void closeBlock() {
        indentLevel--;
        line("}");
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
