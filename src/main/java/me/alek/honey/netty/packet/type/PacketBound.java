package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.handler.MinecraftHandlers;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.Reflection;

public enum PacketBound {
    SERVER,

    CLIENT;

    private final Enum<?> nmsEnum = Reflection.getEnumAtIndex(NMS.getEnumProtocolDirectionClass(), ordinal());

    public String getProtocolHandlerName() {
        return this == SERVER ? MinecraftHandlers.DECODER : MinecraftHandlers.ENCODER;
    }

    public Enum<?> getNMSType() {
        return nmsEnum;
    }
}
