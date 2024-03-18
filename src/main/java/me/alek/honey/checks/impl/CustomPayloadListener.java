package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.checks.types.KeyedViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.utility.*;
import me.alek.honey.services.ViolationService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CustomPayloadListener extends AbstractPacketListener<WrappedPlayInCustomPayload> implements IPacketListener {

    private final Class<?> PACKET_SERIALIZER_CLASS = Reflection.getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    private final ConstructorInvoker<?> PACKET_SERIALIZER_CONSTRUCTOR = Reflection.getConstructor(PACKET_SERIALIZER_CLASS, ByteBuf.class);
    private final MethodInvoker<?> ITEMSTACK_SERIALIZER_INVOKER = Reflection.getMethodFromReturnType(PACKET_SERIALIZER_CLASS, 0, ItemReflection.getNMSItemStackClass());

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInCustomPayload> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        ByteBuf serializer;
        try {
            serializer = (ByteBuf) packetContainer.getPacket().getDataSerializer().getHandle();
        } catch (Exception ex) {
            return false;
        }
        String channel = packetContainer.getPacket().getChannel();
        if (channel.equals("MC|BEdit") || channel.equals("MC|BOpen") || channel.equals("MC|BSign")) {

            ByteBuf copiedSerializer = (ByteBuf) PACKET_SERIALIZER_CONSTRUCTOR.invoke(Unpooled.copiedBuffer(serializer));
            try {
                Object item = ITEMSTACK_SERIALIZER_INVOKER.invokeWithException(copiedSerializer);
                ItemStack bukkitItem = ItemReflection.asBukkitItem(item);

                container.getStorage().put(ItemStack.class, bukkitItem);
            } catch (Exception ex) {
            }
        }

        container.getStorage().put(ByteBuf.class, serializer);

        callback.addAction(response -> {
            response.builder().onViolation(onViolation -> {
                if (onViolation != null) onViolation.run();

                container.getDefaultDevelopmentData().addPoint("Channel", channel);
                container.getDefaultDevelopmentData().addPoint("ByteBuf", serializer.readableBytes() + " bytes");
            });
        });

        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInCustomPayload> packetContainer, CheckContextContainer container) {
        if (packetContainer.isCancelled()) {
            ByteBuf byteBuf = container.getStorage().get(ByteBuf.class, 0);
            if (byteBuf != null)
                byteBuf.release();
        }
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInCustomPayload> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedPlayInCustomPayload> visitor = new CheckVisitor<>(player);
        visitor.visit(new PayloadA());
        visitor.visit(new PayloadB());
        visitor.visit(new PayloadC());
        visitor.visit(new PayloadD());
        visitor.visit(new PayloadE());
        visitor.visit(new PayloadF());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CUSTOM_PAYLOAD);
    }

    public static class PayloadA implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ByteBuf serializer = container.getStorage().get(ByteBuf.class, 0);
                if (serializer == null || serializer.getClass() == EmptyByteBuf.class) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 11;
        }

        @Override
        public String getName() {
            return "Payload.A";
        }

        @Override
        public String getDescription() {
            return "Empty serializer for payload";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CUSTOM_PAYLOAD_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadA.class);
        }
    }

    public static class PayloadB implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {
        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ByteBuf serializer = container.getStorage().get(ByteBuf.class,0);
                if (serializer == null) {
                    return ResponseBuilder.SUCCESS;
                }
                return ResponseBuilder.inst().flag(serializer.readableBytes()).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 12;
        }

        @Override
        public String getName() {
            return "Payload.B";
        }

        @Override
        public String getDescription() {
            return "Too big serializer for payload";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CUSTOM_PAYLOAD_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadB.class);
        }
    }

    public static class PayloadC implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = container.getStorage().get(ItemStack.class, 0);
                if (item != null) {
                    CheckContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);

                    NBTCompound nbt = NBTFactory.fromItem(item);
                    if (ItemAnalysis.checkItem(player, item, nbt, developmentData)) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }

                    int nbtSize;
                    if (nbt != null && (nbtSize = nbt.toString().getBytes().length) > getSetting().max) {
                        return ResponseBuilder.inst().cancel().flag()
                                .onViolation(onViolation -> {
                                    if (onViolation != null) onViolation.run();

                                    container.getDevelopmentData(this).addPoint("NBT Size", nbtSize);
                                })
                                .build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 13;
        }

        @Override
        public String getName() {
            return "Payload.C (IA)";
        }

        @Override
        public String getDescription() {
            return "Illegal item in payload";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CUSTOM_PAYLOAD_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadC.class);
        }
    }

    public static class PayloadD implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String channel = packet.getChannel();
                ItemStack itemInHand = player.player.getItemInHand();

                if (!channel.isEmpty()) {
                    if (!channel.contains("BSign") && !channel.contains("BEdit") && !channel.contains("BOpen")) {
                        return ResponseBuilder.SUCCESS;
                    }
                    if (itemInHand.getType() == MaterialMappings.getItem("writable-book") || itemInHand.getType() == Material.WRITTEN_BOOK) {
                        return ResponseBuilder.SUCCESS;
                    }
                }
                return ResponseBuilder.inst().cancel().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 14;
        }

        @Override
        public String getName() {
            return "Payload.D";
        }

        @Override
        public String getDescription() {
            return "Illegal usage of book channel";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CUSTOM_PAYLOAD_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadD.class);
        }
    }

    public static class PayloadE implements PacketCheck<WrappedPlayInCustomPayload>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String channel = packet.getChannel();
                if (channel.equalsIgnoreCase("register") &&
                        player.player.getListeningPluginChannels().size() > getSetting().max) {

                    return ResponseBuilder.inst().cancel().flag()
                            .onViolation(onViolation -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Channel Size", player.player.getListeningPluginChannels().size());
                            })
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 15;
        }

        @Override
        public String getName() {
            return "Payload.E";
        }

        @Override
        public String getDescription() {
            return "Too many registered channels";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CUSTOM_PAYLOAD_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadE.class);
        }
    }

    public static class PayloadF implements PacketCheck<WrappedPlayInCustomPayload>, KeyedViolationCheck<WrappedPlayInCustomPayload, String> {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) throws Exception {
            ConfigurationOptions.VlSetting setting = getSetting(packet);
            if (setting != null && setting.isEnabled()) {

                if (ConfigurationOptions.getChannelFloodSettings().containsKey(packet.getChannel())) {
                    return ResponseBuilder.inst().flag().withLimitsKey(new ViolationService.CheckKey(this, packet.getChannel())).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 16;
        }

        @Override
        public String getName() {
            return "Payload.F";
        }

        @Override
        public String getDescription() {
            return "Packet flood for payload channels";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CUSTOM_PAYLOAD_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting(WrappedPlayInCustomPayload packet) {
            return ConfigurationOptions.getChannelFloodSettings().get(packet.getChannel());
        }

        @Override
        public String getKey(WrappedPlayInCustomPayload packet) {
            return packet.getChannel();
        }
    }
}
