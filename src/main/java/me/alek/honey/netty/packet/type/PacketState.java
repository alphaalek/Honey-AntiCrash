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
            new Array<>(), new Array<>() // will be written to on PacketTypes initialization
    };

    public String getName() {
        return String.valueOf(name().charAt(0)).toUpperCase() + name().substring(1).toLowerCase();
    }

    public Array<PacketType>[] getTypes() {
        return types;
    }

    public static boolean isValid(PacketState state) {
        return state != UNKNOWN && state != ANY;
    }
}
