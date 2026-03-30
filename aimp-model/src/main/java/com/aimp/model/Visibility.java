package com.aimp.model;

public enum Visibility {
    PUBLIC("public"),
    PROTECTED("protected"),
    PACKAGE_PRIVATE("");

    private final String keyword;

    Visibility(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
