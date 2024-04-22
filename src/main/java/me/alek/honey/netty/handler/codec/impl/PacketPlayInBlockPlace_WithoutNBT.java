package me.alek.honey.netty.handler.codec.impl;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.handler.codec.Codec;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import org.bukkit.Bukkit;

public class PacketPlayInBlockPlace_WithoutNBT implements Codec {

    @Override
    public void apply(IStructureModifier<Object> structure, WrappedPacketDataSerializer serializer) {
        structure.writeField(0, serializer.readBlockPosition());
        structure.writeField(1, serializer.readUnsignedByte());
        structure.writeField(2, serializer.readItemStackWithoutNBT());
        structure.writeField(3, serializer.readUnsignedByte() / 16.0F);
        structure.writeField(4, serializer.readUnsignedByte() / 16.0F);
        structure.writeField(5, serializer.readUnsignedByte() / 16.0F);
        structure.writeField(6, System.currentTimeMillis());
    }
}
