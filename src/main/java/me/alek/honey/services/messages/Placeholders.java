package me.alek.honey.services.messages;

public enum Placeholders {

    PREFIX("prefix"),
    NETWORK("network"),
    CHECK("check"),
    CONTEXT("context"),
    PLAYER("player");

    private final String placeholder;

    Placeholders(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getText() {
        return placeholder;
    }
}
