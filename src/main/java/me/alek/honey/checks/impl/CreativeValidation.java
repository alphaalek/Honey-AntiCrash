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
import me.alek.honey.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.util.CallbackAction;
import org.bukkit.GameMode;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CreativeValidation extends AbstractPacketListener<WrappedPlayInSetCreativeSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer, CheckContextContainer container) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInSetCreativeSlot> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedPlayInSetCreativeSlot> visitor = new CheckVisitor<>(player);
        visitor.visit(new CreativeA());
        visitor.visit(new CreativeB());
        visitor.visit(new CreativeC());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.SET_CREATIVE_SLOT);
    }

    public static class CreativeA implements PacketCheck<WrappedPlayInSetCreativeSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (player.player.getGameMode() != GameMode.CREATIVE) {
                    return Validation.inst().cancel().flag().build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return 8;
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CREATIVE_SLOT_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeA.class);
        }
    }

    public static class CreativeB implements PacketCheck<WrappedPlayInSetCreativeSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                int slot = packet.getSlot();
                if (slot > (player.player.getOpenInventory().countSlots() + 3) || (slot < -1 && slot != -999)) {
                    return Validation.inst().cancel().flag()
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
            return 9;
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CREATIVE_SLOT_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeB.class);
        }
    }

    public static class CreativeC implements PacketCheck<WrappedPlayInSetCreativeSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                return ItemAnalysis.flagIf(player, packet.getItem(), container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return 10;
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.CREATIVE_SLOT_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Creative.CreativeC.class);
        }
    }
}
