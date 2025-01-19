package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInPickItem;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.*;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;

import java.util.Collections;
import java.util.List;

public class PickItemValidations extends AbstractValidationPacketListener<WrappedPlayInPickItem> {

    public PickItemValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.FINAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.PICK_ITEM);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInPickItem>> createValidationPipeline() {
        return ValidationPipeline.from(new PickItemA());
    }

    private static class PickItemA implements PacketValidation<WrappedPlayInPickItem>, ViolationValidation<WrappedPlayInPickItem> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInPickItem packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && (packet.getHotBarSlot() < 0 || packet.getHotBarSlot() > 8)) {
                return ValidationViolationContext.FLAG.builder()
                        .onViolation(generateKey(), (innerKey, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            context.getDevelopmentData(innerKey).addPoint("Slot", packet.getHotBarSlot());
                        }).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "PickItem.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.pick-item.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PickItem.PickItemA.class);
        }
    }
}
