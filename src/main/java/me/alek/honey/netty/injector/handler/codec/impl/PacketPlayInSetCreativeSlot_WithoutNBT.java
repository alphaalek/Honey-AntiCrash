package me.alek.honey.netty.injector.handler.codec.impl;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.injector.handler.codec.Codec;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;

public class PacketPlayInSetCreativeSlot_WithoutNBT implements Codec {

    @Override
    public void apply(IStructureModifier<Object> structure, WrappedPacketDataSerializer serializer) {
        structure.writeField(0, serializer.readShort());
        structure.writeField(1, serializer.readItemStackWithoutNBT());
    }
}
