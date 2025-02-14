package me.alek.honey.netty.bukkit;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitEventInternal implements Listener {

    private static final String BROKEN_PIPE_KICK_MESSAGE = "Broken pipe! Try reconnecting.";

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        HoneyPacketHandler packetHandler = HoneyPacketHandler.AWAITING_PROFILES.remove(player.getName());
        if (packetHandler == null) {
            HoneyPlayer.kick(player, BROKEN_PIPE_KICK_MESSAGE);
            return;
        }
        packetHandler.getPlayer().initBukkitPlayer(player);
    }
}
