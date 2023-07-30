package me.alek.honey.netty.modelwrappers.network;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.utility.reflect.NMSUtils;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {


    public WrappedEnumProtocol(Object handle) {
        super(handle);
    }

    public Object createPacket(WrappedEnumProtocolDirection direction, int id) {
        try {
            return getReflectStructure().withType(NMSUtils.getPacketClass())
                    .withTarget(getHandle()).invokeMethodWithException(0, direction.getHandle(), id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}