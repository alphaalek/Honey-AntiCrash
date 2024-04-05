package me.alek.honey.util.pastebin.enums;

public enum Format {
    PYTHON("python", "Python");

    final String code;
    final String name;

    Format(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }
}