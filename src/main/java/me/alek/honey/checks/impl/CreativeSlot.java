package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.services.ViolationService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CreativeSlot extends AsyncPacketAdapter<WrappedPlayInSetCreativeSlot> implements Check {

    private final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {
        WrappedPlayInSetCreativeSlot packet = packetContainer.getPacket();
        if (player.getGameMode() != GameMode.CREATIVE) {
            VIOLATION_SERVICE.increment(player, packet, this);
            packetContainer.cancel();
            return;
        }
        int slot = packet.getSlot();
        if (slot > 103 || (slot < 0 && slot != -999)) {
            VIOLATION_SERVICE.increment(player, packet, this, "Illegal Slot");
            packetContainer.cancel();
            return;
        }

        if (packet.getItem() == null) {
            return;
        }
        if (NBTHelper.checkNMSNBT(packet.getItem())) {
            VIOLATION_SERVICE.increment(player, packet, this, "Illegal NBT");
            packetContainer.cancel();
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.SET_CREATIVE_SLOT);
    }

    @Override
    public String getName() {
        return "Creative Slot";
    }
}
