package me.alek.honey.services;

import me.alek.honey.checks.Check;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ViolationService {

    public void increment(Player player, WrappedPacket<?> packet, Check check, String violationName) {
        Bukkit.broadcastMessage("violated " + player + " for " + violationName + " i " + packet);
    }

    public void increment(Player player, WrappedPacket<?> packet, Check check) {
        Bukkit.broadcastMessage("violated " + player + " for " + check.getName() + " i " + packet);
    }
}
