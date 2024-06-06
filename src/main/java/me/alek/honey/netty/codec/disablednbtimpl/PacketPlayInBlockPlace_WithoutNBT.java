package me.alek.honey.netty.codec.disablednbtimpl;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.codec.LegacyCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;

public class PacketPlayInBlockPlace_WithoutNBT implements LegacyCodec {

    @Override
    public void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) {
        structure.writeField(0, serializer.readBlockPosition());
        structure.writeField(1, serializer.readUnsignedByte());
        structure.writeField(2, serializer.readItemStackWithoutMeta());
        structure.writeField(3, serializer.readUnsignedByte() / 16.0F);
        structure.writeField(4, serializer.readUnsignedByte() / 16.0F);
        structure.writeField(5, serializer.readUnsignedByte() / 16.0F);
        structure.writeField(6, System.currentTimeMillis());
    }
}
