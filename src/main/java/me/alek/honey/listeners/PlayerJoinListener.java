package me.alek.honey.listeners;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements org.bukkit.event.Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        for (IListener check : ListenerRegistry.getChecks()) {
            check.setup(player);
        }
        HoneyPlugin.get().getViolationService().addPlayer(player);

        if (player.hasPermission(ConfigurationOptions.General.NOTIFY_PERMISSION)) {
            HoneyPlugin.get().getNotificationService().subscribe(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        for (IListener check : ListenerRegistry.getChecks()) {
            check.cleanup(player);
        }
        HoneyPlugin.get().getViolationService().removePlayer(player);
        HoneyPlugin.get().getNotificationService().unsubscribe(player);
    }
}
