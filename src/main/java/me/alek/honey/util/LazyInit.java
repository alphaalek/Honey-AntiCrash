package me.alek.honey.util;

import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.util.NetProtocol;

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
            } else {
                WrappedEnumProtocol enumProtocol = WrappedEnumProtocol.getEnumProtocolDelegate(type.getBound(), type.getState());
                if (enumProtocol == null) {
                    throw new UnsupportedOperationException();
                }

                return enumProtocol.getPacketId(type.getNmsClass());
            }
            return null;
        });
    }
}
