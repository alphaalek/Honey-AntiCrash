package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.util.IDRegistry;
import org.bukkit.GameMode;

import java.util.Collections;
import java.util.List;

public class CreativeValidations extends AbstractPacketListener<WrappedPlayInSetCreativeSlot> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInSetCreativeSlot>> getNewPipeline() {
        return ValidationPipeline.from(new CreativeA(), new CreativeB(), new CreativeC());
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION);
    }

    public static class CreativeA implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (player.bukkit.getGameMode() != GameMode.CREATIVE) {
                    return ValidationServiceContext.Builder.validate().cancel().flag().build();
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
            return "Creative.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.creative.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeA.class);
        }
    }

    public static class CreativeB implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                int slot = packet.getSlot();
                if (slot > (player.bukkit.getOpenInventory().countSlots() + 3) || (slot < -1 && slot != -999)) {
                    return ValidationServiceContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                context.getDevelopmentData(key).addPoint("Slot", slot);
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
            return "Creative.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.creative.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeB.class);
        }
    }

    public static class CreativeC implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, packet.getItem(), ViolationKey.of(this), context);
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeC.class);
        }
    }
}
