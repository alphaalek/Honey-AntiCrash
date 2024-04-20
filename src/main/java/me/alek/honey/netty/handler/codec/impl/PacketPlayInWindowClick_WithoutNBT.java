package me.alek.honey.netty.handler.codec.impl;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.handler.codec.Codec;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import org.bukkit.Bukkit;

public class PacketPlayInWindowClick_WithoutNBT implements Codec  {

    @Override
    public void apply(IStructureModifier<Object> structure, WrappedPacketDataSerializer serializer) {
        structure.writeField(0, serializer.readByte());
        structure.writeField(1, serializer.readShort());
        structure.writeField(2, serializer.readByte());
        structure.writeField(3, serializer.readShort());
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            structure.writeField(5, serializer.readByte());
        } else {
            structure.writeField(5, NMS.getInventoryClickTypeClass().getEnumConstants()[serializer.readVarInt()]);
        }
        structure.writeField(4, serializer.readItemStackWithoutNBT());
    }
}
