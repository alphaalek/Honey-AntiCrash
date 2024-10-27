package me.alek.honey.netty.packetwrappers.play.client;

import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface WrappedPlayInCustomPayload<WP extends WrappedPlayInCustomPayload<WP>> extends WrappedPacket<WP> {

    PayloadData getData();

    class ByteWrappedPlayInCustomPayload extends ByteWrappedPacket<ByteWrappedPlayInCustomPayload> implements WrappedPlayInCustomPayload<ByteWrappedPlayInCustomPayload> {

        private final PayloadData data;

        public ByteWrappedPlayInCustomPayload(PacketDataSerializer buf, PacketContainer<ByteWrappedPlayInCustomPayload> packet) {
            super(buf, packet);
            String channel = Types.UTF8.read(buf);
            PacketDataSerializer serializer = new PacketDataSerializer(buf.readBytes(buf.readableBytes()));
            this.data = new PayloadData(channel, serializer);
        }

        @Override
        public PayloadData getData() {
            return data;
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
                            .withoutStatic().withTarget(payload).invokeMethod(payloadDataCustomPayloadCache, 0);

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

            this.data = new PayloadData(channel, serializer);
        }

        public PacketDataSerializer getDataSerializer(boolean copy) {
            return getContainer().getStructure().getDataSerializers(copy).readField(0);
        }

        @Override
        public PayloadData getData() {
            return this.data;
        }
    }

    class PayloadData {

        private final String channel;
        private final PacketDataSerializer serializer;
        private LazyInit<ItemStack> bookChannelItemStack;

        public PayloadData(String channel, PacketDataSerializer serializer) {
            this.channel = channel;
            this.serializer = serializer;

            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2) && isBookChannel()) {
                try {
                    PacketDataSerializer copy = getCopiedSerializer();
                    bookChannelItemStack = new LazyInit<>(() ->
                            Converters.getItemStackConverter()
                                    .convertDelegate(
                                            copy.readItemStackWithFilter(true, Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book"))
                                    )
                    );
                } catch (Exception ignored) {
                }
            }
        }

        public ItemStack getBookChannelItemStack() {
            return Optional.ofNullable(bookChannelItemStack).map(LazyInit::get).orElse(null);
        }

        public PacketDataSerializer getSerializer() {
            return serializer;
        }

        public PacketDataSerializer getCopiedSerializer() {
            return new PacketDataSerializer(Unpooled.copiedBuffer(serializer));
        }

        public String getChannel() {
            return channel;
        }

        public boolean isBookChannel() {
            return channel.equals("MC|BEdit") || channel.equals("MC|BSign");
        }
    }
}
