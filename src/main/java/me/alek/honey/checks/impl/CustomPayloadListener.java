package me.alek.honey.checks.impl;

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
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.checks.types.ViolationKeyedCheck;
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
import me.alek.honey.services.ViolationService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CustomPayloadListener extends AbstractPacketListener<WrappedPlayInCustomPayload> implements IPacketListener {

    private final Class<?> PACKET_SERIALIZER_CLASS = Reflection.getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    private final ConstructorInvoker<?> PACKET_SERIALIZER_CONSTRUCTOR = Reflection.getConstructor(PACKET_SERIALIZER_CLASS, ByteBuf.class);
    private final MethodInvoker<?> ITEMSTACK_SERIALIZER_INVOKER = Reflection.getMethodFromReturnType(PACKET_SERIALIZER_CLASS, 0, ItemReflection.getNMSItemStackClass());

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInCustomPayload> packetContainer, CheckContextContainer container) {
        ByteBuf priorSerializer;
        try {
            priorSerializer = (ByteBuf) packetContainer.getPacket().getDataSerializer().getHandle();
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

        container.put(ByteBuf.class, priorSerializer);
        container.put(ItemStack.class, bukkitItem);
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
        visitor.visit(new PayloadF());
        visitor.visit(new PayloadG());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CUSTOM_PAYLOAD);
    }

    public static class PayloadA implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ByteBuf serializer = container.get(ByteBuf.class, 0);
                if (serializer == null || serializer instanceof EmptyByteBuf) {
                    return ResponseBuilder.inst().cancel().flag().terminate().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.A";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadA.class);
        }
    }

    public static class PayloadB implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {
        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ByteBuf serializer = container.get(ByteBuf.class,0);
                if (serializer == null) {
                    return ResponseBuilder.SUCCESS; // burde aldrig ske, for så vil PayloadA være blevet flagged
                }
                if (serializer.capacity() > ConfigurationOptions.CustomPayload.PayloadB.MAX) {
                    serializer.release();
                    return ResponseBuilder.inst().cancel().flag().terminate().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.B";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadB.class);
        }
    }

    public static class PayloadC implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = container.poll(ItemStack.class);
                ItemStack itemInHand = player.getItemInHand();

                if (!(ItemAnalysis.checkItem(player, itemInHand) || ItemAnalysis.checkItem(player, item))) {
                    if (item == null) {
                        return ResponseBuilder.SUCCESS;
                    }
                    NBTCompound tag = NBTFactory.fromItem(item);
                    if (tag == null || tag.toString().getBytes().length <= ConfigurationOptions.CustomPayload.PayloadC.MAX) {
                        return ResponseBuilder.SUCCESS;
                    }
                }
                ByteBuf serializer = container.poll(ByteBuf.class);
                if (serializer != null && serializer.refCnt() > 0) {
                    serializer.release();
                }
                return ResponseBuilder.inst().cancel().flag().terminate().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.C";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadC.class);
        }
    }

    public static class PayloadD implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String channel = packet.getChannel();
                ItemStack itemInHand = player.getItemInHand();

                if (!channel.isEmpty()) {
                    if (!channel.contains("BSign") && !channel.contains("BEdit") && !channel.contains("BOpen")) {
                        return ResponseBuilder.SUCCESS;
                    }
                    if (itemInHand.getType() == Material.BOOK_AND_QUILL || itemInHand.getType() == Material.WRITTEN_BOOK) {
                        return ResponseBuilder.SUCCESS;
                    }
                }
                ByteBuf serializer = container.get(ByteBuf.class, 0);
                if (serializer != null) {
                    serializer.release();
                }
                return ResponseBuilder.inst().cancel().flag().terminate().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.D";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadD.class);
        }
    }

    public static class PayloadE implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String channel = packet.getChannel();
                if (channel.equals("BungeeCord") || channel.contains("FML") || channel.equals("ML|OpenTE") || channel.equals("WECUI")) {
                    return ResponseBuilder.SUCCESS; // ingen grund til at tjekke for Community channels
                }
                ByteBuf serializer = container.get(ByteBuf.class, 0);
                if (channel.equalsIgnoreCase("register")) {

                    if (player.getListeningPluginChannels().size() > ConfigurationOptions.CustomPayload.PayloadE.MAX) {
                        serializer.release();
                        return ResponseBuilder.inst().cancel().flag().terminate().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.E";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadE.class);
        }
    }

    public static class PayloadF implements PacketCheck<WrappedPlayInCustomPayload>, ViolationKeyedCheck<WrappedPlayInCustomPayload, String> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) throws Exception {
            if (ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadF.class).isEnabled()) {

                if (ConfigurationOptions.getChannelFloodSettings().containsKey(packet.getChannel())) {
                    return ResponseBuilder.inst().flag().withKey(new ViolationService.CheckKey(this, packet.getChannel())).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.F";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting(String key) {
            return ConfigurationOptions.getChannelFloodSettings().get(key);
        }

        @Override
        public String getKey(WrappedPlayInCustomPayload packet) {
            return packet.getChannel();
        }
    }

    public static class PayloadG implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                String channel = packet.getChannel();
                if (channel.equals("WDL|INIT") || channel.equals("WDL|CONTROL") || channel.equals("WDL|REQUEST")) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Payload.G";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.CustomPayload.PayloadG.class);
        }
    }

}
