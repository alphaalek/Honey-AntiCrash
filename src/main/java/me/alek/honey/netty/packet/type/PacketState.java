package me.alek.honey.netty.packet.type;

import me.alek.honey.util.Array;
import org.apache.commons.lang.WordUtils;

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
        return WordUtils.capitalize(name().toLowerCase());
    }

    public Array<PacketType>[] getTypes() {
        return types;
    }
}
