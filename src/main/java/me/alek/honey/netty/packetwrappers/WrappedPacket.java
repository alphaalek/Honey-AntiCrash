package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.World;

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

    HoneyPlayer getPlayer();

    PacketContainer<WP> getContainer();

    PacketType getType();

    Map<String, Object> getProperties();
}


