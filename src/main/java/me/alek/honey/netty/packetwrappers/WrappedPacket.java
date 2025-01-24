package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.Map;

public interface WrappedPacket<WP extends WrappedPacket<WP>> {

    @SuppressWarnings("unchecked")
    default WP get() {
        return (WP) this;
    }

    default boolean isByteBased() {
        return false;
    }

    default boolean isObjectBased() {
        return false;
    }

    NetProtocol getProtocol();

    PacketType getType();

    Map<String, Object> getProperties();
}


