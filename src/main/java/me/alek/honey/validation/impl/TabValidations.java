package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInTabComplete;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutTabComplete;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.*;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;

import java.util.*;

public class TabValidations extends AbstractPacketListener<WrappedPlayInTabComplete> {


    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInTabComplete> packet, ValidationPipelineContext context, Callback callback) {
        return packet.getAnyWrapped().getMessage().startsWith("/");
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInTabComplete>> getNewPipeline() {
        return ValidationPipeline.from(new TabA(), new TabC());
    }

    @Override
    public void load() {
        TabA.prohibitedCommands = CommandValidations.getRegisteredCommands(ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS);

        if (TabC.labelSetting != null) {
            TabC.labelSetting.threshold = ConfigurationOptions.TabComplete.TabC.LABEL_THRESHOLD;
        }
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.TAB_COMPLETE);
    }

    public static class TabA implements PacketValidation<WrappedPlayInTabComplete>, ViolationValidation<WrappedPlayInTabComplete> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static String[] prohibitedCommands;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInTabComplete packet, ValidationPipelineContext context) {
            String message = packet.getMessage();
            String label = message.split(" ")[0];

            for (String prohibitedCommand : prohibitedCommands) {
                if (CommandValidations.match(prohibitedCommand, label)) {
                    return ValidationServiceContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                String messageFormatted = "\"" + (message.length() > 25 ? message.substring(0, 25) + "<...>" : message) + "\"";
                                context.getDevelopmentData(key).addPoint("Message", messageFormatted);
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

    public static class ClientboundTabValidations extends AbstractPacketListener<WrappedPlayOutTabComplete> implements PacketValidation<WrappedPlayOutTabComplete>, ViolationValidation<WrappedPlayOutTabComplete> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public List<PacketType> getPacketTypes() {
            return Collections.singletonList(PacketTypes.Play.Server.TAB_COMPLETE);
        }

        @Override
        public ValidationPipeline<PacketValidation<WrappedPlayOutTabComplete>> getNewPipeline() {
            return ValidationPipeline.from(this);
        }

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayOutTabComplete packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(packet.getSuggestionsSize()).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
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

        private static final int id = IDRegistry.CHECKS.acquire();
        private static ConfigurationOptions.ViolationSetting labelSetting;
        private static final ViolationService.HoneyKey labelKey = ViolationService.HoneyKey.of(0, "total");
        private static final ViolationService.HoneyKey totalKey = ViolationService.HoneyKey.of(1, "label");

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInTabComplete packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (labelSetting == null) {
                    if (!player.violationContext.getViolationDistributors().has(getID())) {
                        player.violationContext.getDistributor(ViolationService.HoneyKey.of(this), getSetting());
                    }

                    labelSetting = getSetting().clone();
                    labelSetting.threshold = ConfigurationOptions.TabComplete.TabC.LABEL_THRESHOLD;

                    player.violationContext.setSetting(ViolationService.HoneyKey.of(this), labelKey, labelSetting);
                }

                int label = packet.getMessage().indexOf(" ");
                int total = packet.getMessage().length();
                return ValidationServiceContext.Builder.validate().add(
                        ValidationServiceContext.Builder.validate().flag(label == -1 ? total : label).withViolationKey(labelKey).build(),
                        ValidationServiceContext.Builder.validate().flag(total).withViolationKey(totalKey).build()
                ).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
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
