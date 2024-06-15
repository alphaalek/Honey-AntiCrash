package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInChat;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ChatValidation extends AbstractPacketListener<WrappedPlayInChat> implements IPacketListener {

    @Override
    public CheckVisitor<WrappedPlayInChat> getVisitor(HoneyPlayer player) {
        return CheckVisitor.from(player,
                new ChatA(), new ChatB());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Play.Client.CHAT);
    }

    public static class ChatA implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInChat packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String message = packet.getMessage();
                if (message.isEmpty()) {
                    return Validation.inst().cancel().flag().terminate().build();
                }
                for (char character : message.toCharArray()) {
                    if (character == Character.MIN_VALUE) {
                        return Validation.inst().cancel().flag().terminate().build();
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
            return "Chat.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.chat.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Chat.ChatA.class);
        }
    }

    public static class ChatB implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInChat packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                int messageLength = packet.getMessage().length();

                if (messageLength > (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_11) ? 100 : 256)) {
                    return Validation.inst().cancel().flag()
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Message length", messageLength);
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
            return "Chat.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.chat.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Chat.ChatB.class);
        }
    }
}
