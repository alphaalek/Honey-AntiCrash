package me.alek.honey.netty.codec;

import io.netty.handler.codec.DecoderException;

import java.util.function.Supplier;

public interface Rewritable<T> extends Supplier<T> {

    interface Serializer<T, C extends TypeSerializer.TypeContext> extends TypeSerializer<T, C> {

        @Override
        default T read(PacketDataSerializer buf, C context) throws DecoderException {
            return readRewritable(buf, context).get();
        }

        Rewritable<T> readRewritable(PacketDataSerializer buf, C context) throws DecoderException;
    }

    interface Result<T> extends Supplier<T> {

        static <T> Result<T> noRewritten() {
            return new Result<T>() {
                @Override
                public boolean isRewritten() {
                    return false;
                }

                @Override
                public T get() {
                    throw new UnsupportedOperationException("Can't get a non rewritten value");
                }
            };
        }

        static <T> Result<T> rewritten(T val) {
            return new Result<T>() {
                @Override
                public boolean isRewritten() {
                    return true;
                }

                @Override
                public T get() {
                    return val;
                }
            };
        }

        boolean isRewritten();

        default T orElse(T val) {
            if (isRewritten()) {
                return get();
            } else {
                return val;
            }
        }
    }

    static <T> Rewritable<T> noAction(T val) {
        return new Rewritable<T>() {
            @Override
            public Result<PacketDataSerializer> result() {
                return Result.noRewritten();
            }

            @Override
            public T get() {
                return val;
            }
        };
    }

    static <T> Rewritable<T> rewritten(T val, Result<PacketDataSerializer> rewritten) {
        return new Rewritable<T>() {
            @Override
            public Result<PacketDataSerializer> result() {
                return rewritten;
            }

            @Override
            public T get() {
                return val;
            }
        };
    }

    Result<PacketDataSerializer> result();
}
