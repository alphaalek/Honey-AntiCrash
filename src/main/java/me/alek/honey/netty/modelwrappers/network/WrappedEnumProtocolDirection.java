package me.alek.honey.netty.modelwrappers.network;

import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;

public enum WrappedEnumProtocolDirection {

    SERVERBOUND,

    CLIENTBOUND;

    public Enum<?> getHandle() {
        return Reflection.getEnumAtIndex(NMSUtils.getEnumProtocolDirectionClass(), ordinal());
    }

}
