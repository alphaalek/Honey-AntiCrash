package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.impl.HeldItemA;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class HeldItem extends AbstractPacketListener<WrappedPlayInHeldItemSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInHeldItemSlot> getVisitor() {
        CheckVisitor<WrappedPlayInHeldItemSlot> visitor = new CheckVisitor<>();
        visitor.visit(new HeldItemA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.HELD_ITEM_SLOT);
    }

}
