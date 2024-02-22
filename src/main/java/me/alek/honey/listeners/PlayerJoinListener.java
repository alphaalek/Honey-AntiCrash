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

    private final static Map<UUID, Long> instantJoinedMap = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final HoneyPlayer player = HoneyPlayer.getPlayer(event.getPlayer().getUniqueId());

        for (IListener check : ListenerRegistry.getChecks()) {
            if (check.hasInitializedCheckInstances())
                check.setup(player);
        }
        HoneyPlugin.get().getNotificationService().setup(player);

        instantJoinedMap.put(player.uuid, System.currentTimeMillis());

        if (player.player.isOp() && HoneyPlugin.get().isUsingOldVersion()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(NotificationService.PREFIX + " §7Found an update: v" + HoneyPlugin.get().getLatestVersion() + ". Download:");
                    player.sendMessage("§ahttps://github.com/alphaalek/Honey-AntiCrash/releases/");
                }
            }.runTaskLater(HoneyPlugin.get().getPlugin(), 40L);
        }

        SQLManagerWrapper sqlHandler = HoneyPlugin.get().getSQLManager();
        if (sqlHandler.isEnabled()) {
            try {
                sqlHandler.preparePlayer(player);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final HoneyPlayer player = HoneyPlayer.getPlayer(event.getPlayer().getUniqueId());
        for (IListener check : ListenerRegistry.getChecks()) {
            if (check.hasInitializedCheckInstances())
                check.cleanup(player);
        }
        instantJoinedMap.remove(player.uuid);

        HoneyPlugin.get().getLoggerService().removePacketData(player);
        HoneyPlugin.get().getNotificationService().unsubscribe(player.player);

        HoneyPlugin.get().removeRecentPingLookup(player);

        HoneyPlugin.get().getNettyPacketHandleTracker().remove(player);
        HoneyPlugin.get().getServerThreadHandleTracker().remove(player);

        HoneyPlugin.get().getMonitorManager().removeFromMonitors(player);
    }

    public static Map<UUID, Long> getInstantJoinedMap() {
        return instantJoinedMap;
    }

    public static long getInstantJoined(Player player) {
        return instantJoinedMap.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }

}
