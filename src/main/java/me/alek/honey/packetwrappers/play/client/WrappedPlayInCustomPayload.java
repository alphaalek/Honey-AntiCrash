package me.alek.honey.packetwrappers.play.client;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInCustomPayload extends WrappedPacket<WrappedPlayInCustomPayload> {

    private static final boolean MODERN = NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_12_2);

    private final ByteBuf buf;
    private final String channel;
    private ItemStack itemStack;

    public WrappedPlayInCustomPayload(Object rawPacket, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);

        buf = getDataSerializer(false);
        channel = getMinecraftKey().toString();

        if (channel.equals("MC|BEdit") || channel.equals("MC|BSign")) {
            try {
                itemStack = Converters.getItemStackConverter().convertDelegate(getDataSerializer(true).readItemStack());
            } catch (Exception ignored) {
            }
        }
    }

    public PacketDataSerializer getDataSerializer(boolean copy) {
        return getContainer().getDataSerializers(copy).readField(0);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    private Object getMinecraftKey() {
        if (MODERN) {
            return getContainer().getObjects(NMS.getMinecraftKeyClass()).readField(0);
        }
        return getContainer().getStrings().readField(0);
    }

    public String getChannel() {
        return channel;
    }
}
