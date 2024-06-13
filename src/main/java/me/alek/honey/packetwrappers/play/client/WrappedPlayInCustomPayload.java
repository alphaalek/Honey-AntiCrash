package me.alek.honey.packetwrappers.play.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.MethodInvoker;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class WrappedPlayInCustomPayload extends WrappedPacket<WrappedPlayInCustomPayload> {

    private static final AtomicReference<MethodInvoker<Object>> minecraftKeyCustomPayloadCache = new AtomicReference<>();
    private static final AtomicReference<MethodInvoker<Object>> payloadDataCustomPayloadCache = new AtomicReference<>();

    private final String channel;
    private final ByteBuf buf;
    private ItemStack bookChannelItemStack;

    public WrappedPlayInCustomPayload(Object rawPacket, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
            buf = Unpooled.buffer();

            Object payload = getContainer().getObjects(NMS.getCustomPacketPayload1_20_2Class()).readField(0);
            Object key;
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                Object payloadData = ReflectStructureCache.acquireStructure(NMS.getCustomPacketPayload1_20_2Class())
                        .withoutStatic().withTarget(payload).invokeMethod(payloadDataCustomPayloadCache, 0);

                key = ReflectStructureCache.acquireStructure(payloadData.getClass())
                        .withType(NMS.getMinecraftKeyClass()).withTarget(payloadData).invokeMethod(minecraftKeyCustomPayloadCache, 0);
            } else {
                key = ReflectStructureCache.acquireStructure(NMS.getCustomPacketPayload1_20_2Class())
                        .withType(NMS.getMinecraftKeyClass()).withTarget(payload).invokeMethod(minecraftKeyCustomPayloadCache, 0);
            }

            channel = key.toString();
        } else {
            buf = getDataSerializer(false);

            if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_12_2)) {
                Object key = getContainer().getObjects(NMS.getMinecraftKeyClass()).readField(0);

                channel = key.toString();
            } else {
                channel = getContainer().getStrings().readField(0);
            }

            if (channel.equals("MC|BEdit") || channel.equals("MC|BSign")) {
                try {
                    bookChannelItemStack = Converters.getItemStackConverter().convertDelegate(getDataSerializer(true).readItemStack());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private PacketDataSerializer getDataSerializer(boolean copy) {
        return getContainer().getDataSerializers(copy).readField(0);
    }

    public ItemStack getBookChannelItemStack() {
        return bookChannelItemStack;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    public String getChannel() {
        return channel;
    }
}
