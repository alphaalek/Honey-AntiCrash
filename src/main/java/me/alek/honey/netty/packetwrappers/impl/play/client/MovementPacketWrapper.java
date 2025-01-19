package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import org.bukkit.Location;

public abstract class MovementPacketWrapper<WP extends MovementPacketWrapper<WP>> extends ObjectWrappedPacket<WP> {

    public static class Status extends MovementPacketWrapper<Status> {

        public Status(Object rawPacket, PacketContainer<Status> packetContainer) {
            super(rawPacket, packetContainer);
        }
    }

    private Location location;

    public MovementPacketWrapper(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public Location getLocation() {
        return location != null ? location : (location = new Location(
                getPlayer().getWorld(),
                getContainer().getStructure().getDoubles().readField(0),
                getContainer().getStructure().getDoubles().readField(1),
                getContainer().getStructure().getDoubles().readField(2),
                getContainer().getStructure().getFloats().readField(0),
                getContainer().getStructure().getFloats().readField(1)
        ));
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
        getContainer().getStructure().getDoubles().writeField(0, x);
    }

    public void setY(double y) {
        getContainer().getStructure().getDoubles().writeField(1, y);
    }

    public void setZ(double z) {
        getContainer().getStructure().getDoubles().writeField(2, z);
    }

    public void setYaw(float yaw) {
        getContainer().getStructure().getFloats().writeField(0, yaw);
    }

    public void setPitch(float pitch) {
        getContainer().getStructure().getFloats().writeField(1, pitch);
    }

    public boolean onGround() {
        return getContainer().getStructure().getBooleans().readField(0);
    }
}
