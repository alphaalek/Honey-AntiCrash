package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.ResponseContainer;
import me.alek.honey.validation.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class HeldItemValidations extends AbstractPacketListener<WrappedPlayInHeldItemSlot> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInHeldItemSlot>> getNewPipeline() {
        return ValidationPipeline.from(new HeldItemA(), new HeldItemB());
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.HELD_ITEM_SLOT);
    }

    public static class HeldItemA implements PacketValidation<WrappedPlayInHeldItemSlot>, ViolationValidation<WrappedPlayInHeldItemSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInHeldItemSlot packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                int slot = packet.getSlot();
                if (slot >= 36 || slot < 0) {
                    return Validation.validate().cancel().flag().terminate()
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Slot", slot);
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
            return "HeldItem.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.held.item.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.HeldItem.HeldItemA.class);
        }
    }

    public static class HeldItemB implements PacketValidation<WrappedPlayInHeldItemSlot>, ViolationValidation<WrappedPlayInHeldItemSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInHeldItemSlot packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, player.player.getInventory().getItem(packet.getSlot()), container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "HeldItem.B (IA)";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.held.item.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.HeldItem.HeldItemB.class);
        }
    }
}
