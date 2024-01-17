package me.alek.honey.listeners;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.storage.SQLManagerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
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
        final Player player = event.getPlayer();
        for (IListener check : ListenerRegistry.getChecks()) {
            check.setup(player);
        }
        HoneyPlugin.get().getViolationService().add(player);

        if (player.hasPermission(ConfigurationOptions.General.NOTIFY_PERMISSION))
            HoneyPlugin.get().getNotificationService().subscribe(player);

        instantJoinedMap.put(player.getUniqueId(), System.currentTimeMillis());

        if (player.isOp() && HoneyPlugin.get().isUsingOldVersion()) {
            HoneyPlugin.get().getUpdateChecker().sendWarning(player);
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
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(35);
                } catch (Exception ex) {

                };
            }
        };
        runnable.runTaskTimer(HoneyPlugin.get().getPlugin(), 0L, 2L);
        new BukkitRunnable() {

            @Override
            public void run() {
                runnable.cancel();
            }
        }.runTaskLater(HoneyPlugin.get().getPlugin(), 18L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        for (IListener check : ListenerRegistry.getChecks()) {
            check.cleanup(player);
        }
        instantJoinedMap.remove(player.getUniqueId());

        HoneyPlugin.get().getViolationService().remove(player);
        HoneyPlugin.get().getNotificationService().unsubscribe(player);

        HoneyPlugin.get().removeRecentPingLookup(player);

        HoneyPlugin.get().getNettyPacketHandleTracker().remove(player);
        HoneyPlugin.get().getServerThreadHandleTracker().remove(player);
    }

    public static Map<UUID, Long> getInstantJoinedMap() {
        return instantJoinedMap;
    }

    public static long getInstantJoined(Player player) {
        return instantJoinedMap.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }

}
