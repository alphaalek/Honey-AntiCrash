package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class HeldItemListener extends AbstractPacketListener<WrappedPlayInHeldItemSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInHeldItemSlot> getVisitor() {
        CheckVisitor<WrappedPlayInHeldItemSlot> visitor = new CheckVisitor<>();
        visitor.visit(new HeldItemA());
        visitor.visit(new HeldItemB());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.HELD_ITEM_SLOT);
    }

    public static class HeldItemA implements PacketCheck<WrappedPlayInHeldItemSlot> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInHeldItemSlot packet, CheckContextContainer container) {
            if (packet.getSlot() >= 36 || packet.getSlot() < 0) {
                return ResponseBuilder.inst().cancel().punish().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "HeldItem.A";
        }
    }

    public static class HeldItemB implements PacketCheck<WrappedPlayInHeldItemSlot> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInHeldItemSlot packet, CheckContextContainer container) throws Exception {
            if (ItemAnalysis.checkItem(player, player.getInventory().getItem(packet.getSlot()))) {
                return ResponseBuilder.inst().cancel().punish().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "HeldItem.B";
        }
    }

}
