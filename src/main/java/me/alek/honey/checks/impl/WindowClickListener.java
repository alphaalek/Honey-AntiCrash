package me.alek.honey.checks.impl;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInWindowClick;
import me.alek.honey.netty.utility.protocol.NetProtocol;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class WindowClickListener extends AbstractPacketListener<WrappedPlayInWindowClick> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInWindowClick> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInWindowClick> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInWindowClick> getVisitor() {
        CheckVisitor<WrappedPlayInWindowClick> visitor = new CheckVisitor<>();
        visitor.visit(new WindowA());
        visitor.visit(new WindowB());
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
            visitor.visit(new WindowC());
        }
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.WINDOW_CLICK);
    }

    public static class WindowA implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInWindowClick packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                int slot = packet.getWindowSlot();

                int inventorySlots = player.getOpenInventory().countSlots();
                if (player.getOpenInventory() != null && (player.getOpenInventory().getType() == InventoryType.CRAFTING && player.getInventory().getType() == InventoryType.PLAYER)) {
                    inventorySlots += 4; // armor slots
                }
                if (player.getOpenInventory() == null || slot > (inventorySlots - 1) || (slot < -1 && slot != -999)) {
                    container.getDevelopmentData(this).addPoint("Slot", slot);
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.WindowClick.WindowA.class);
        }
    }

    public static class WindowB implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInWindowClick packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = packet.getItem();
                if (ItemAnalysis.checkItem(player, item)) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Window.B";
        }

        @Override
        public String getDescription() {
            return "Clicking on illegal item";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.WindowClick.WindowB.class);
        }
    }

    public static class WindowC implements PacketCheck<WrappedPlayInWindowClick>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInWindowClick packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                if (player.getOpenInventory().getType().name().equals("LECTERN")) {

                    if (packet.getMode() == 1) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.WindowClick.WindowC.class);
        }
    }
}
