package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;

public class ChatValidations extends AbstractValidationPacketListener<WrappedPlayInChat> {

    public ChatValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.CHAT_MESSAGE);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInChat>> createValidationPipeline() {
        return ValidationPipeline.from(new ChatA(), new ChatB());
    }

    public static class ChatA implements PacketValidation<WrappedPlayInChat>, ViolationValidation<WrappedPlayInChat> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInChat packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                String message = packet.getMessage();
                if (message.isEmpty()) {
                    return ValidationServiceContext.Builder.validate().cancel().flag().terminate().build();
                }

                for (char character : message.toCharArray()) {
                    if (character == Character.MIN_VALUE) {
                        return ValidationServiceContext.Builder.validate().cancel().flag().terminate().build();
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
            return "Chat.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.chat.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Chat.ChatA.class);
        }
    }

    public static class ChatB implements PacketValidation<WrappedPlayInChat>, ViolationValidation<WrappedPlayInChat> {

        private static final int MAX = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_11) ? 100 : 256;
        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInChat packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                int length = packet.getMessage().length();
                if (length > MAX) {
                    return ValidationServiceContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                context.getDevelopmentData(key).addPoint("Message length", length);
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
            return "Chat.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.chat.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Chat.ChatB.class);
        }
    }
}
