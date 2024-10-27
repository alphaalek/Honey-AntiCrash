package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.ObjectPacketStructure;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketType;

public class ObjectWrappedPacket<WP extends WrappedPacket<WP>> extends WrappedObject<WP> implements WrappedPacket<WP> {

    private final PacketContainer<WP> packet;

    public ObjectWrappedPacket(Object handle, PacketContainer<WP> packet) {
        super(handle);
        this.packet = packet;
    }
    @Override
    public HoneyPlayer getPlayer() {
        return packet.getPlayer();
    }

    @Override
    public PacketContainer<WP> getContainer() {
        return packet;
    }

    @Override
    public PacketType getType() {
        return packet.getType();
    }
}
