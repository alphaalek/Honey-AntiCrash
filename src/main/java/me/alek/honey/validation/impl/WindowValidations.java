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
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInWindowClick;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;

public class WindowValidations extends AbstractValidationPacketListener<WrappedPlayInWindowClick> {

    public WindowValidations(PacketLibrary library) {
        super(library, PacketContainer.State.INITIAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.CLICK_WINDOW);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInWindowClick>> getNewPipeline() {
        ValidationPipeline<PacketValidation<WrappedPlayInWindowClick>> visitor = new ValidationPipeline<>();
        visitor.register(new WindowA());
        visitor.register(new WindowB());
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
            visitor.register(new WindowC());
        }
        visitor.register(new WindowD());
        return visitor;
    }

    public static class WindowA implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                // TODO: fix
                /*
                int slot = packet.getWindowSlot();

                InventoryProcessor.InventoryEmulator ctx = Processors.INVENTORY.getContext(player);

                if (!ctx.windowOpen) {
                    return ValidationServiceContext.FLAG;
                }


                if (player.bukkit.getOpenInventory() != null && (player.bukkit.getOpenInventory().getType() == InventoryType.CRAFTING && player.bukkit.getInventory().getType() == InventoryType.PLAYER)) {
                    inventorySlots += 4; // armor slots
                }
                if (player.bukkit.getOpenInventory() == null || slot > (inventorySlots - 1) || (slot < -1 && slot != -999)) {
                    return ValidationServiceContext.Builder.validate().cancel().flag()
                            .onViolation(ViolationKey.of(this), (key, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                context.getDevelopmentData(key).addPoint("Slot", slot);
                            })
                            .build();
                }
                */
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Window.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowA.class);
        }
    }

    public static class WindowB implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, packet.getCarriedItemStack(), ViolationKey.of(this), context);
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Window.B (IA)";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowB.class);
        }
    }

    public static class WindowC implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (player.bukkit.getOpenInventory().getType().name().equals("LECTERN")
                        && packet.getWindowClickType() == WrappedPlayInWindowClick.WindowClickType.QUICK_MOVE) {
                    return ValidationServiceContext.FLAG;
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
            return "Window.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.c");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowC.class);
        }
    }

    public static class WindowD implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (packet.getActionNumber() < 0
                        || packet.getStateId() < 0
                        || packet.getWindowId() < 0) {
                    return ValidationServiceContext.FLAG;
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
            return "Window.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.d");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowD.class);
        }
    }
}
