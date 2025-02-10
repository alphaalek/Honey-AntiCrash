package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;
import org.bukkit.Bukkit;

public class ByteInWrappedPacketImpl<WP extends SourcedWrappedPacket<WP>> implements ByteInWrappedPacket<WP> {

    public static class CacheRewriteCaller<WP extends ByteInWrappedPacket<WP>> extends ByteInWrappedPacketImpl<WP> implements PacketRewriter, AutoReleasable {

        protected PacketDataSerializer cachedRewrittenSerializer;

        public CacheRewriteCaller(PacketDataSerializer buf, PacketContainer<WP> packet) {
            super(buf, packet);
        }

        @Override
        public PacketDataSerializer serializer() {
            return this.cachedRewrittenSerializer != null ? this.cachedRewrittenSerializer : super.serializer();
        }

        public void handleRewritten(Rewritable<?> rewritable) {
            if (rewritable.rewritten().isRewritten()) {
                if (this.cachedRewrittenSerializer != null) {
                    // release prior cached rewritten serializers, as we have expanded the rewriting of the buffer
                    this.cachedRewrittenSerializer.release();
                }

                this.cachedRewrittenSerializer = rewritable.rewritten().get();
            }
        }

        @Override
        public void release() {
            if (this.cachedRewrittenSerializer != null) {
                AutoReleasable.doRelease(this.cachedRewrittenSerializer);
            }
        }

        @Override
        public boolean shouldRewrite() {
            return this.cachedRewrittenSerializer != null;
        }

        @Override
        public void rewrite(PacketDataSerializer serializer) {
            Bukkit.getLogger().info("Rewriting " + cachedRewrittenSerializer);
            serializer.writeBytes(this.cachedRewrittenSerializer.readerIndex(0));
            this.cachedRewrittenSerializer = null;
        }
    }

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
            this.accumulatedByteSize = packet.getAnyByteWrapped().accumulatedBytes();
            this.doubleByteSequences = packet.getAnyByteWrapped().doubleByteSequences();
        }
    }

    @Override
    public boolean isAbstract() {
        return !getClass().equals(ByteInWrappedPacketImpl.class);
    }

    @Override
    public PacketDataSerializer serializer() {
        return this.buf;
    }

    @Override
    public int bytes() {
        return this.byteSize;
    }

    @Override
    public int accumulatedBytes() {
        return this.accumulatedByteSize;
    }

    @Override
    public int doubleByteSequences() {
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
