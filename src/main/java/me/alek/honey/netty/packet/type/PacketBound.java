package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.Reflection;

public enum PacketBound {
    SERVER,

    CLIENT;

    public String getProtocolHandlerName() {
        return this == SERVER ? "decoder" : "encoder";
    }

    public Enum<?> getNMSWrapperType() {
        return Reflection.getEnumAtIndex(NMS.getEnumProtocolDirectionClass(), ordinal());
    }
}
