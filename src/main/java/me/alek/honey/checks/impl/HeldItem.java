package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class HeldItem extends AsyncPacketAdapter<WrappedPlayInHeldItemSlot> implements PacketCheck {

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {
        final WrappedPlayInHeldItemSlot packet = packetContainer.getPacket();
        if (packet.getSlot() >= 36 || packet.getSlot() < 0) {
            punish(player, packetContainer, "Illegal Slot");
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.HELD_ITEM_SLOT);
    }

    @Override
    public String getName() {
        return null;
    }
}
