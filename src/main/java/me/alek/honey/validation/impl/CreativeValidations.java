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
import me.alek.honey.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.util.IDRegistry;
import org.bukkit.GameMode;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CreativeValidations extends AbstractPacketListener<WrappedPlayInSetCreativeSlot> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInSetCreativeSlot>> getNewPipeline() {
        return ValidationPipeline.from(new CreativeA(), new CreativeB(), new CreativeC());
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.SET_CREATIVE_SLOT);
    }

    public static class CreativeA implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                if (player.player.getGameMode() != GameMode.CREATIVE) {
                    return Validation.validate().cancel().flag().build();
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
            return "Creative.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.creative.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeA.class);
        }
    }

    public static class CreativeB implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {
                int slot = packet.getSlot();
                if (slot > (player.player.getOpenInventory().countSlots() + 3) || (slot < -1 && slot != -999)) {
                    return Validation.validate().cancel().flag()
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
            return "Creative.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.creative.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeB.class);
        }
    }

    public static class CreativeC implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                return ItemAnalysis.flagIf(player, packet.getItem(), container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Creative.C (IA)";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.creative.c");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeC.class);
        }
    }
}
