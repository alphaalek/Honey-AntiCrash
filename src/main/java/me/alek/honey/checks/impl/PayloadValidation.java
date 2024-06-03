package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.util.*;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class PayloadValidation extends AbstractPacketListener<WrappedPlayInCustomPayload> implements IPacketListener {

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
                ItemStack bukkitItem = ItemReflection.asBukkitCopy(item);

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
            if (byteBuf != null) {
                byteBuf.release();
            }
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
                    return Validation.inst().cancel().flag().build();
                }
            }
            return Validation.PASS;
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
            return Lang.get("validation.payload.a");
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
                    return Validation.PASS;
                }
                return Validation.inst().flag(serializer.readableBytes()).build();
            }
            return Validation.PASS;
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
            return Lang.get("validation.payload.b");
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

                    NBTCompound nbt = NBTFactory.fromItem(item, false, false);
                    CallbackAction<CheckContextContainer.DevelopmentData> callback = new CallbackAction<>();
                    if (ItemAnalysis.checkItem(player, item, nbt, callback)) {
                        return Validation.inst().cancel().flag().onViolation(onViolation -> {
                            if (onViolation != null) onViolation.run();

                            CheckContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);
                            callback.call(developmentData);
                        }).build();
                    }

                    int nbtSize;
                    if (nbt != null && (nbtSize = nbt.toString().getBytes().length) > getSetting().max) {
                        return Validation.inst().cancel().flag()
                                .onViolation(onViolation -> {
                                    if (onViolation != null) onViolation.run();

                                    container.getDevelopmentData(this).addPoint("NBT Size", nbtSize);
                                })
                                .build();
                    }
                }
            }
            return Validation.PASS;
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
            return Lang.get("validation.payload.c");
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
                        return Validation.PASS;
                    }
                    if (itemInHand.getType() == MaterialMappings.getType("writable-book") || itemInHand.getType() == Material.WRITTEN_BOOK) {
                        return Validation.PASS;
                    }
                }
                return Validation.inst().cancel().flag().build();
            }
            return Validation.PASS;
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
            return Lang.get("validation.payload.d");
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

                    return Validation.inst().cancel().flag()
                            .onViolation(onViolation -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Channel Size", player.player.getListeningPluginChannels().size());
                            })
                            .build();
                }
            }
            return Validation.PASS;
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
            return Lang.get("validation.payload.e");
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
}
