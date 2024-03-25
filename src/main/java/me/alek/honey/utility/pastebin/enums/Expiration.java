package me.alek.honey.utility.pastebin.enums;

public enum Expiration {
    ONE_DAY("1D", "1 Day");

    private final String code;
    private final String description;

    Expiration(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}