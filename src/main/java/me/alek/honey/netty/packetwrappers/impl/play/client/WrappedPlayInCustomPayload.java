package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class WrappedPlayInCustomPayload extends ByteInWrappedPacketImpl.CacheRewriteCaller<WrappedPlayInCustomPayload> {

    int MAX_CHANNEL_LENGTH = 32;

    private final PayloadData data;

    public WrappedPlayInCustomPayload(PacketDataSerializer buf, PacketContainer<WrappedPlayInCustomPayload> packet) {
        super(buf, packet, false);
        String channel = buf.readString(MAX_CHANNEL_LENGTH);
        PacketDataSerializer serializer = new PacketDataSerializer(buf.readBytes(buf.readableBytes()));
        this.data = new PayloadData(this, packet, channel, serializer);
    }

    public PayloadData getPayloadData() {
        return data;
    }

    @Override
    public void rewrite(PacketDataSerializer serializer) {
        serializer.writeString(this.data.getChannel());
        super.rewrite(serializer);
    }

    @Override
    public void release() {
        super.release();
        AutoReleasable.doRelease(this.data.getSerializer());
    }

    @Override
    public Map<String, Object> getProperties() {
        return data.getProperties();
    }

    public static class PayloadData {

        public static final String EDIT_BOOK_CHANNEL = "MC|BEdit";
        public static final String SIGN_BOOK_CHANNEL = "MC|BSign";

        private final String channel;
        private final PacketDataSerializer serializer;
        private final boolean signing;
        private WrappedImmutableItemStack bookChannelItemStack;

        public PayloadData(WrappedPlayInCustomPayload source, PacketContainer<WrappedPlayInCustomPayload> packet, String channel, PacketDataSerializer serializer) {
            this.channel = channel;
            this.serializer = serializer;
            this.signing = this.channel.equals(SIGN_BOOK_CHANNEL);

            if (packet.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2) && isBookChannel()) {
                this.bookChannelItemStack = BookTag.decodeBookEditStack(source, packet, this.signing, serializer);
            }
        }

        public Map<String, Object> getProperties() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("channel", this.channel);
            properties.put("buffer", this.serializer);
            return properties;
        }

        public WrappedImmutableItemStack getBookChannelItemStack() {
            return this.bookChannelItemStack;
        }

        public PacketDataSerializer getSerializer() {
            return this.serializer;
        }

        public String getChannel() {
            return this.channel;
        }

        public boolean isSigning() {
            return this.signing;
        }

        public boolean isBookChannel() {
            return this.channel.equals(EDIT_BOOK_CHANNEL) || this.channel.equals(SIGN_BOOK_CHANNEL);
        }
    }
}
