package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CodecException;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;

public class NoOpCodec implements EmulatorCodec<Object, PacketDataSerializer> {

    @Override
    public Object decodeModern(ByteBuf buf, PacketState state) throws CodecException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object decodeLegacy(ByteBuf buf, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PacketDataSerializer encodeModern(PacketDataSerializer buf, Object val, PacketState state) throws CodecException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PacketDataSerializer encodeLegacy(Object val, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException {
        throw new UnsupportedOperationException();
    }
}
