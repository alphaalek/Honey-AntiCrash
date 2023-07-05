package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.play.client.common.WrappedAbstractPositional;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class WrappedPlayInFlying<WP extends WrappedPlayInFlying<WP>> extends WrappedAbstractPositional<WP> {

    public WrappedPlayInFlying(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket, packetContainer);
    }

    @Override
    public Location getLocation(PacketContainer<WP> packetContainer) {
        return new Location(
                getWorld(),
                getContainer().getDoubles().readField(0),
                getContainer().getDoubles().readField(1),
                getContainer().getDoubles().readField(2),
                getContainer().getFloats().readField(0),
                getContainer().getFloats().readField(1)
        );
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
