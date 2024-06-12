package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.util.NetProtocol;

public interface EmulatorCodec<T, B extends ByteBuf> {

    boolean LEGACY = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17);

    T decodeModern(ByteBuf buf, PacketState state) throws CodecException;
    T decodeLegacy(ByteBuf buf, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) throws CodecException;

    B encodeModern(T val, PacketState state, PacketBuilder<PacketType, B> function) throws CodecException;
    B encodeLegacy(T val, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) throws CodecException;

    @FunctionalInterface
    interface PacketBuilder<K1, K2> {
        void accept(K1 k1, K2 k2) throws CodecException;
    }

    interface In<T, B extends ByteBuf> extends EmulatorCodec<T, B> {

        default B encodeModern(T val, PacketState state, PacketBuilder<PacketType, B> function) {
            throw new UnsupportedOperationException();
        }

        default B encodeLegacy(T val, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) {
            throw new UnsupportedOperationException();
        }
    }

    interface Out<T, B extends ByteBuf> extends EmulatorCodec<T, B> {

        default T decodeModern(ByteBuf buf, PacketState state) {
            throw new UnsupportedOperationException();
        }

        default T decodeLegacy(ByteBuf buf, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) {
            throw new UnsupportedOperationException();
        }
    }
}
