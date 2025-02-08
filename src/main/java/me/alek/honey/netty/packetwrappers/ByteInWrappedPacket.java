package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.Reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ByteInWrappedPacket<WP extends SourcedWrappedPacket<WP>> implements ByteWrappedPacket<WP>, SourcedWrappedPacket<WP> {

    private final PacketContainer<WP> packet;
    private final PacketContainer.PipelineState state;
    private final PacketDataSerializer buf;
    private final int byteSize;
    private final int accumulatedByteSize;
    private final int doubleByteSequences;

    public ByteInWrappedPacket(PacketDataSerializer buf, PacketContainer<WP> packet) {
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
        return !getClass().equals(ByteInWrappedPacket.class);
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
