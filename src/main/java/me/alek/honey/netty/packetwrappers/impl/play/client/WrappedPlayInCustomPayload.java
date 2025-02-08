package me.alek.honey.netty.packetwrappers.impl.play.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.visitor.book.EditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.*;

public interface WrappedPlayInCustomPayload<WP extends WrappedPlayInCustomPayload<WP>> extends SourcedWrappedPacket<WP>, AutoReleasable {

    int MAX_CHANNEL_LENGTH = 20;

    PayloadData<WP> getPayloadData();

    class ByteWrappedPlayInCustomPayload extends ByteInWrappedPacket<ByteWrappedPlayInCustomPayload> implements WrappedPlayInCustomPayload<ByteWrappedPlayInCustomPayload> {

        private final PayloadData<ByteWrappedPlayInCustomPayload> data;

        public ByteWrappedPlayInCustomPayload(PacketDataSerializer buf, PacketContainer<ByteWrappedPlayInCustomPayload> packet) {
            super(buf, packet);
            String channel = buf.readString(MAX_CHANNEL_LENGTH);
            PacketDataSerializer serializer = new PacketDataSerializer(buf.readBytes(buf.readableBytes()));
            this.data = new PayloadData<>(this, packet, channel, serializer);
        }

        @Override
        public PayloadData<ByteWrappedPlayInCustomPayload> getPayloadData() {
            return data;
        }

        @Override
        public void release() {
            AutoReleasable.doRelease(this.data.getSerializer());
        }

        @Override
        public Map<String, Object> getProperties() {
            return data.getProperties();
        }
    }

    class PayloadData<WP extends WrappedPlayInCustomPayload<WP>> {

        public static final String EDIT_BOOK_CHANNEL = "MC|BEdit";
        public static final String SIGN_BOOK_CHANNEL = "MC|BSign";

        private static final JsonParser JSON_PARSER = new JsonParser();

        private final String channel;
        private final PacketDataSerializer serializer;
        private final PacketContainer<WP> packet;
        private final NetProtocol protocol;
        private final NetProtocol protocolCreated;
        private final boolean signing;
        private LazyInit<WrappedImmutableItemStack> bookChannelItemStack;

        public PayloadData(WP source, PacketContainer<WP> packet, String channel, PacketDataSerializer serializer) {
            this.channel = channel;
            this.serializer = serializer;
            this.packet = packet;
            this.protocol = source.getProtocol();
            this.protocolCreated = source.getProtocolCreated();
            this.signing = this.channel.equals(SIGN_BOOK_CHANNEL);

            if (this.protocol.isOlderThanOrEqual(NetProtocol.v1_12_2) && isBookChannel()) {
                this.bookChannelItemStack = new LazyInit<>(() -> source.decodeExternally(() -> this.readItemStack(serializer)));
            }
        }

        private WrappedImmutableItemStack readItemStack(PacketDataSerializer serializer) {
            EditBookTagVisitor visitor = this.signing ? EditBookTagVisitor.of(this.protocol, this.protocolCreated, true, packet.getPlayerName()) : EditBookTagVisitor.of(this.protocol, this.protocolCreated);
            WrappedImmutableItemStack stack = serializer.readItemStack(visitor, 0); // no need to read safely, because we applied the edit book tag visitor
            CodecTypeValidation.BOOK_EDIT_ITEM_STACK.validate(stack, DecodeContext.BookEditContext.from(serializer, this.packet, this.signing));

            HoneyPacketCodec.checkReadable(this.protocolCreated, this.packet.getType(), Optional.ofNullable(this.packet.getObjectWrapped()).map(WrappedObject::getHandle).orElse(null), serializer);

            applyTagLegacy(stack, this.signing);
            return stack;
        }

        private void applyTagLegacy(WrappedImmutableItemStack item, boolean signing) {
            CompoundTag tag = new CompoundTag();
            CompoundTag serialized = item.getTag().asNBT().get();
            tag.put("pages", serialized.getArrayOf("pages", TagType.STRING));
            if (signing) {
                tag.put("author", serialized.get("author"));
                tag.put("title", serialized.get("title"));

                if (this.protocol.isEqual(NetProtocol.v1_8_8)) {
                    List<String> newPages = new ArrayList<>();
                    for (String page : tag.<String>getArrayOf("pages", TagType.STRING)) {
                        JsonElement element = JSON_PARSER.parse(page);
                        if (element.isJsonObject()) {
                            newPages.add(((JsonObject) element).get("text").getAsString());
                        } else {
                            newPages.add(element.getAsString());
                        }
                    }
                    tag.putArray("pages", TagType.STRING, newPages);
                }
            }
            item.setTag(tag);
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

        public String getChannel() {
            return this.channel;
        }

        public boolean isBookChannel() {
            return this.channel.equals(EDIT_BOOK_CHANNEL) || this.channel.equals(SIGN_BOOK_CHANNEL);
        }
    }
}
