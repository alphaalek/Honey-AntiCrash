package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.response.ResponseContainer;
import me.alek.honey.validation.response.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInWindowClick;
import me.alek.honey.util.IDRegistry;
import org.bukkit.event.inventory.InventoryType;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class WindowValidations extends AbstractPacketListener<WrappedPlayInWindowClick> {

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

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Play.Client.WINDOW_CLICK);
    }

    public static class WindowA implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {
                int slot = packet.getWindowSlot();

                int inventorySlots = player.player.getOpenInventory().countSlots();
                if (player.player.getOpenInventory() != null && (player.player.getOpenInventory().getType() == InventoryType.CRAFTING && player.player.getInventory().getType() == InventoryType.PLAYER)) {
                    inventorySlots += 4; // armor slots
                }
                if (player.player.getOpenInventory() == null || slot > (inventorySlots - 1) || (slot < -1 && slot != -999)) {
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
            return "Window.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowA.class);
        }
    }

    public static class WindowB implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationContextContainer container) {
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
            return "Window.B (IA)";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowB.class);
        }
    }

    public static class WindowC implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {
                if (player.player.getOpenInventory().getType().name().equals("LECTERN")
                        && packet.getMode() == 1) {
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
            return "Window.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.c");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowC.class);
        }
    }

    public static class WindowD implements PacketValidation<WrappedPlayInWindowClick>, ViolationValidation<WrappedPlayInWindowClick> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInWindowClick packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                if (packet.getMode() < 0 || packet.getActionNumber() < 0 || packet.getWindowButton() < 0 || packet.getWindowId() < 0) {
                    HoneyPlugin.get().getLoggerService().getLogger().info("DEBUG: Mode: " + packet.getMode() + " , action: " + packet.getActionNumber() + ", id: " + packet.getWindowId() + ", button: " + packet.getWindowButton() + ", slot: " + packet.getWindowSlot());
                    return Validation.validate().flag().build();
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
            return "Window.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.window.d");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowD.class);
        }
    }
}
