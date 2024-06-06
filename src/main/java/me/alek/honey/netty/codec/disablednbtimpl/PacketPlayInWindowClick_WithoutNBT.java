package me.alek.honey.netty.codec.disablednbtimpl;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.codec.LegacyCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;

public class PacketPlayInWindowClick_WithoutNBT implements LegacyCodec {

    @Override
    public void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) {
        structure.writeField(0, serializer.readByte());
        structure.writeField(1, serializer.readShort());
        structure.writeField(2, serializer.readByte());
        structure.writeField(3, serializer.readShort());
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            structure.writeField(5, serializer.readByte());
        } else {
            structure.writeField(5, NMS.getInventoryClickTypeClass().getEnumConstants()[serializer.readVarInt()]);
        }
        structure.writeField(4, serializer.readItemStackWithoutMeta());
    }
}
