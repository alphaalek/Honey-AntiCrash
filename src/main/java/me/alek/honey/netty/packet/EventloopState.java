package me.alek.honey.netty.packet;

public enum EventloopState {

    NO_STATE,

    DECODE,

    IN_BETWEEN,

    HONEY_HANDLE,

    SERVER_HANDLE
}
