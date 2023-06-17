package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
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
        listenerMap.put(observer, new NotificationListener() {
            @Override
            public void notify(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String notification) {
                observer.sendMessage("§8[§6Honey§8] §e" + player.getName() + "§7 fejlede §e" + notification + "§f. §7(§a" + packet.getType().toString() + "§7)");
            }

            @Override
            public void warn(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String warning) {
                observer.sendMessage("§8[§6Honey§8] §e" + player.getName() + "§7 blev kicked for §e" + warning +  "§f. §7(§a" + packet.getType().toString() + "§7)");
            }
        });
    }

    public void unsubscribe(Player player) {
        listenerMap.remove(player);
    }

    public void notify(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String notification) {
        for (Map.Entry<Player, NotificationListener> observer : listenerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().notify(player, packet, notification);
        }
    }

    public void warn(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String warning) {
        for (Map.Entry<Player, NotificationListener> observer : listenerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().warn(player, packet, warning);
        }
    }

    private interface NotificationListener {

        void notify(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String notification);

        void warn(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String warning);
    }
}
