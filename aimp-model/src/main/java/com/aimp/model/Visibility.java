package com.aimp.model;

/**
 * Models Java visibility for generated members.
 */
public enum Visibility {
    /** Public visibility. */
    PUBLIC("public"),
    /** Protected visibility. */
    PROTECTED("protected"),
    /** Package-private visibility. */
    PACKAGE_PRIVATE("");

    private final String keyword;

    Visibility(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the Java visibility keyword, or an empty string for package-private visibility.
     *
     * @return the Java visibility keyword
     */
    public String keyword() {
        return keyword;
    }
}
