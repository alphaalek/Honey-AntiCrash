package me.alek.honey.listeners;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import org.bukkit.entity.Player;

public class PacketLogger extends FuzzyPacketAdapter {

    @Override
    @SuppressWarnings("unchecked")
    public void onPacketReceive(Player player, PacketContainer packetContainer) {
        HoneyPlugin.get().getLoggerService().logPacket(packetContainer, player.getName());
    }
}
