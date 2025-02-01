package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;

public interface EmulatorCodec<T, B extends ByteBuf> {

    boolean LEGACY = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17);

    T decodeModern(ByteBuf buf, PacketState state) throws CodecException;

    T decodeLegacy(ByteBuf buf, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException;

    B encodeModern(B buf, T val, PacketState state) throws CodecException;

    B encodeLegacy(T val, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException;

    @FunctionalInterface
    interface PacketCodecInitializer<K1, K2> {
        void accept(K1 k1, K2 k2) throws CodecException;
    }

    interface In<T, B extends ByteBuf> extends EmulatorCodec<T, B> {

        default B encodeModern(B buf, T val, PacketState state) {
            throw new UnsupportedOperationException();
        }

        default B encodeLegacy(T val, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) {
            throw new UnsupportedOperationException();
        }
    }

    interface Out<T, B extends ByteBuf> extends EmulatorCodec<T, B> {

        default T decodeModern(ByteBuf buf, PacketState state) {
            throw new UnsupportedOperationException();
        }

        default T decodeLegacy(ByteBuf buf, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) {
            throw new UnsupportedOperationException();
        }
    }
}
