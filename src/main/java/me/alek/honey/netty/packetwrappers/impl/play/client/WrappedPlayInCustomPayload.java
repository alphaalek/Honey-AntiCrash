package me.alek.honey.netty.packetwrappers.impl.play.client;

import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.ArrayTag;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface WrappedPlayInCustomPayload<WP extends WrappedPlayInCustomPayload<WP>> extends WrappedPacket<WP>, AutoReleasable {

    PayloadData getPayloadData();

    class ByteWrappedPlayInCustomPayload extends ByteWrappedPacket<ByteWrappedPlayInCustomPayload> implements WrappedPlayInCustomPayload<ByteWrappedPlayInCustomPayload> {

        private final PayloadData data;

        public ByteWrappedPlayInCustomPayload(PacketDataSerializer buf, PacketContainer<ByteWrappedPlayInCustomPayload> packet) {
            super(buf, packet);
            String channel = Types.UTF8.read(buf, 20);
            PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.copiedBuffer(buf));
            this.data = new PayloadData(channel, serializer, packet.getProtocolByPacketState());
        }

        @Override
        public PayloadData getPayloadData() {
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

    class ObjectWrappedPlayInCustomPayload extends ObjectWrappedPacket<ObjectWrappedPlayInCustomPayload> implements WrappedPlayInCustomPayload<ObjectWrappedPlayInCustomPayload> {

        private static final AtomicReference<MethodInvoker<Object>> minecraftKeyCustomPayloadCache = new AtomicReference<>();
        private static final AtomicReference<MethodInvoker<Object>> payloadDataCustomPayloadCache = new AtomicReference<>();

        private final PayloadData data;

        public ObjectWrappedPlayInCustomPayload(Object rawPacket, PacketContainer<ObjectWrappedPlayInCustomPayload> packetContainer) {
            super(rawPacket, packetContainer);

            String channel;
            PacketDataSerializer serializer;

            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                serializer = new PacketDataSerializer(Unpooled.buffer());

                Object payload = getContainer().getStructure().getObjectsOf(NMS.getCustomPacketPayload1_20_2Class()).readField(0);
                Object key;
                if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                    Object payloadData = ReflectStructure.Provider.acquireStructure(NMS.getCustomPacketPayload1_20_2Class())
                            .withStatic(false).withTarget(payload).invokeMethod(payloadDataCustomPayloadCache, 0);

                    key = ReflectStructure.Provider.acquireStructure(payloadData.getClass())
                            .withType(NMS.getMinecraftKeyClass()).withTarget(payloadData).invokeMethod(minecraftKeyCustomPayloadCache, 0);
                } else {
                    key = ReflectStructure.Provider.acquireStructure(NMS.getCustomPacketPayload1_20_2Class())
                            .withType(NMS.getMinecraftKeyClass()).withTarget(payload).invokeMethod(minecraftKeyCustomPayloadCache, 0);
                }

                channel = key.toString();
            } else {
                serializer = getDataSerializer(false);

                if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_12_2)) {
                    Object key = getContainer().getStructure().getObjectsOf(NMS.getMinecraftKeyClass()).readField(0);

                    channel = key.toString();
                } else {
                    channel = getContainer().getStructure().getStrings().readField(0);
                }
            }

            this.data = new PayloadData(channel, serializer, NetProtocol.getProtocol());
        }

        public PacketDataSerializer getDataSerializer(boolean copy) {
            return getContainer().getStructure().getDataSerializers(copy).readField(0);
        }

        @Override
        public PayloadData getPayloadData() {
            return this.data;
        }

        @Override
        public void release() {
            AutoReleasable.doRelease(this.data.getSerializer());
            this.data.release();
        }
    }

    class PayloadData implements AutoReleasable {

        private final String channel;
        private final PacketDataSerializer serializer;
        private LazyInit<WrappedImmutableItemStack> bookChannelItemStack;
        private Runnable release;

        public PayloadData(String channel, PacketDataSerializer serializer, NetProtocol protocol) {
            this.channel = channel;
            this.serializer = serializer;

            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2) && isBookChannel()) {
                try {
                    PacketDataSerializer copy = makeSerializerCopy();
                    this.bookChannelItemStack = new LazyInit<>(() -> this.readItemStack(protocol, copy));
                    this.release = () -> AutoReleasable.doRelease(copy);
                } catch (Exception ignored) {
                }
            }
        }

        private WrappedImmutableItemStack readItemStack(NetProtocol protocol, PacketDataSerializer copy) {
            WrappedImmutableItemStack item = copy.readItemStack(protocol);

            if (item != null && item.getNBT() != null) {
                ArrayTag<String> pages = item.getNBT().getArrayOf("pages", TagType.STRING);
                if (channel.equals("MC|BEdit")) {
                    item.getNBT().clear();
                } else if (channel.equals("MC|BSign")) {
                    String author = item.getNBT().getString("author");
                    String title = item.getNBT().getString("title");
                    item.getNBT().clear();

                    item.getNBT().put("author", new PrimitiveTag<>(TagType.STRING, author));
                    item.getNBT().put("title", new PrimitiveTag<>(TagType.STRING, title));
                }

                item.getNBT().put("pages", pages);
            }

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
            properties.put("buffer", this.serializer.toString());
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
            return this.channel.equals("MC|BEdit") || this.channel.equals("MC|BSign");
        }
    }
}
