package me.alek.honey.netty.codec;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.NMS;

public class DisabledNBTCodec {

    public static LegacyCodec BLOCK_PLACE = new LegacyCodec() {

        @Override
        public void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) {
            structure.writeField(0, serializer.readBlockPosition().getHandle());
            structure.writeField(1, serializer.readUnsignedByte());
            structure.writeField(2, serializer.readItemStackWithFilter(false));
            structure.writeField(3, serializer.readUnsignedByte() / 16.0F);
            structure.writeField(4, serializer.readUnsignedByte() / 16.0F);
            structure.writeField(5, serializer.readUnsignedByte() / 16.0F);
            structure.writeField(6, System.currentTimeMillis());
        }
    };

    public static LegacyCodec WINDOW_CLICK = new LegacyCodec() {

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
            structure.writeField(4, serializer.readItemStackWithFilter(false));
        }
    };
}
