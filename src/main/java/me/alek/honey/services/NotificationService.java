package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {

    private final Map<Player, NotificationListener> listenerMap = new HashMap<>();

    public NotificationService() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isOnline() && isSubscribed(player)) {
                        unsubscribe(player);
                        return;
                    }
                    if (hasNotifyPermission(player) && !isSubscribed(player)) {
                        subscribe(player);
                        return;
                    }
                    if(!hasNotifyPermission(player) && isSubscribed(player)) {
                        unsubscribe(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(HoneyPlugin.get(), 0L, 120L);
    }

    public boolean hasNotifyPermission(Player player) {
        return player.hasPermission(ConfigurationOptions.General.NOTIFY_PERMISSION);
    }

    public boolean isSubscribed(Player player) {
        return listenerMap.containsKey(player);
    }

    public void subscribe(final Player observer) {
        listenerMap.put(observer, new NotificationService.NotificationListener() {
            @Override
            public void notify(String notification) {
                observer.sendMessage(notification);
            }

            @Override
            public void notify(Player player, String type) {
                observer.sendMessage("§8[§6Honey§8] §e" + player.getName() + "§7 fejlede packets §e" + type + "§7.");
            }

            @Override
            public void notify(Player player, String type, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler) {
                observer.sendMessage("§8[§6Honey§8] §e" + player.getName() + "§7 fejlede packets §e" + type +  "§7. " + formatNotification(setting, packetLimitHandler));
            }

            @Override
            public void warn(Player player, String type) {
                observer.sendMessage("§8[§6Honey§8] §e" + player.getName() + "§7 blev kicked for packets §e" + type + "§7.");
            }
        });
    }

    public String formatNotification(ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler) {
        if (setting == null) {
            return "";
        }
        if (packetLimitHandler instanceof ViolationService.PacketIntervalHandler) {
            return "§7flags: §e" + ((ViolationService.PacketIntervalHandler)packetLimitHandler).getFlagged() + "§7/§e" + setting.getMaxPerInterval() + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.getVlsAmount() + "§7vls, §e" + setting.getInterval() + "§7ms";
        }
        else if (packetLimitHandler instanceof ViolationService.PacketBufferHandler) {
            return "§7buffer: §e" + ((ViolationService.PacketBufferHandler)packetLimitHandler).getBufferIndex() + "§7/§e" + setting.getThreshold() + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.getVlsAmount() + "§7vls, §e" + setting.getInterval() + "§7ms";
        }
        return "";
    }

    public void unsubscribe(Player player) {
        listenerMap.remove(player);
    }

    public void notify(String notification) {
        for (Map.Entry<Player, NotificationListener> observer : listenerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().notify(notification);
        }
    }

    public void notify(Player player, String notification) {
        notify(player, notification, null, null);
    }

    public void notify(Player player, String notification, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler) {
        for (Map.Entry<Player, NotificationListener> observer : listenerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey()) || !player.isOnline() || HoneyPlugin.get().getViolationService().isPunishing(player)) {
                continue;
            }
            if (setting == null || packetLimitHandler == null) {
                observer.getValue().notify(player, notification);
            }
            else {
                observer.getValue().notify(player, notification, setting, packetLimitHandler);
            }
        }
    }

    public void warn(Player player, String warning) {
        for (Map.Entry<Player, NotificationListener> observer : listenerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey()) || !player.isOnline()) {
                continue;
            }
            observer.getValue().warn(player, warning);
        }
    }

    private interface NotificationListener {

        void notify(String notification);

        void notify(Player player, String notification);

        void notify(Player player, String notification, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler);

        void warn(Player player, String notification);
    }
}
