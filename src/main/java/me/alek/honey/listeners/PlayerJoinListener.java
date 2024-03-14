package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.services.NotificationService;
import me.alek.honey.storage.SQLManagerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().isOnline()) return; // kicked from other event?

        final HoneyPlayer player = HoneyPlayer.getPlayer(event.getPlayer().getUniqueId());

        for (IListener check : ListenerRegistry.getChecks()) {
            if (check.hasInitializedCheckInstances())
                check.setup(player);
        }
        HoneyPlugin.get().getNotificationService().setup(player);

        if (player.player.isOp() && HoneyPlugin.get().isUsingOldVersion()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(NotificationService.PREFIX + " §7Found an update: v" + HoneyPlugin.get().getLatestVersion() + ". Download:");
                    player.sendMessage("§ahttps://builtbybit.com/resources/honey-anti-crash-anti-packet-exploit.40905/");
                }
            }.runTaskLater(HoneyPlugin.get().getPlugin(), 40L);
        }

        SQLManagerWrapper sqlHandler = HoneyPlugin.get().getSQLManager();
        new BukkitRunnable() {

            @Override
            public void run() {
                if (sqlHandler.isEnabled()) {
                    try {
                        sqlHandler.preparePlayer(player);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }.runTaskAsynchronously(HoneyPlugin.get().getPlugin());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        HoneyPlayer player = HoneyPlayer.getPlayer(event.getPlayer().getUniqueId());
        if (player == null) player = HoneyPlayer.registerPlayer(event.getPlayer());

        for (IListener check : ListenerRegistry.getChecks()) {
            if (check.hasInitializedCheckInstances())
                check.cleanup(player);
        }

        HoneyPlugin.get().getLoggerService().removePacketData(player);
        HoneyPlugin.get().getNotificationService().unsubscribe(player.player);

        HoneyPlugin.get().removeRecentPingLookup(player);

        HoneyPlugin.get().getNettyPacketHandleTracker().remove(player);
        HoneyPlugin.get().getServerThreadHandleTracker().remove(player);

        HoneyPlugin.get().getMonitorManager().removeFromMonitors(player);

        player.leaveCallback.call(player);
    }
}
