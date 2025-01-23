package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.util.protocol.NetProtocol;

public interface Type<T, C extends Type.TypeContext> {

    T read(PacketDataSerializer buf, C context);

    PacketDataSerializer write(PacketDataSerializer buf, T val, C context);

    interface In<T, C extends Type.TypeContext> extends Type<T, C> {

        default PacketDataSerializer write(PacketDataSerializer buf, T val, C context) {
            throw new UnsupportedOperationException();
        }
    }

    interface Out<T, C extends Type.TypeContext> extends Type<T, C> {

        default T read(PacketDataSerializer buf, C context) {
            throw new UnsupportedOperationException();
        }
    }

    interface ProtocolType<T, C extends TypeContext, TYPE extends Type<T, C>> {

        TYPE forProtocol(NetProtocol protocol);
    }

    interface NoContext<T> extends Type<T, TypeContext.Empty> {

        @Override
        default T read(PacketDataSerializer buf, TypeContext.Empty context) {
            return read(buf);
        }

        T read(PacketDataSerializer buf);

        @Override
        default PacketDataSerializer write(PacketDataSerializer buf, T value, TypeContext.Empty context) {
            return write(buf, value);
        }

        PacketDataSerializer write(PacketDataSerializer buf, T value);

        interface In<T> extends NoContext<T> {

            default PacketDataSerializer write(PacketDataSerializer buf, T val) {
                throw new UnsupportedOperationException();
            }
        }

        interface Out<T> extends NoContext<T> {

            default T read(PacketDataSerializer buf) {
                throw new UnsupportedOperationException();
            }
        }
    }

    interface TypeContext {

        interface Empty extends TypeContext {

        }

        interface OneArg<T> extends TypeContext {

            static <T> OneArg<T> of(T arg) {
                return () -> arg;
            }

            T get();
        }
    }
}
