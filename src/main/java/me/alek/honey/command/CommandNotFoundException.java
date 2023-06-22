package me.alek.honey.command;

public class CommandNotFoundException extends Exception {

    private final String message;

    public CommandNotFoundException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
