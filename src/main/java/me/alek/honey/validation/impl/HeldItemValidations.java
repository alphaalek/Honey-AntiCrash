package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;

public class HeldItemValidations extends AbstractValidationPacketListener<WrappedPlayInHeldItemSlot> {

    public HeldItemValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.HELD_ITEM_CHANGE);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInHeldItemSlot>> getNewPipeline() {
        return ValidationPipeline.from(new HeldItemA(), new HeldItemB());
    }

    public static class HeldItemA implements PacketValidation<WrappedPlayInHeldItemSlot>, ViolationValidation<WrappedPlayInHeldItemSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInHeldItemSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                int slot = packet.getSlot();
                if (slot >= 36 || slot < 0) {
                    return ValidationServiceContext.Builder.validate().cancel().flag().terminate()
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
            return "HeldItem.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.held.item.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.HeldItem.HeldItemA.class);
        }
    }

    public static class HeldItemB implements PacketValidation<WrappedPlayInHeldItemSlot>, ViolationValidation<WrappedPlayInHeldItemSlot> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInHeldItemSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, player.bukkit.getInventory().getItem(packet.getSlot()), ViolationKey.of(this), context);
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.HeldItem.HeldItemB.class);
        }
    }
}
