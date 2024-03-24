package me.alek.honey.utility.pastebin.enums;

public enum Visibility {
    PUBLIC(0, "Public");

    private final int code;
    private final String name;

    private Visibility(int code, String name) {
        this.code = code;
        this.name = name;
    }

    static Visibility find(int code) {
        Visibility[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Visibility value = var1[var3];
            if (value.getCode() == code) {
                return value;
            }
        }

        return null;
    }

    public int getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }
}