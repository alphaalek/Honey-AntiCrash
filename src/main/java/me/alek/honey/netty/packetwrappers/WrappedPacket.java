package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class WrappedPacket<WP extends WrappedPacket<WP>> extends WrappedObject<WrappedPacket<WP>> {

    private final PacketContainer<WP> packetContainer;

    public WrappedPacket(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket);
        this.packetContainer = packetContainer;
    }

    public HoneyPlayer getPlayer() {
        return packetContainer.getPlayer();
    }

    public World getWorld() {
        return getPlayer() != null ? getPlayer().getWorld() : Bukkit.getWorlds().get(0);
    }

    public PacketContainer<WP> getContainer() {
        return packetContainer;
    }

    public PacketType getType() {
        return packetContainer.getType();
    }

}


