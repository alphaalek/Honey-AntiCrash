package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
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
        return getContainer().getDoubles().read(0);
    }

    public double getY() {
        if (location != null) {
            return location.getY();
        }
        return getContainer().getDoubles().read(1);
    }

    public double getZ() {
        if (location != null) {
            return location.getZ();
        }
        return getContainer().getDoubles().read(2);
    }

    public float getYaw() {
        if (location != null) {
            return location.getYaw();
        }
        return getContainer().getFloats().read(0);
    }

    public float getPitch() {
        if (location != null) {
            return location.getPitch();
        }
        return getContainer().getFloats().read(1);
    }

    public void setX(double x) {
        getContainer().getDoubles().write(0, x);
    }

    public void setY(double y) {
        getContainer().getDoubles().write(1, y);
    }

    public void setZ(double z) {
        getContainer().getDoubles().write(2, z);
    }

    public void setYaw(float yaw) {
        getContainer().getFloats().write(0, yaw);
    }

    public void setPitch(float pitch) {
        getContainer().getFloats().write(1, pitch);
    }

    public boolean onGround() {
        return getContainer().getBooleans().read(0);
    }
}
