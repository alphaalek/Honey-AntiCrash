package me.alek.honey.messages;

public enum Placeholders {

    // for honey notifications
    PREFIX("prefix"),
    NETWORK("network"),
    CHECK("check"),
    CONTEXT("context"),
    PLAYER("player"),

    // for inbound packet filter
    AMOUNT("amount"),
    CURRENT_AMOUNT("current_amount"),

    // for quarantined async worker
    TIME("time"),
    ;

    private final String placeholder;

    Placeholders(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getText() {
        return placeholder;
    }
}
