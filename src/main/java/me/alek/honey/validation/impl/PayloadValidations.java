package me.alek.honey.validation.impl;

import io.netty.buffer.EmptyByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class PayloadValidations<CP extends WrappedPlayInCustomPayload<CP>> extends AbstractValidationPacketListener<CP> {

    public PayloadValidations(PacketLibrary library) {
        super(library, PacketContainer.State.BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.CUSTOM_PAYLOAD);
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<CP> packet, ValidationPipelineContext context, Callback callback) {
        String channel = packet.getAnyWrapped().getData().getChannel();

        callback.addAction(Callback.Action.<ViolationKey, ValidationServiceContext>from((keyOuter, response) -> {
            response.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                if (onViolation != null) onViolation.run();

                context.getDevelopmentData(keyInner).addPoint("Channel", channel);
            });
        }));

        return true;
    }

    @Override
    public ValidationPipeline<PacketValidation<CP>> getNewPipeline() {
        return ValidationPipeline.from(new PayloadA<>(), new PayloadB<>(), new PayloadC<>(), new PayloadD<>(), new PayloadE<>());
    }

    public static class PayloadA<CP extends WrappedPlayInCustomPayload<CP>> implements PacketValidation<CP>, ViolationValidation<CP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, CP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && packet.getData().getSerializer().getBuf() instanceof EmptyByteBuf) {
                return ValidationServiceContext.FLAG;
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadA.class);
        }
    }

    public static class PayloadB<CP extends WrappedPlayInCustomPayload<CP>> implements PacketValidation<CP>, ViolationValidation<CP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, CP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(packet.getData().getSerializer().readableBytes()).build();
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadB.class);
        }
    }

    public static class PayloadC<CP extends WrappedPlayInCustomPayload<CP>> implements PacketValidation<CP>, ViolationValidation<CP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, CP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                ItemStack item = packet.getData().getBookChannelItemStack();
                if (item != null) {
                    NBTCompound nbt = NBTFactory.fromItem(item, false, false);
                    Callback callback = new Callback();
                    if (ItemAnalysis.checkItem(player, item, nbt, callback)) {
                        return ValidationServiceContext.Builder.validate().cancel().flag()
                                .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                    if (onViolation != null) onViolation.run();

                                    callback.call(context.getDevelopmentData(key));
                                })
                                .build();
                    }

                    int nbtSize;
                    if (nbt != null && (nbtSize = nbt.toString().getBytes().length) > getSetting().max) {
                        return ValidationServiceContext.Builder.validate().cancel().flag()
                                .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                    if (onViolation != null) onViolation.run();

                                    context.getDevelopmentData(key).addPoint("NBT Size", nbtSize);
                                })
                                .build();
                    }
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadC.class);
        }
    }

    public static class PayloadD<CP extends WrappedPlayInCustomPayload<CP>> implements PacketValidation<CP>, ViolationValidation<CP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, CP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && packet.getData().isBookChannel()) {
                if (packet.getData().getBookChannelItemStack() == null) {
                    return ValidationServiceContext.FLAG;
                }

                ItemStack itemInHand = player.bukkit.getItemInHand();
                if (itemInHand.getType() != MaterialMappings.getType("writable-book")
                        && itemInHand.getType() != Material.WRITTEN_BOOK) {
                    return ValidationServiceContext.FLAG;
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadD.class);
        }
    }

    public static class PayloadE<CP extends WrappedPlayInCustomPayload<CP>> implements PacketValidation<CP>, ViolationValidation<CP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, CP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                String channel = packet.getData().getChannel();
                if (channel.equalsIgnoreCase("register") &&
                        player.bukkit.getListeningPluginChannels().size() > getSetting().max) {
                    return ValidationServiceContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                context.getDevelopmentData(key).addPoint("Plugin Channels", player.bukkit.getListeningPluginChannels().size());
                            })
                            .build();
                }
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.CustomPayload.PayloadE.class);
        }
    }
}
