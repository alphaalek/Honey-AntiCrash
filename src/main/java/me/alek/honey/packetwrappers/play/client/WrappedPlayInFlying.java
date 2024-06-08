package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Location;

public abstract class WrappedPlayInFlying<WP extends WrappedPlayInFlying<WP>> extends WrappedPacket<WP> {

    public static class Status extends WrappedPlayInFlying<Status> {

        public Status(Object rawPacket, PacketContainer<Status> packetContainer) {
            super(rawPacket, packetContainer);
        }
    }

    private Location location;

    public WrappedPlayInFlying(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public Location getLocation() {
        return location != null ? location : new Location(
                getWorld(),
                getContainer().getDoubles().readField(0),
                getContainer().getDoubles().readField(1),
                getContainer().getDoubles().readField(2),
                getContainer().getFloats().readField(1),
                getContainer().getFloats().readField(0)
        );
    }

    public double getX() {
        return getLocation().getX();
    }

    public double getY() {
        return getLocation().getY();
    }

    public double getZ() {
        return getLocation().getZ();
    }

    public float getYaw() {
        return getLocation().getYaw();
    }

    public float getPitch() {
        return getLocation().getPitch();
    }

    public void setX(double x) {
        getContainer().getDoubles().writeField(0, x);
    }

    public void setY(double y) {
        getContainer().getDoubles().writeField(1, y);
    }

    public void setZ(double z) {
        getContainer().getDoubles().writeField(2, z);
    }

    public void setYaw(float yaw) {
        getContainer().getFloats().writeField(0, yaw);
    }

    public void setPitch(float pitch) {
        getContainer().getFloats().writeField(1, pitch);
    }

    public boolean onGround() {
        return getContainer().getBooleans().readField(0);
    }
}
