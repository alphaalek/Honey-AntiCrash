package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;

public class WrappedPlayInPickItem extends ByteInWrappedPacket<WrappedPlayInPickItem> {

    @Property
    private final int hotBarSlot;

    public WrappedPlayInPickItem(PacketDataSerializer buf, PacketContainer<WrappedPlayInPickItem> wrappedPlayInPickItemPacketContainer) {
        super(buf, wrappedPlayInPickItemPacketContainer);
        this.hotBarSlot = buf.readVarInt();
    }

    public int getHotBarSlot() {
        return this.hotBarSlot;
    }
}