package me.alek.honey.netty.codec;

import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.util.Array;

import java.lang.invoke.MethodHandle;
import java.util.EnumMap;
import java.util.function.Supplier;

public class LegacyCodecLookupProvider implements Supplier<EnumMap<PacketState, Array<MethodHandle>>> {

    private final EnumMap<PacketState, Array<MethodHandle>> legacySerializerLookup = new EnumMap<>(PacketState.class);

    public LegacyCodecLookupProvider() {
        for (PacketState state : PacketState.values()) {
            legacySerializerLookup.put(state, new Array<>());
        }
    }

    @Override
    public EnumMap<PacketState, Array<MethodHandle>> get() {
        return legacySerializerLookup;
    }
}
