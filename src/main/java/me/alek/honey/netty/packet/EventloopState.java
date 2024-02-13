package me.alek.honey.netty.packet;

import java.util.Arrays;

public enum EventloopState {


    NO_STATE(null),

    HONEY_DECODE("hd"),

    SERVER_DECODE("sd"),

    HONEY_HANDLE("hh"),

    SERVER_HANDLE("sh");

    private final String format;

    EventloopState(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public static EventloopState[] VALID_STATES = Arrays.asList(HONEY_DECODE, SERVER_DECODE, HONEY_HANDLE, SERVER_HANDLE)
            .toArray(new EventloopState[0]);
}
