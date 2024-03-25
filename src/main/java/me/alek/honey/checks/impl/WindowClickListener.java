package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInWindowClick;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.utility.CallbackAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class WindowClickListener extends AbstractPacketListener<WrappedPlayInWindowClick> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInWindowClick> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInWindowClick> packetContainer, CheckContextContainer container) {
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInWindowClick> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedPlayInWindowClick> visitor = new CheckVisitor<>(player);
        visitor.visit(new WindowA());
        visitor.visit(new WindowB());
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
            visitor.visit(new WindowC());
        }
        visitor.visit(new WindowD());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.WINDOW_CLICK);
    }

    public static class WindowA implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInWindowClick packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                int slot = packet.getWindowSlot();

                int inventorySlots = player.player.getOpenInventory().countSlots();
                if (player.player.getOpenInventory() != null && (player.player.getOpenInventory().getType() == InventoryType.CRAFTING && player.player.getInventory().getType() == InventoryType.PLAYER)) {
                    inventorySlots += 4; // armor slots
                }
                if (player.player.getOpenInventory() == null || slot > (inventorySlots - 1) || (slot < -1 && slot != -999)) {
                    return ResponseBuilder.inst().cancel().flag()
                            .onViolation(onViolation -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Slot", slot);
                            })
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 58;
        }

        @Override
        public String getName() {
            return "Window.A";
        }

        @Override
        public String getDescription() {
            return "Exceeding max slots of inventory";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.WINDOW_CLICK_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowA.class);
        }
    }

    public static class WindowB implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInWindowClick packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = packet.getItem();
                if (ItemAnalysis.checkItem(player, item, container.getDevelopmentData(this))) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 59;
        }

        @Override
        public String getName() {
            return "Window.B (IA)";
        }

        @Override
        public String getDescription() {
            return "Clicking on illegal item";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.WINDOW_CLICK_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowB.class);
        }
    }

    public static class WindowC implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInWindowClick packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                if (player.player.getOpenInventory().getType().name().equals("LECTERN")
                        && packet.getMode() == 1) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 60;
        }

        @Override
        public String getName() {
            return "Window.C";
        }

        @Override
        public String getDescription() {
            return "Illegal lectern quick move";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.WINDOW_CLICK_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowC.class);
        }
    }

    public static class WindowD implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInWindowClick packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (packet.getMode() < 0 || packet.getActionNumber() < 0 || packet.getWindowButton() < 0 || packet.getWindowId() < 0) {
                    HoneyPlugin.get().getLoggerService().getLogger().info("DEBUG: Mode: " + packet.getMode() + " , action: " + packet.getActionNumber() + ", id: " + packet.getWindowId() + ", button: " + packet.getWindowButton() + ", slot: " + packet.getWindowSlot());
                    return ResponseBuilder.inst().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 61;
        }

        @Override
        public String getName() {
            return "Window.D";
        }

        @Override
        public String getDescription() {
            return "Negative window click values";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.WINDOW_CLICK_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.WindowClick.WindowD.class);
        }
    }
}
