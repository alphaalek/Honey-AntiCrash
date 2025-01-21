package me.alek.honey.netty.packetwrappers.impl.play.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecType;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.ArrayTag;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface WrappedPlayInCustomPayload<WP extends WrappedPlayInCustomPayload<WP>> extends WrappedPacket<WP>, AutoReleasable {

    PayloadData<WP> getPayloadData();

    class ByteWrappedPlayInCustomPayload extends ByteWrappedPacket<ByteWrappedPlayInCustomPayload> implements WrappedPlayInCustomPayload<ByteWrappedPlayInCustomPayload> {

        private final PayloadData<ByteWrappedPlayInCustomPayload> data;

        public ByteWrappedPlayInCustomPayload(PacketDataSerializer buf, PacketContainer<ByteWrappedPlayInCustomPayload> packet) {
            super(buf, packet);
            String channel = Types.UTF8.read(buf, 20);
            PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.copiedBuffer(buf));
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

        private static final String EDIT_BOOK_CHANNEL = "MC|BEdit";
        private static final String SIGN_BOOK_CHANNEL = "MC|BSign";

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
                try {
                    PacketDataSerializer copy = makeSerializerCopy();
                    this.bookChannelItemStack = new LazyInit<>(() -> packet.decodeExternally(() -> this.readItemStack(protocol, copy)));
                    this.release = () -> AutoReleasable.doRelease(copy);
                } catch (Exception ignored) {
                }
            }
        }

        private WrappedImmutableItemStack readItemStack(NetProtocol protocol, PacketDataSerializer serializer) {
            WrappedImmutableItemStack item = serializer.readItemStack(protocol);

            if (item == null) {
                throw new DecoderException("Invalid book!");
            }

            boolean signing = this.channel.equals(SIGN_BOOK_CHANNEL);
            CodecType.BOOK_EDIT_ITEM_STACK.validate(item, new DecodeContext.BookEditContext<>(this.packet, signing));

            CompoundTag tag = new CompoundTag();
            tag.put("pages", item.getNBT().getArrayOf("pages", TagType.STRING));
            if (signing) {
                tag.put("author", new PrimitiveTag<>(TagType.STRING, item.getNBT().getString("author")));
                tag.put("title", new PrimitiveTag<>(TagType.STRING, item.getNBT().getString("title")));

                if (protocol.isEqual(NetProtocol.v1_8_8)) {
                    for (PrimitiveTag<String> page : tag.getArrayOf("pages", String.class)) {
                        JsonElement element = JSON_PARSER.parse(page.getValue());
                        if (element.isJsonObject()) {
                            page.setValue(((JsonObject) element).get("text").getAsString());
                        } else {
                            page.setValue(element.getAsString());
                        }
                    }
                }
            }
            item.setNBT(tag);

            return item;
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
