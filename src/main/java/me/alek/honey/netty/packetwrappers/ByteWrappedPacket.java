package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;

public class ByteWrappedPacket<WP extends WrappedPacket<WP>> implements WrappedPacket<WP> {

    private final PacketContainer<WP> packet;
    private final PacketDataSerializer buf;
    private final int byteSize;

    public ByteWrappedPacket(PacketDataSerializer buf, PacketContainer<WP> packet) {
        this.buf = buf;
        this.packet = packet;
        this.byteSize = buf.readableBytes();
    }

    public PacketDataSerializer getBuf() {
        return this.buf;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    @Override
    public HoneyPlayer getPlayer() {
        return this.packet.getPlayer();
    }

    @Override
    public PacketContainer<WP> getContainer() {
        return this.packet;
    }

    @Override
    public PacketType getType() {
        return this.packet.getType();
    }
}
