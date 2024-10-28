package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import org.bukkit.Bukkit;

import java.util.Map;

public class ByteWrappedPacket<WP extends WrappedPacket<WP>> implements WrappedPacket<WP> {

    private final PacketContainer<WP> packet;
    private final PacketDataSerializer buf;
    private final int byteSize;
    private final int accumulatedByteSize;
    private final int doubleByteSequences;

    public ByteWrappedPacket(PacketDataSerializer buf, PacketContainer<WP> packet) {
        this.buf = buf;
        this.packet = packet;
        this.byteSize = buf.readableBytes();

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
    }

    @Override
    public boolean isByteBased() {
        return true;
    }

    public PacketDataSerializer getBuf() {
        return this.buf;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    public int getAccumulatedByteSize() {
        return this.accumulatedByteSize;
    }

    public int getDoubleByteSequences() {
        return this.doubleByteSequences;
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

    @Override
    public Map<String, Object> getProperties() {
        // TODO
        return null;
    }
}
