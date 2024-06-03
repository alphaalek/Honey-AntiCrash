package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import me.alek.honey.util.CallbackAction;

import java.util.*;
import java.util.function.Supplier;

public class HeldItemValidation extends AbstractPacketListener<WrappedPlayInHeldItemSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer, CheckContextContainer container) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInHeldItemSlot> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedPlayInHeldItemSlot> visitor = new CheckVisitor<>(player);
        visitor.visit(new HeldItemA());
        visitor.visit(new HeldItemB());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.HELD_ITEM_SLOT);
    }

    public static class HeldItemA implements PacketCheck<WrappedPlayInHeldItemSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInHeldItemSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                int slot = packet.getSlot();
                if (slot >= 36 || slot < 0) {
                    return Validation.inst().cancel().flag().terminate()
                            .onViolation(onViolation -> {
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
            return 22;
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.HELD_ITEM_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.HeldItem.HeldItemA.class);
        }
    }

    public static class HeldItemB implements PacketCheck<WrappedPlayInHeldItemSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInHeldItemSlot packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                return ItemAnalysis.flagIf(player, player.player.getInventory().getItem(packet.getSlot()), container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return 23;
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.HELD_ITEM_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.HeldItem.HeldItemB.class);
        }
    }
}
