package me.alek.honey.checks.impl;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.checks.PacketCheck;
import me.alek.honey.checks.helper.ItemHelper;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CustomPayload extends AsyncPacketAdapter<WrappedPlayInCustomPayload> implements PacketCheck {

    private final Class<?> PACKET_SERIALIZER_CLASS = Reflection.getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    private final ConstructorInvoker PACKET_SERIALIZER_CONSTRUCTOR = Reflection.getConstructor(PACKET_SERIALIZER_CLASS, ByteBuf.class);
    private final MethodInvoker ITEMSTACK_SERIALIZER_INVOKER = Reflection.getMethodFromReturnType(PACKET_SERIALIZER_CLASS, 0, ItemHelper.getNMSItemStackClass());

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        WrappedPlayInCustomPayload packet = packetContainer.getPacket();
        Bukkit.broadcastMessage("received custom payload: " + packet);
        ByteBuf priorSerializer;
        try {
            priorSerializer = (ByteBuf) packet.getDataSerializer();
        } catch (Exception ex) {
            return;
        }
        ByteBuf serializer = (ByteBuf) PACKET_SERIALIZER_CONSTRUCTOR.invoke(Unpooled.wrappedBuffer(priorSerializer));
        Object item = null;
        ItemStack bukkitItem = null;
        try {
            item = ITEMSTACK_SERIALIZER_INVOKER.invokeWithException(serializer);
            bukkitItem = ItemHelper.asBukkitItem(item);
        } catch (Exception ex) {
        }
        ItemStack itemInHand = player.getItemInHand();

        Object tag = NBTHelper.getTag(item);
        if (tag != null) {
            if (tag.toString().getBytes().length > ConfigurationOptions.CustomPayload.MAX_SERIALIZED_ITEM_NBT_LENGTH) {
                punishAndConfiscate(player, packetContainer, "Illegal Nbt", bukkitItem);
                serializer.release();
                return;
            }
        }
        if (NBTHelper.checkBukkitNBT(bukkitItem)) {
            punishAndConfiscate(player, packetContainer, "Illegal NBT", bukkitItem);
            serializer.release();
            return;
        }
        final int refCnt = serializer.refCnt();
        final int capacity = serializer.capacity();

        Bukkit.broadcastMessage("refCnt: " + refCnt + ", capacity: " + capacity);
        if (       capacity > ConfigurationOptions.CustomPayload.MAX_BYTEBUF_CAPACITY
                || capacity < ConfigurationOptions.CustomPayload.MIN_BYTEBUF_CAPACITY
                || refCnt > ConfigurationOptions.CustomPayload.MAX_BYTEBUF_REFCNT
                || refCnt < ConfigurationOptions.CustomPayload.MIN_BYTEBUF_REFCNT
        ) {
            punishAndConfiscate(player, packetContainer, "Illegal Bytebuf", bukkitItem);
            serializer.release();
            return;
        }
        String key = packet.getKeyString();
        if (key.contains("BSign") || key.contains("BEdit") || key.contains("BOpen")) {
            if (!ItemHelper.isBook(itemInHand)) {
                punishAndConfiscate(player, packetContainer, "Illegal Book", bukkitItem);
                serializer.release();
                return;
            }
        }
        if (key.toLowerCase().contains("register")) {
            int channelSize = player.getListeningPluginChannels().size();

            for (int i = 0; i < serializer.toString(Charsets.UTF_8).split("\000").length; i++) {
                if (++channelSize > ConfigurationOptions.CustomPayload.MAX_PLUGIN_CHANNELS) {
                    punish(player, packetContainer, "Too Many Channels");
                    serializer.release();
                    return;
                }
            }
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CUSTOM_PAYLOAD);
    }

    @Override
    public String getName() {
        return "Custom Payload";
    }
}
