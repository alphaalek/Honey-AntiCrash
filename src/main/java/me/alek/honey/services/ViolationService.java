package me.alek.honey.services;

import me.alek.honey.listeners.check.Check;
import me.alek.honey.netty.wrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ViolationService {

    public void violate(Player player, WrappedPacket<?> packet, Check check, String violationName) {
        Bukkit.broadcastMessage("violated " + player + " for " + violationName + " i " + packet);
    }

    public void violate(Player player, WrappedPacket<?> packet, Check check) {
        Bukkit.broadcastMessage("violated " + player + " for " + check.getName() + " i " + packet);
    }
}
