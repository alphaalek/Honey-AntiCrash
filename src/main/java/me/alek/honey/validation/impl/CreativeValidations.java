package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.ValidationViolationContext;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.util.IDRegistry;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CreativeValidations extends AbstractValidationPacketListener<WrappedPlayInSetCreativeSlot> {

    public CreativeValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.INITIAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInSetCreativeSlot>> createValidationPipeline() {
        return ValidationPipeline.from(new CreativeA(), new CreativeB(), new CreativeC());
    }

    public static class CreativeA implements PacketValidation<WrappedPlayInSetCreativeSlot>, ViolationValidation<WrappedPlayInSetCreativeSlot> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (player.bukkit.getGameMode() != GameMode.CREATIVE) {
                    return ValidationViolationContext.Builder.validate().cancel().flag().build();
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

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && (packet.getSlot() > 45 || packet.getSlot() < -1)) {
                // TODO: check if slot 0-4 can be used (crafting input + output)
                return ValidationViolationContext.FLAG.builder()
                        .onViolation(generateKey(), (innerKey, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            context.getDevelopmentData(innerKey).addPoint("Slot", packet.getSlot());
                        }).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, packet.getItemStack(), generateKey(), context)
                        .builder().onViolation(generateKey(), (keyInner, onViolation) -> {
                            if (onViolation != null) {
                                onViolation.run();
                            }
                            if (player.bukkit.getGameMode() == GameMode.CREATIVE) {
                                try {
                                    ItemStack item = player.bukkit.getInventory().getItem(packet.getSlot());
                                    if (item != null && packet.getItemStack().isSimilar(item)) {
                                        item.setType(Material.AIR);
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
