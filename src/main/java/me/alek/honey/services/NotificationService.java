package me.alek.honey.services;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NotificationService {

    public void notify(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String notification) {
        Bukkit.broadcastMessage("Notify " + player.getName() + " " + notification);
    }

    public void warn(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String warning) {
        Bukkit.broadcastMessage("Warning " + player.getName() + " " + warning);
    }
}
