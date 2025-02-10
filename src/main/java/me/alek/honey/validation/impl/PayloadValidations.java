package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;

import java.util.Collections;
import java.util.List;

public class PayloadValidations extends AbstractValidationPacketListener<WrappedPlayInCustomPayload> {

    public PayloadValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.INITIAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.CUSTOM_PAYLOAD);
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInCustomPayload> packet, ValidationPipelineContext context) {
        String channel = packet.getCurrentWrapped().getPayloadData().getChannel();

        context.getCallback().addAction(Callback.Action.<ViolationKey, ValidationViolationContext>from((keyOuter, response) -> {
            response.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                if (onViolation != null) onViolation.run();

                context.getDevelopmentData(keyInner).addPoint("Channel", channel);
            });
        }));

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInCustomPayload>> createValidationPipeline() {
        return ValidationPipeline.from(new PayloadA(), new PayloadB(), new PayloadC(), new PayloadD());
    }

    public static class PayloadA implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInCustomPayload packet, ValidationPipelineContext context) {
            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
            return Settings.getSetting(this, ConfigurationOptions.Payload.PayloadA.class);
        }
    }

    public static class PayloadB implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInCustomPayload packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(packet.getPayloadData().getSerializer().readableBytes()).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
            return Settings.getSetting(this, ConfigurationOptions.Payload.PayloadB.class);
        }
    }

    public static class PayloadC implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInCustomPayload packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && packet.getPayloadData().isBookChannel()) {
                WrappedImmutableItemStack item = packet.getPayloadData().getBookChannelItemStack();
                if (item != null) {
                    return ItemAnalysis.flagIf(player, item, generateKey(), context);
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
            return Settings.getSetting(this, ConfigurationOptions.Payload.PayloadC.class);
        }
    }

    public static class PayloadD implements PacketValidation<WrappedPlayInCustomPayload>, ViolationValidation<WrappedPlayInCustomPayload> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInCustomPayload packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                String channel = packet.getPayloadData().getChannel();
                if (channel.equalsIgnoreCase("register") &&
                        player.bukkit.getListeningPluginChannels().size() > getSetting().max) {
                    return ValidationViolationContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                context.getDevelopmentData(key).addPoint("Plugin Channels", player.bukkit.getListeningPluginChannels().size());
                            })
                            .build();
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
            return Settings.getSetting(this, ConfigurationOptions.Payload.PayloadD.class);
        }
    }
}
