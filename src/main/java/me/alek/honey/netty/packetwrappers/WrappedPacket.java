package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import org.bukkit.World;
import org.bukkit.entity.Player;

public abstract class WrappedPacket<WP extends WrappedPacket<WP>> extends WrappedObject<WrappedPacket<WP>> {

    private final PacketContainer<WP> packetContainer;

    public WrappedPacket(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket);
        this.packetContainer = packetContainer;
    }

    public Player getPlayer() {
        return packetContainer.getPlayer();
    }

    public World getWorld() {
        return getPlayer().getWorld();
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


