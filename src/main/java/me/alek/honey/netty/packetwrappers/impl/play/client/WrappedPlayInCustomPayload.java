package me.alek.honey.netty.packetwrappers.impl.play.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.validation.CodecType;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.PrimitiveTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface WrappedPlayInCustomPayload<WP extends WrappedPlayInCustomPayload<WP>> extends SourcedWrappedPacket<WP>, AutoReleasable {

    int MAX_CHANNEL_LENGTH = 20;

    PayloadData<WP> getPayloadData();

    class ByteWrappedPlayInCustomPayload extends ByteInWrappedPacket<ByteWrappedPlayInCustomPayload> implements WrappedPlayInCustomPayload<ByteWrappedPlayInCustomPayload> {

        private final PayloadData<ByteWrappedPlayInCustomPayload> data;

        public ByteWrappedPlayInCustomPayload(PacketDataSerializer buf, PacketContainer<ByteWrappedPlayInCustomPayload> packet) {
            super(buf, packet);
            String channel = buf.readString(MAX_CHANNEL_LENGTH);
            PacketDataSerializer serializer = new PacketDataSerializer(buf.readBytes(buf.readableBytes()));
            this.data = new PayloadData<>(this, channel, serializer, packet.getProtocol());
        }

        @Override
        public PayloadData<ByteWrappedPlayInCustomPayload> getPayloadData() {
            return data;
        }

        @Override
        public void release() {
            AutoReleasable.doRelease(this.data.getSerializer());
            this.data.release();
        }

        @Override
        public Map<String, Object> getProperties() {
            return data.getProperties();
        }
    }

    class PayloadData<WP extends WrappedPlayInCustomPayload<WP>> implements AutoReleasable {

        public static final String EDIT_BOOK_CHANNEL = "MC|BEdit";
        public static final String SIGN_BOOK_CHANNEL = "MC|BSign";

        private static final JsonParser JSON_PARSER = new JsonParser();

        private final String channel;
        private final PacketDataSerializer serializer;
        private final WP packet;
        private LazyInit<WrappedImmutableItemStack> bookChannelItemStack;
        private Runnable release;

        public PayloadData(WP packet, String channel, PacketDataSerializer serializer, NetProtocol protocol) {
            this.channel = channel;
            this.serializer = serializer;
            this.packet = packet;

            if (protocol.isOlderThanOrEqual(NetProtocol.v1_12_2) && isBookChannel()) {
                this.bookChannelItemStack = new LazyInit<>(() -> packet.decodeExternally(() -> this.readItemStack(protocol, serializer)));
                this.release = () -> AutoReleasable.doRelease(serializer);
            }
        }

        private WrappedImmutableItemStack readItemStack(NetProtocol protocol, PacketDataSerializer serializer) {
            WrappedImmutableItemStack item = serializer.readItemStack(protocol);

            if (item == null) {
                throw new DecoderException("Invalid book!");
            }

            boolean signing = this.channel.equals(SIGN_BOOK_CHANNEL);
            CodecType.BOOK_EDIT_ITEM_STACK.validate(item, new DecodeContext.BookEditContext<>(this.packet, signing));

            applyTagLegacy(protocol, item, signing);
            return item;
        }

        private void applyTagLegacy(NetProtocol protocol, WrappedImmutableItemStack item, boolean signing) {
            CompoundTag tag = new CompoundTag();
            CompoundTag serialized = item.getTag().asNBT().get();
            tag.put("pages", serialized.getArrayOf("pages", TagType.STRING));
            if (signing) {
                tag.put("author", new PrimitiveTag<>(TagType.STRING, serialized.getString("author")));
                tag.put("title", new PrimitiveTag<>(TagType.STRING, serialized.getString("title")));

                if (protocol.isEqual(NetProtocol.v1_8_8)) {
                    ArrayTag<String> newPages = new ArrayTag<>(TagType.STRING);
                    for (String page : tag.<String>getArrayOf("pages", TagType.STRING)) {
                        JsonElement element = JSON_PARSER.parse(page);
                        if (element.isJsonObject()) {
                            newPages.add(((JsonObject) element).get("text").getAsString());
                        } else {
                            newPages.add(element.getAsString());
                        }
                    }
                    tag.put("pages", newPages);
                }
            }
            item.setTag(tag);
        }

        @Override
        public void release() {
            if (this.release != null) {
                this.release.run();
            }
        }

        public Map<String, Object> getProperties() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("channel", this.channel);
            properties.put("buffer", this.serializer);
            return properties;
        }

        public boolean isItemInitialized() {
            return this.bookChannelItemStack.initialized();
        }

        public WrappedImmutableItemStack getBookChannelItemStack() {
            return Optional.ofNullable(this.bookChannelItemStack).map(LazyInit::get).orElse(null);
        }

        public PacketDataSerializer getSerializer() {
            return this.serializer;
        }

        public PacketDataSerializer makeSerializerCopy() {
            return new PacketDataSerializer(Unpooled.copiedBuffer(this.serializer));
        }

        public String getChannel() {
            return this.channel;
        }

        public boolean isBookChannel() {
            return this.channel.equals(EDIT_BOOK_CHANNEL) || this.channel.equals(SIGN_BOOK_CHANNEL);
        }
    }
}
