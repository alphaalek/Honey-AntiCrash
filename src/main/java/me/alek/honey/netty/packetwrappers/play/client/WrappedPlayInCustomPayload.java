package me.alek.honey.netty.packetwrappers.play.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class WrappedPlayInCustomPayload extends WrappedPacket<WrappedPlayInCustomPayload> {

    private static final AtomicReference<MethodInvoker<Object>> minecraftKeyCustomPayloadCache = new AtomicReference<>();
    private static final AtomicReference<MethodInvoker<Object>> payloadDataCustomPayloadCache = new AtomicReference<>();

    private final String channel;
    private final ByteBuf buf;
    private LazyInit<ItemStack> bookChannelItemStack;

    public WrappedPlayInCustomPayload(Object rawPacket, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
            buf = Unpooled.buffer();

            Object payload = getContainer().getObjectsOf(NMS.getCustomPacketPayload1_20_2Class()).readField(0);
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
            buf = getDataSerializer(false);

            if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_12_2)) {
                Object key = getContainer().getObjectsOf(NMS.getMinecraftKeyClass()).readField(0);

                channel = key.toString();
            } else {
                channel = getContainer().getStrings().readField(0);
            }

            if (channel.equals("MC|BEdit") || channel.equals("MC|BSign")) {
                try {
                    PacketDataSerializer copy = getDataSerializer(true);
                    bookChannelItemStack = new LazyInit<>(() ->
                            Converters.getItemStackConverter().convertDelegate(copy.readItemStack()));
                } catch (Exception ignored) {
                }
            }
        }
    }

    public PacketDataSerializer getDataSerializer(boolean copy) {
        return getContainer().getDataSerializers(copy).readField(0);
    }

    public ItemStack getBookChannelItemStack() {
        return Optional.ofNullable(bookChannelItemStack).map(LazyInit::get).orElse(null);
    }

    public ByteBuf getBuf() {
        return buf;
    }

    public String getChannel() {
        return channel;
    }
}
