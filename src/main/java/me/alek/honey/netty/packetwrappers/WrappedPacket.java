package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import org.bukkit.Bukkit;
import org.bukkit.World;

public abstract class WrappedPacket<WP extends WrappedPacket<WP>> extends WrappedObject<WrappedPacket<WP>> {

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

    public PacketState getState() {
        return packetContainer.getState();
    }

    public PacketTypeEnum getType() {
        return packetContainer.getType();
    }

}


