package me.alek.honey.checks.listeners;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.ItemReflection;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CustomPayloadListener extends AbstractPacketListener<WrappedPlayInCustomPayload> implements IPacketListener {

    private final Class<?> PACKET_SERIALIZER_CLASS = Reflection.getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    private final ConstructorInvoker PACKET_SERIALIZER_CONSTRUCTOR = Reflection.getConstructor(PACKET_SERIALIZER_CLASS, ByteBuf.class);
    private final MethodInvoker ITEMSTACK_SERIALIZER_INVOKER = Reflection.getMethodFromReturnType(PACKET_SERIALIZER_CLASS, 0, ItemReflection.getNMSItemStackClass());

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInCustomPayload> packetContainer, CheckContextContainer container) {
        ByteBuf priorSerializer;
        try {
            priorSerializer = (ByteBuf) packetContainer.getPacket().getDataSerializer();
        } catch (Exception ex) {
            return false;
        }
        ByteBuf newSerializer = (ByteBuf) PACKET_SERIALIZER_CONSTRUCTOR.invoke(Unpooled.wrappedBuffer(priorSerializer));

        ItemStack bukkitItem = null;
        try {
            Object item = ITEMSTACK_SERIALIZER_INVOKER.invokeWithException(newSerializer);
            bukkitItem = ItemReflection.asBukkitItem(item);
        } catch (Exception ex) {
        }

        container.putSerializer(priorSerializer);
        container.putItem(bukkitItem);
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInCustomPayload> getVisitor() {
        CheckVisitor<WrappedPlayInCustomPayload> visitor = new CheckVisitor<>();
        visitor.visit(new PayloadA());
        visitor.visit(new PayloadB());
        visitor.visit(new PayloadC());
        visitor.visit(new PayloadD());
        visitor.visit(new PayloadE());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CUSTOM_PAYLOAD);
    }

    public static class PayloadA implements PacketCheck<WrappedPlayInCustomPayload> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            ByteBuf serializer = container.getSerializer(0);
            if (serializer == null || serializer instanceof EmptyByteBuf) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .terminate()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PayloadA";
        }
    }

    public static class PayloadB implements PacketCheck<WrappedPlayInCustomPayload> {
        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            ByteBuf serializer = container.getSerializer(0);
            if (serializer == null) {
                return ResponseBuilder.SUCCESS; // burde aldrig ske, for så vil PayloadA være blevet flagged
            }
            if (       serializer.capacity() > ConfigurationOptions.CustomPayload.MAX_BYTEBUF_CAPACITY
                    || serializer.readableBytes() > ConfigurationOptions.CustomPayload.MAX_BYTEBUF_SIZE
            ) {
                serializer.release();
                ItemStack bukkitItem = container.getItem(0);

                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .confiscate(bukkitItem)
                        .terminate()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PayloadB";
        }
    }

    public static class PayloadC implements PacketCheck<WrappedPlayInCustomPayload> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            ItemStack item = container.getItem(0);
            ItemStack itemInHand = player.getItemInHand();

            if (!(ItemAnalysis.checkItem(itemInHand) || ItemAnalysis.checkItem(item))) {
                if (item == null) {
                    return ResponseBuilder.SUCCESS;
                }
                NBTCompound tag = NBTFactory.fromItem(item);
                if (tag == null || tag.toString().getBytes().length <= ConfigurationOptions.CustomPayload.MAX_SERIALIZED_ITEM_NBT_LENGTH) {
                    return ResponseBuilder.SUCCESS;
                }
            }
            ByteBuf serializer = container.getSerializer(0);
            if (serializer != null && serializer.refCnt() > 0) {
                serializer.release();
            }
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .confiscate(item)
                    .terminate()
                    .build();
        }

        @Override
        public String getViolationName() {
            return "PayloadC";
        }
    }

    public static class PayloadD implements PacketCheck<WrappedPlayInCustomPayload> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            String channel = packet.getChannel();
            ItemStack itemInHand = player.getItemInHand();

            if (!channel.isEmpty()) {
                if (!channel.contains("BSign") && !channel.contains("BEdit") && !channel.contains("BOpen")) {
                    return ResponseBuilder.SUCCESS;
                }
                if (itemInHand.getType() == Material.BOOK || itemInHand.getType() == Material.BOOK_AND_QUILL || itemInHand.getType() == Material.WRITTEN_BOOK) {
                    return ResponseBuilder.SUCCESS;
                }
            }
            ByteBuf serializer = container.getSerializer(0);
            if (serializer != null) {
                serializer.release();
            }
            ItemStack bukkitItem = container.getItem(0);
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .confiscate(bukkitItem)
                    .terminate()
                    .build();
        }

        @Override
        public String getViolationName() {
            return "PayloadD";
        }
    }

    public static class PayloadE implements PacketCheck<WrappedPlayInCustomPayload> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            String channel = packet.getChannel();
            if (channel == null) {
                return ResponseBuilder.SUCCESS; // burde aldrig ske, fordi PayloadD vil terminate visitor hvis channel er empty
            }
            if (channel.equals("BungeeCord")) {
                return ResponseBuilder.SUCCESS; // ingen grund til at tjekke for BungeeCord plugin channels
            }
            ByteBuf serializer = container.getSerializer(0);

            if (channel.toLowerCase().contains("register")) {
                int channelSize = player.getListeningPluginChannels().size();

                for (int i = 0; i < serializer.toString(Charsets.UTF_8).split("\000").length; i++) {
                    if (++channelSize > ConfigurationOptions.CustomPayload.MAX_PLUGIN_CHANNELS) {
                        serializer.release();
                        return ResponseBuilder
                                .inst()
                                .cancel()
                                .punish()
                                .terminate()
                                .build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PayloadE";
        }
    }

}
