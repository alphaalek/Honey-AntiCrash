package me.alek.honey.validation.impl;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInTabComplete;
import me.alek.honey.netty.packetwrappers.impl.play.server.WrappedPlayOutTabComplete;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.*;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;

import java.util.*;

public class TabValidations extends AbstractValidationPacketListener<WrappedPlayInTabComplete> {

    public TabValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.TAB_COMPLETE);
    }

    public ValidationPipeline<PacketValidation<WrappedPlayInTabComplete>> createValidationPipeline() {
        return ValidationPipeline.from(new TabA(), new TabC());
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInTabComplete> packet, ValidationPipelineContext context) {
        return packet.getAnyWrapped().getMessage().startsWith("/");
    }

    @Override
    public void load() {
        TabA.prohibitedCommands = CommandValidations.getRegisteredCommands(ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS);

        if (TabC.labelSetting != null) {
            TabC.labelSetting.threshold = ConfigurationOptions.TabComplete.TabC.LABEL_THRESHOLD;
        }
    }

    public static class TabA implements PacketValidation<WrappedPlayInTabComplete>, ViolationValidation<WrappedPlayInTabComplete> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static String[] prohibitedCommands;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInTabComplete packet, ValidationPipelineContext context) {
            String message = packet.getMessage();
            String label = message.split(" ")[0];

            for (String prohibitedCommand : prohibitedCommands) {
                if (CommandValidations.match(prohibitedCommand, label)) {
                    return ValidationViolationContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                String messageFormatted = "\"" + (message.length() > 25 ? message.substring(0, 25) + "<...>" : message) + "\"";
                                context.getDevelopmentData(key).addPoint("Message", messageFormatted);
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
            return "Tab.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.tab.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.TabComplete.TabA.class);
        }
    }

    public static class ClientboundTabValidations extends AbstractValidationPacketListener<WrappedPlayOutTabComplete> implements PacketValidation<WrappedPlayOutTabComplete>, ViolationValidation<WrappedPlayOutTabComplete> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        public ClientboundTabValidations(PacketLibrary library) {
            super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
        }

        private static List<PacketType> getPacketTypes() {
            return Collections.singletonList(PacketTypes.Play.Server.TAB_COMPLETE);
        }

        @Override
        public ValidationPipeline<PacketValidation<WrappedPlayOutTabComplete>> createValidationPipeline() {
            return ValidationPipeline.from(this);
        }

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayOutTabComplete packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(packet.getSuggestionsSize()).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Tab.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.tab.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.TabComplete.TabB.class);
        }
    }

    public static class TabC implements PacketValidation<WrappedPlayInTabComplete>, ViolationValidation<WrappedPlayInTabComplete> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static ConfigurationOptions.ViolationSetting labelSetting;
        private static final HoneyKey labelKey = HoneyKey.of(0, "total");
        private static final HoneyKey totalKey = HoneyKey.of(1, "label");

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInTabComplete packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (labelSetting == null) {
                    if (!player.violationContext.getViolationDistributors().has(getID())) {
                        player.violationContext.getDistributor(generateKey(), getSetting());
                    }

                    labelSetting = getSetting().clone();
                    labelSetting.threshold = ConfigurationOptions.TabComplete.TabC.LABEL_THRESHOLD;

                    player.violationContext.setSetting(HoneyKey.of(this), labelKey, labelSetting);
                }

                int label = packet.getMessage().indexOf(" ");
                int total = packet.getMessage().length();
                return ValidationViolationContext.Builder.validate().add(
                        ValidationViolationContext.Builder.validate().flag(label == -1 ? total : label).withViolationKey(labelKey).build(),
                        ValidationViolationContext.Builder.validate().flag(total).withViolationKey(totalKey).build()
                ).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Tab.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.tab.c");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.TabComplete.TabC.class);
        }
    }
}
