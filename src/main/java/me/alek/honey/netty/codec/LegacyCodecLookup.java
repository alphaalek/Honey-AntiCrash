package me.alek.honey.netty.codec;

import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.util.Array;

import java.lang.invoke.MethodHandle;
import java.util.EnumMap;

public class LegacyCodecLookup {

    private static final EnumMap<PacketState, Array<MethodHandle>> legacySerializerApplicationLookup = new EnumMap<>(PacketState.class);

    static {
        for (PacketState state : PacketState.values()) {
            legacySerializerApplicationLookup.put(state, new Array<>());
        }
    }

    public static EnumMap<PacketState, Array<MethodHandle>> get() {
        return legacySerializerApplicationLookup;
    }
}
