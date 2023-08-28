package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;

public enum PacketBound {
    SERVER,

    CLIENT;

    public Enum<?> getNMSWrapperType() {
        return Reflection.getEnumAtIndex(NMSUtils.getEnumProtocolDirectionClass(), ordinal());
    }
}
