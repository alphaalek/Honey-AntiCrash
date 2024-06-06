package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.util.NetProtocol;

public interface EmulatorCodec<T, B extends ByteBuf> {

    boolean LEGACY = NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_17);

    T decodeModern(ByteBuf buf) throws CodecException;
    T decodeLegacy(ByteBuf buf, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) throws CodecException;

    B encodeModern(T val, PacketBuilder<PacketTypeEnum, B> function) throws CodecException;
    B encodeLegacy(T val, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) throws CodecException;

    @FunctionalInterface
    interface PacketBuilder<K1, K2> {
        void accept(K1 k1, K2 k2) throws CodecException;
    }

    interface In<T, B extends ByteBuf> extends EmulatorCodec<T, B> {

        default B encodeModern(T val, PacketBuilder<PacketTypeEnum, B> function) {
            throw new UnsupportedOperationException();
        }

        default B encodeLegacy(T val, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) {
            throw new UnsupportedOperationException();
        }
    }

    interface Out<T, B extends ByteBuf> extends EmulatorCodec<T, B> {

        default T decodeModern(ByteBuf buf) {
            throw new UnsupportedOperationException();
        }

        default T decodeLegacy(ByteBuf buf, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) {
            throw new UnsupportedOperationException();
        }
    }
}
