package me.alek.honey.netty.injector;

public class MissingHandlerException extends Exception {

    public MissingHandlerException() {

    }

    public MissingHandlerException(String message) {
        super(message);
    }
}
