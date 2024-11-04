package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.util.protocol.NetProtocol;

public interface Type<T> {

    T read(PacketDataSerializer buf, Object... params);

    PacketDataSerializer write(PacketDataSerializer buf, T val, Object... params);

    interface In<T> extends Type<T> {

        default PacketDataSerializer write(PacketDataSerializer buf, T val, Object... params) {
            throw new UnsupportedOperationException();
        }
    }

    interface Out<T> extends Type<T> {

        default T read(PacketDataSerializer buf, Object... params) {
            throw new UnsupportedOperationException();
        }
    }

    interface ProtocolType<T, TYPE extends Type<T>> {

        TYPE forProtocol(NetProtocol protocol);
    }
}
