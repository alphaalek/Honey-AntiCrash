package me.alek.honey.checks.impl;

import io.netty.buffer.EmptyByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketValidation;
import me.alek.honey.checks.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.util.MaterialMappings;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class PayloadValidation extends AbstractPacketListener<WrappedPlayInCustomPayload> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInCustomPayload> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        String channel = packetContainer.getPacket().getChannel();

        callback.addAction(response -> {
            response.builder().onViolation((check, onViolation) -> {
                if (onViolation != null) onViolation.run();

                container.getDevelopmentData(check).addPoint("Channel", channel);
                container.getDevelopmentData(check).addPoint("Byte Buffer", packetContainer.getPacket().getBuf().readableBytes() + " bytes");
            });
        });

        return true;
    }

    @Override
    public CheckPipeline<PacketValidation<WrappedPlayInCustomPayload>> getPipeline(HoneyPlayer player) {
        return CheckPipeline.from(player,
                new PayloadA(), new PayloadB(), new PayloadC(), new PayloadD(), new PayloadE());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Play.Client.CUSTOM_PAYLOAD);
    }

    public static class PayloadA implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                if (packet.getBuf().getClass().equals(EmptyByteBuf.class)) {
                    return Validation.inst().cancel().flag().build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadA.class);
        }
    }

    public static class PayloadB implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                return Validation.inst().flag(packet.getBuf().readableBytes()).build();
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadB.class);
        }
    }

    public static class PayloadC implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = packet.getBookChannelItemStack();
                if (item != null) {
                    NBTCompound nbt = NBTFactory.fromItem(item, false, false);
                    CallbackAction<CheckContextContainer.DevelopmentData> callback = new CallbackAction<>();
                    if (ItemAnalysis.checkItem(player, item, nbt, callback)) {
                        return Validation.inst().cancel().flag().onViolation((check, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            callback.call(container.getDevelopmentData(this));
                        }).build();
                    }

                    int nbtSize;
                    if (nbt != null && (nbtSize = nbt.toString().getBytes().length) > getSetting().max) {
                        return Validation.inst().cancel().flag()
                                .onViolation((check, onViolation) -> {
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
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadC.class);
        }
    }

    public static class PayloadD implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                String channel = packet.getChannel();
                if (channel.equals("MC|BSign")
                        || channel.equals("MC|BEdit")) {

                    ItemStack itemInHand = player.player.getItemInHand();
                    if (itemInHand.getType() == MaterialMappings.getType("writable-book")
                            || itemInHand.getType() == Material.WRITTEN_BOOK) {
                        return Validation.inst().cancel().flag().build();
                    }
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadD.class);
        }
    }

    public static class PayloadE implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String channel = packet.getChannel();
                if (channel.equalsIgnoreCase("register") &&
                        player.player.getListeningPluginChannels().size() > getSetting().max) {
                    return Validation.inst().cancel().flag()
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Plugin Channels", player.player.getListeningPluginChannels().size());
                            })
                            .build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadE.class);
        }
    }
}
