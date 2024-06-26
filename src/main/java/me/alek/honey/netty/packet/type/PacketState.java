package me.alek.honey.netty.packet.type;

import me.alek.honey.util.Array;

public enum PacketState {
    HANDSHAKE,

    PLAY,

    STATUS,

    LOGIN,

    CONFIGURATION,

    ANY,

    UNKNOWN;

    @SuppressWarnings("unchecked")
    private final Array<PacketType>[] types = (Array<PacketType>[]) new Array[]{
            new Array<>(), new Array<>()
    };

    public Array<PacketType>[] getTypes() {
        return types;
    }
}
