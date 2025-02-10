package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;

public class ByteInWrappedPacketImpl<WP extends SourcedWrappedPacket<WP>> implements ByteInWrappedPacket<WP> {

    private final PacketContainer<WP> packet;
    private final PacketContainer.PipelineState state;
    private final PacketDataSerializer buf;
    private final int byteSize;
    private final int accumulatedByteSize;
    private final int doubleByteSequences;

    public ByteInWrappedPacketImpl(PacketDataSerializer buf, PacketContainer<WP> packet) {
        this.buf = buf;
        this.packet = packet;
        this.state = packet.getPipelineState();
        this.byteSize = buf.readableBytes();

        if (packet.getAnyByteWrapped() == null) {
            int start = buf.readerIndex(), accumulated = 0, prevByte = 0, doubleSequences = 0;
            while (buf.isReadable()) {
                byte b = buf.readByte();
                accumulated += Math.abs(b) + 8;
                if (b == prevByte) {
                    doubleSequences++;
                }
                prevByte = b;
            }
            buf.readerIndex(start);
            this.accumulatedByteSize = accumulated;
            this.doubleByteSequences = doubleSequences;
        } else {
            this.accumulatedByteSize = packet.getAnyByteWrapped().getAccumulatedByteSize();
            this.doubleByteSequences = packet.getAnyByteWrapped().getDoubleByteSequences();
        }
    }

    @Override
    public boolean isAbstract() {
        return !getClass().equals(ByteInWrappedPacketImpl.class);
    }

    @Override
    public PacketDataSerializer getBuf() {
        return this.buf;
    }

    @Override
    public int getByteSize() {
        return this.byteSize;
    }

    @Override
    public int getAccumulatedByteSize() {
        return this.accumulatedByteSize;
    }

    @Override
    public int getDoubleByteSequences() {
        return this.doubleByteSequences;
    }

    @Override
    public HoneyPlayer getPlayer() {
        return this.packet.getPlayer();
    }

    @Override
    public PacketContainer.PipelineState getState() {
        return this.state;
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
