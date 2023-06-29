package me.alek.honey.netty.modelwrappers.network;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.reflect.NMSUtils;

import javax.annotation.Nullable;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {


    public WrappedEnumProtocol(@Nullable Object handle) {
        super(handle);
    }

    public WrappedPacket<?> createPacket(WrappedEnumProtocolDirection direction, int id) {
        try {
            return getReflectStructure().withType(NMSUtils.getPacketClass(), Converters.getPacketConverter())
                    .withTarget(getHandle()).invokeMethodWithException(0, direction.getHandle(), id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}