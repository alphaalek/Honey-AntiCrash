package me.alek.honey.listeners;

import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.END_PORTAL)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> event.getPlayer().teleport(event.getTo()));
        }
    }
}
