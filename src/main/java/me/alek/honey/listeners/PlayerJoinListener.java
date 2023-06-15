package me.alek.honey.listeners;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.checks.CheckRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (Check check : CheckRegistry.getChecks()) {
            check.setup(event.getPlayer());
        }
        HoneyPlugin.get().getViolationService().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        for (Check check : CheckRegistry.getChecks()) {
            check.cleanup(event.getPlayer());
        }
        HoneyPlugin.get().getViolationService().removePlayer(event.getPlayer());
    }
}
