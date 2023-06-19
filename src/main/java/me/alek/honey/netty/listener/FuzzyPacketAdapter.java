package me.alek.honey.netty.listener;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import org.bukkit.entity.Player;

public class FuzzyPacketAdapter extends SyncPacketAdapter {

    public void onPacketReceive(Player player, PacketContainer packetContainer) {

    }

    public void onPacketSend(Player player, PacketContainer packetContainer) {

    }

    public void onPacketCancel(Player player, PacketContainer packetContainer, PacketBound bound) {

    }
}
