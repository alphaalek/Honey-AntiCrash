package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;
import org.bukkit.Location;

public class WrappedPlayInFlying<WP extends WrappedPlayInFlying<WP>> extends WrappedPacket<WP> {

    private Location location;

    public WrappedPlayInFlying(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public Location getLocation() {
        if (location == null) {
            return location = new Location(getWorld(), getX(), getY(), getZ(), getYaw(), getPitch());
        }
        return location;
    }

    public double getX() {
        if (location != null) {
            return location.getX();
        }
        return getPacket().getDoubles().read(0);
    }

    public double getY() {
        if (location != null) {
            return location.getY();
        }
        return getPacket().getDoubles().read(1);
    }

    public double getZ() {
        if (location != null) {
            return location.getZ();
        }
        return getPacket().getDoubles().read(2);
    }

    public float getYaw() {
        if (location != null) {
            return location.getYaw();
        }
        return getPacket().getFloats().read(0);
    }

    public float getPitch() {
        if (location != null) {
            return location.getPitch();
        }
        return getPacket().getFloats().read(1);
    }

    public boolean onGround() {
        return getPacket().getBooleans().read(0);
    }
}
