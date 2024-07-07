package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        HoneyPlayer player = HoneyPlayer.getPlayer(event.getPlayer().getUniqueId());
        if (player == null) {
            return;
        }
        player.remove();
    }
}
