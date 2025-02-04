package me.alek.honey.netty.codec.types;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.util.protocol.NetProtocol;

public interface TypeSerializer<T, C extends TypeSerializer.TypeContext> {

    T read(PacketDataSerializer buf, C context) throws DecoderException;

    PacketDataSerializer write(PacketDataSerializer buf, T val, C context) throws EncoderException;

    interface In<T, C extends TypeSerializer.TypeContext> extends TypeSerializer<T, C> {

        default PacketDataSerializer write(PacketDataSerializer buf, T val, C context) {
            throw new UnsupportedOperationException();
        }
    }

    interface Out<T, C extends TypeSerializer.TypeContext> extends TypeSerializer<T, C> {

        default T read(PacketDataSerializer buf, C context) {
            throw new UnsupportedOperationException();
        }
    }

    interface ProtocolType<T, C extends TypeContext, TYPE extends TypeSerializer<T, C>> {

        TYPE forProtocol(NetProtocol protocol);
    }

    interface NoContext<T> extends TypeSerializer<T, TypeContext.Empty> {

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

        interface Protocol extends OneArg<NetProtocol> {

            static Protocol of(NetProtocol protocol) {
                return () -> protocol;
            }
        }

        interface OneArg<T> extends TypeContext {

            static <T> OneArg<T> of(T arg) {
                return () -> arg;
            }

            T get();
        }

        interface TwoArgs<T, K> extends TypeContext {

            static <T, K> TwoArgs<T, K> of(T first, K second) {
                return new TwoArgs<T, K>() {
                    @Override
                    public T getFirst() {
                        return first;
                    }

                    @Override
                    public K getSecond() {
                        return second;
                    }
                };
            }

            T getFirst();

            K getSecond();
        }
    }
}
