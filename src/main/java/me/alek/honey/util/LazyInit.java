package me.alek.honey.util;

import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.FieldAccessor;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Supplier;

public class LazyInit<T> {

    private T val;
    private final Supplier<T> function;

    public LazyInit(Supplier<T> function) {
        this.function = function;
    }

    public T get() {
        return val == null ? (val = function.get()) : val;
    }

    public static LazyInit<Integer> getMinecraftIDInit(PacketTypeEnum type) {
        return new LazyInit<>(() -> {
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                Collection<FieldAccessor<Object>> accessors = CodecFactory.getPacketTypes1_20_5(type.getState());

                int clientBoundIndex = 0;
                int serverBoundIndex = 0;

                for (FieldAccessor<Object> accessor : accessors) {
                    Type genericType = accessor.getField().getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) genericType;
                        Type[] args = parameterizedType.getActualTypeArguments();
                        if (args.length == 0) {
                            continue;
                        }

                        String packet = args[0].getTypeName();
                        if (packet.equals(type.getNmsClass().getName())) {
                            return type.getBound() == PacketBound.SERVER ? serverBoundIndex : clientBoundIndex;
                        }

                        if (PacketWrapperInvokerFactory.getPacketTypeForName(packet).getBound() == PacketBound.SERVER) {
                            serverBoundIndex++;
                        } else {
                            clientBoundIndex++;
                        }
                    }
                }
            } else {
                WrappedEnumProtocol enumProtocol = WrappedEnumProtocol.getEnumProtocolDelegate(type.getBound(), type.getState());

                return enumProtocol.getPacketId(type.getNmsClass());
            }
            return null;
        });
    }
}
