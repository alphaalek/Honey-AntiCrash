package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.Reflection;

public enum PacketBound {
    SERVER,

    CLIENT;

    public Enum<?> getNMSWrapperType() {
        return Reflection.getEnumAtIndex(NMSUtils.getEnumProtocolDirectionClass(), ordinal());
    }
}
