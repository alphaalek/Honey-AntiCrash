package me.alek.honey.utility.pastebin.enums;

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

    static Format find(String code) {
        Format[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Format value = var1[var3];
            if (value.getCode().equals(code)) {
                return value;
            }
        }

        return NONE;
    }
}