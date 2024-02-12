package me.alek.honey.netty.packet;

public enum EventloopState {

    NO_STATE,

    HONEY_DECODE,

    SERVER_DECODE,

    HONEY_HANDLE,

    QUEUE_TRACKING,

    SERVER_HANDLE
}
