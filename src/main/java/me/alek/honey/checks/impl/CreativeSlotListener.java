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
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CreativeSlotListener extends AbstractPacketListener<WrappedPlayInSetCreativeSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInSetCreativeSlot> getVisitor() {
        CheckVisitor<WrappedPlayInSetCreativeSlot> visitor = new CheckVisitor<>();
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
        public ResponseContainer check(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (player.getGameMode() != GameMode.CREATIVE) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Creative.A";
        }

        @Override
        public String getDescription() {
            return "Using creative without being in right mode";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Creative.CreativeA.class);
        }
    }

    public static class CreativeB implements PacketCheck<WrappedPlayInSetCreativeSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                int slot = packet.getSlot();

                if (slot > (player.getOpenInventory().countSlots() + 3) || (slot < -1 && slot != -999)) {
                    container.getDevelopmentData(this).addPoint("Slot", slot);
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Creative.B";
        }

        @Override
        public String getDescription() {
            return "Exceeding max slots of inventory";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Creative.CreativeB.class);
        }
    }

    public static class CreativeC implements PacketCheck<WrappedPlayInSetCreativeSlot>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (ItemAnalysis.checkItem(player, packet.getItem())) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Creative.C";
        }

        @Override
        public String getDescription() {
            return "Clicking on illegal item";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Creative.CreativeC.class);
        }
    }
}
