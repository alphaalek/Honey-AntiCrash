package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {

    private final Map<CommandSender, NotificationListener> observerMap = new HashMap<>();
    private final List<CommandSender> untoggledlist = new ArrayList<>();
    public static String PREFIX;

    public NotificationService() {
        subscribe(Bukkit.getConsoleSender());

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (!player.isOnline() && isSubscribed(player)) {
                        unsubscribe(player);
                        untoggledlist.remove(player);
                        return;
                    }
                    if (hasNotifyPermission(player) && !isSubscribed(player) && !isUntoggled(player)) {
                        subscribe(player);
                        return;
                    }
                    if(!hasNotifyPermission(player) && isSubscribed(player)) {
                        unsubscribe(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(HoneyPlugin.get(), 0L, 120L);

        reload();
    }

    public void reload() {
        PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX);
    }

    public boolean hasNotifyPermission(CommandSender observer) {
        return observer.hasPermission(ConfigurationOptions.General.NOTIFY_PERMISSION);
    }

    public boolean isUntoggled(CommandSender observer) {
        return untoggledlist.contains(observer);
    }

    public boolean isSubscribed(CommandSender observer) {
        return observerMap.containsKey(observer);
    }

    public void subscribe(final CommandSender observer) {
        observerMap.put(observer, new NotificationService.NotificationListener() {
            @Override
            public void notify(String notification) {
                observer.sendMessage(notification);
            }

            @Override
            public void notify(Player player, String type) {
                observer.sendMessage(PREFIX + "§7 " + player.getName() + formatNetwork(player) + "§7 failed §e" + type + "§7.");
            }

            @Override
            public void notify(Player player, String type, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, CheckContextContainer.DevelopmentData developmentData) {

                if (developmentData != null) {

                    StringBuilder hoverableTextBuilder = new StringBuilder();
                    for (String dataSetEntry : developmentData.getDataSet()) {
                        hoverableTextBuilder.append(dataSetEntry).append("\n");
                    }
                    String hoverableText = hoverableTextBuilder.substring(0, hoverableTextBuilder.length() - 1);

                    TextComponent message = new TextComponent(PREFIX + "§7 " + player.getName() + formatNetwork(player) + "§7 failed §e" + type +  "§7. " + formatNotification(setting, packetLimitHandler));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverableText).create()));

                    if (observer instanceof Player) {
                        Player observerPlayer = (Player) observer;

                        observerPlayer.spigot().sendMessage(message);
                    } else {
                        observer.sendMessage(message.getText());
                    }
                    return;
                }
                observer.sendMessage(PREFIX + "§7 " + player.getName() + formatNetwork(player) + "§7 failed §e" + type +  "§7. " + formatNotification(setting, packetLimitHandler));
            }

            @Override
            public void warn(Player player, String type) {
                observer.sendMessage(PREFIX + "§7 " + player.getName() + formatNetwork(player) + "§7 was kicked for §e" + type + "§7.");
            }
        });
    }

    public void unsubscribe(CommandSender observer) {
        observerMap.remove(observer);
    }

    public void untoggle(CommandSender observer) {
        unsubscribe(observer);
        untoggledlist.add(observer);
    }

    public void toggle(CommandSender observer) {
        subscribe(observer);
        untoggledlist.remove(observer);
    }

    public int getTPS() {
        Object minecraftServer = NMSUtils.getMinecraftServer();
        IStructureModifier<double[]> minecraftServerModifier = ReflectStructureCache.acquireStructure(NMSUtils.getMinecraftServerClass())
                .withType(double[].class).withTarget(minecraftServer);
        return (int) minecraftServerModifier.readField(0)[0];
    }

    public int getPing(Player player) {
        IStructureModifier<Object> craftPlayerModifier = ReflectStructureCache.acquireStructure(NMSUtils.getCraftPlayerClass())
                .withType(NMSUtils.getEntityPlayerClass()).withTarget(player);
        Object entityPlayer = craftPlayerModifier.invokeMethod("getHandle", 0);
        IStructureModifier<Integer> entityPlayerModifier
                = ReflectStructureCache.acquireStructure(entityPlayer.getClass())
                .asInt().withTarget(entityPlayer);
        return entityPlayerModifier.readField("ping", 0);
    }

    public String formatPing(Player player, boolean cache) {
        int ping = cache ? HoneyPlugin.get().getRecentPing(player) : getPing(player);
        return ((ping < 150) ? "§a" : (ping < 500) ? "§e" : "§c") + ping;
    }

    public String formatTPS(boolean cache) {
        int tps = cache ? HoneyPlugin.get().getRecentTPS() : getTPS();
        return ((tps > 16.5) ? "§a" : (tps < 8) ? "§e" : "§c") + tps;
    }

    private String formatNetwork(Player player) {
        String ping = formatPing(player, false);
        String tps = formatTPS(false);

        return " §8(" + ping + "ms§7/" + tps + "tps§8)";
    }

    public String formatNotification(ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler) {
        if (setting == null) {
            return "";
        }
        if (packetLimitHandler instanceof ViolationService.PacketIntervalHandler) {
            return "§7flags: §e" + ((ViolationService.PacketIntervalHandler)packetLimitHandler).getFlagged() + "§7/§e" + setting.getMaxPerInterval() + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.getVlsAmount() + "§7vls, §e" + (setting.getInterval() < 1000 ? + setting.getInterval() + "§7ms" : ((double)setting.getInterval())/1000 + "§7s");
        }
        else if (packetLimitHandler instanceof ViolationService.PacketBufferOverflowHandler) {
            return "§7buffer: §e" + ((int) ((ViolationService.PacketBufferOverflowHandler)packetLimitHandler).getBufferIndex()) + "§7/§e" + setting.getThreshold() + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.getVlsAmount() + "§7vls";
        }
        return "";
    }

    public void notify(String notification) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().notify(notification);
        }
    }

    public void notify(Player player, String notification, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, CheckContextContainer.DevelopmentData developmentData) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey()) || !player.isOnline() || HoneyPlugin.get().getViolationService().isPunishing(player)) {
                continue;
            }
            observer.getValue().notify(player, notification, setting, packetLimitHandler, developmentData);
        }
    }

    public void warn(Player player, String warning) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {
            if (!hasNotifyPermission(observer.getKey()) || !player.isOnline()) {
                continue;
            }
            observer.getValue().warn(player, warning);
        }
    }

    private interface NotificationListener {

        void notify(String notification);

        void notify(Player player, String notification);

        void notify(Player player, String notification, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, CheckContextContainer.DevelopmentData developmentData);

        void warn(Player player, String notification);
    }
}
