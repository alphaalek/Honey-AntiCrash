package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.services.NotificationService;
import me.alek.honey.storage.SQLManagerWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    public static void onPostLoginSuccess(HoneyPlayer player) {
        for (IListener check : ListenerRegistry.getChecks()) {
            if (check.hasInitializedCheckInstances()) {
                check.setup(player);
            }
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
        if (player == null) {
            player = HoneyPlayer.registerPlayer(event.getPlayer());
        }

        for (IListener check : ListenerRegistry.getChecks()) {
            if (check.hasInitializedCheckInstances()) {
                check.cleanup(player);
            }
        }

        HoneyPlugin.get().getLoggerService().removePacketData(player);
        HoneyPlugin.get().getNotificationService().unsubscribe(player.player);

        HoneyPlugin.get().removeRecentPingLookup(player);

        HoneyPlugin.get().getNettyPacketTracker().remove(player);
        HoneyPlugin.get().getServerThreadTracker().remove(player);

        HoneyPlugin.get().getMonitorManager().removeFromMonitors(player);

        player.leaveCallback.call(player);
        player.online = false;
        IDRegistry.PLAYERS.remove(player.registryID);
    }
}
