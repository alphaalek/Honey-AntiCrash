package me.alek.honey.netty.packetwrappers.play.client.common;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Location;

public abstract class WrappedAbstractPositional<WP extends WrappedPacket<WP>> extends WrappedPacket<WP> {

    private final Location location;

    public WrappedAbstractPositional(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket, packetContainer);
        location = getLocation(packetContainer);
    }

    public abstract Location getLocation(PacketContainer<WP> packetContainer);

    public Location getLocation() {
        return location;
    }

    public double getX() {
        return location.getX();
    }

    public double getY() {
        return location.getY();
    }

    public double getZ() {
        return location.getZ();
    }

    public float getYaw() {
        return location.getYaw();
    }

    public float getPitch() {
        return location.getPitch();
    }
}
