package com.aimp.core;

/**
 * Small helper for writing indented Java source deterministically.
 */
public final class JavaSourceWriter {
    private static final String INDENT = "    ";

    private final StringBuilder builder = new StringBuilder();
    private int indentLevel;

    /**
     * Creates an empty Java source writer.
     */
    public JavaSourceWriter() {
    }

    /**
     * Writes a single source line at the current indentation level.
     *
     * @param value the line content without the trailing newline
     */
    public void line(String value) {
        if (!value.isEmpty()) {
            builder.append(INDENT.repeat(indentLevel));
            builder.append(value);
        }
        builder.append('\n');
    }

    /**
     * Writes a single blank line unless one is already present.
     */
    public void blankLine() {
        if (builder.isEmpty() || builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
            return;
        }
        if (builder.length() < 2 || builder.charAt(builder.length() - 2) != '\n') {
            builder.append('\n');
        }
    }

    /**
     * Opens a Java block and increases indentation for following lines.
     *
     * @param header the block header text without the opening brace
     */
    public void openBlock(String header) {
        line(header + " {");
        indentLevel++;
    }

    /**
     * Closes the current Java block and decreases indentation.
     */
    public void closeBlock() {
        indentLevel--;
        line("}");
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
