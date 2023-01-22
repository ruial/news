package com.briefbytes.news.model;

public enum ContentFormat {
    TEXT,
    HTML;

    public static ContentFormat fromInteger(int x) {
        return switch(x) {
            case 0 -> TEXT;
            case 1 -> HTML;
            default -> null;
        };
    }
}
