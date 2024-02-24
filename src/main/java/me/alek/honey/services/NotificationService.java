package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutChat;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.services.messages.MessageFormat;
import me.alek.honey.services.messages.MessageOffer;
import me.alek.honey.services.messages.Placeholders;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.DecayingListeningCounter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NotificationService {

    private final Map<CommandSender, NotificationListener> observerMap = new HashMap<>();
    private final List<CommandSender> untoggledlist = new ArrayList<>();
    private final Map<UUID, DecayingListeningCounter> recentlyCancelledPacketsMap = new HashMap<>();

    public static String PREFIX;
    private MessageFormat violationFormat;
    private MessageFormat punishFormat;

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
        }.runTaskTimerAsynchronously(HoneyPlugin.get().getPlugin(), 0L, 120L);

        reload();
    }

    public void setup(HoneyPlayer player) {
        if (hasNotifyPermission(player.player) && !isSubscribed(player.player) && !isUntoggled(player.player)) {
            subscribe(player.player);
        }
    }

    public void reload() {
        PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX);
        violationFormat = MessageFormat.load(ConfigurationOptions.Messages.VIOLATION_MESSAGE);
        punishFormat = MessageFormat.load(ConfigurationOptions.Messages.PUNISH_MESSAGE);
    }

    public String getPrefix() {
        return PREFIX + "§r";
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

    private String getAppropriateMessage(CommandSender observer, String message) {
        if (observer instanceof Player) return message;

        return ChatColor.stripColor(message);
    }

    public void subscribe(final CommandSender observer) {
        observerMap.put(observer, new NotificationService.NotificationListener() {
            @Override
            public void notify(String notification) {
                observer.sendMessage(getAppropriateMessage(observer, notification));
            }

            @Override
            public void notify(HoneyPlayer player, String type, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, CheckContextContainer.DevelopmentData developmentData, ViolationService.CheckKey checkKey) {
                MessageOffer offer = getOffer(player, type, checkKey);
                offer = offer.builder()
                        .offer(Placeholders.CONTEXT, () -> formatNotification(setting, packetLimitHandler))
                        .build();
                String message = getMessage(violationFormat, offer);

                if (developmentData != null && observer instanceof Player) {
                    StringBuilder hoverableTextBuilder = new StringBuilder();
                    for (String dataSetEntry : developmentData.getDataSet()) {
                        hoverableTextBuilder.append(dataSetEntry).append("\n");
                    }
                    String hoverableText = hoverableTextBuilder.substring(0, hoverableTextBuilder.length() - 1);

                    TextComponent textComponentMessage = new TextComponent(message);
                    textComponentMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverableText).create()));

                    Player observerPlayer = (Player) observer;
                    observerPlayer.spigot().sendMessage(textComponentMessage);
                }
                else observer.sendMessage(message);
            }

            @Override
            public void warn(HoneyPlayer player, String type, ViolationService.CheckKey checkKey) {
                observer.sendMessage(getMessage(punishFormat, getOffer(player, type, checkKey)));
            }
        });
    }

    public MessageOffer getOffer(HoneyPlayer player, String type, ViolationService.CheckKey checkKey) {
        return new MessageOffer.Builder()
                .offer(Placeholders.PLAYER, () -> player.name)
                .offer(Placeholders.NETWORK, () -> formatNetworkStats(player))
                .offer(Placeholders.CHECK, () -> type + (checkKey == null ? "" : " (" + checkKey.getT1() + ")"))
                .build();
    }

    public String getMessage(MessageFormat format, MessageOffer offer) {
        return format.getMessage(offer);
    }

    public void unsubscribe(CommandSender observer) {
        observerMap.remove(observer);

        if (observer instanceof Player) {
            recentlyCancelledPacketsMap.remove(((Player)observer).getUniqueId());
        }
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

    public int getPing(HoneyPlayer player) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17)) {
            IStructureModifier<Integer> craftPlayerModifier =
                    ReflectStructureCache.acquireStructure(NMSUtils.getCraftPlayerClass())
                    .asInt().withTarget(player.player);

            return craftPlayerModifier.invokeMethod("getPing");
        }
        IStructureModifier<Object> craftPlayerModifier =
                ReflectStructureCache.acquireStructure(NMSUtils.getCraftPlayerClass())
                .withType(NMSUtils.getEntityPlayerClass()).withTarget(player.player);

        Object entityPlayer = craftPlayerModifier.invokeMethod("getHandle");

        IStructureModifier<Integer> entityPlayerModifier =
                ReflectStructureCache.acquireStructure(entityPlayer.getClass())
                .asInt().withTarget(entityPlayer);

        return entityPlayerModifier.readField("ping", 0);
    }

    public String formatPing(HoneyPlayer player) {
        int ping = getPing(player);
        return ((ping < 150) ? "§a" : (ping < 500) ? "§e" : "§c") + ping;
    }

    public String formatTPS() {
        int tps = (int) HoneyPlugin.get().getTickloop().evaluateTPS(Timespan.TEN_SECOND);
        return ((tps > 16.5) ? "§a" : (tps < 8) ? "§e" : "§c") + tps;
    }

    private String formatNetworkStats(HoneyPlayer player) {
        String ping = formatPing(player);
        String tps = formatTPS();

        return "§8(" + ping + "ms§7/" + tps + "tps§8)§7";
    }

    public String formatNotification(ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler) {
        if (setting == null) {
            return "";
        }
        if (packetLimitHandler instanceof ViolationService.PacketIntervalHandler) {
            return "§7flags: §e" + ((ViolationService.PacketIntervalHandler)packetLimitHandler).flaggedCopy + "§7/§e" + setting.maxPerInterval + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls, §e" + (setting.interval < 1000 ? setting.interval + "§7ms" : ((double)setting.interval)/1000 + "§7s");
        }
        else if (packetLimitHandler instanceof ViolationService.PacketIntervalThrottleHandler) {
            ViolationService.PacketIntervalThrottleHandler packetIntervalThrottleHandler = (ViolationService.PacketIntervalThrottleHandler) packetLimitHandler;
            return "§7flags: §e" + ((int) packetIntervalThrottleHandler.getAmount()) + "§7/§e" + packetLimitHandler.getSetting().threshold + "§7/§e"
                    + packetLimitHandler.getSetting().until + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls";
        }
        else if (packetLimitHandler instanceof ViolationService.PacketBufferThrottleHandler) {
            ViolationService.PacketBufferThrottleHandler packetBufferThrottleHandler = (ViolationService.PacketBufferThrottleHandler) packetLimitHandler;
            return "§7buffer: §e" + ((int) packetBufferThrottleHandler.bufferIndexCopy) + "§7/§e" + setting.until + "§7/§e" + setting.threshold + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls";
        }
        else if (packetLimitHandler instanceof ViolationService.PacketBufferOverflowHandler) {
            ViolationService.PacketBufferOverflowHandler packetBufferOverflowHandler = (ViolationService.PacketBufferOverflowHandler) packetLimitHandler;
            return "§7buffer: §e" + ((int) packetBufferOverflowHandler.bufferIndexCopy) + "§7/§e" + setting.threshold +
                    "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls";
        }
        else if (packetLimitHandler instanceof ViolationService.PacketSingleOverflowHandler) {
            ViolationService.PacketSingleOverflowHandler packetSingleOverflowHandler = (ViolationService.PacketSingleOverflowHandler) packetLimitHandler;
            return "§7flags: §e" + ((int) packetSingleOverflowHandler.getAmount()) + "§7/§e" + setting.threshold + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls";
        }
        else if (packetLimitHandler instanceof ViolationService.PacketSimpleHandler) {
            return "§7flags: §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls";
        }
        return "";
    }

    public void showCancellingPackets(HoneyPlayer player) {
        if (player == null) return;

        DecayingListeningCounter counter = recentlyCancelledPacketsMap.computeIfAbsent(player.uuid,
                (d) -> new DecayingListeningCounter(() -> (long) 3000));
        counter.count();

        int size = counter.size();
        if (size < 5) return;

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            player.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§cCancelling inbound packets... [" + size + "]"));
            return;
        }
        sendActionBarPacket1_8(player, "§cCancelling inbound packets... [" + size + "]");
    }

    public void sendActionBarPacket1_8(HoneyPlayer player, String text) {
        WrappedPacket<WrappedPlayOutChat> actionBarPacket = PacketFactory.createNewPacket(PacketType.Play.Server.CHAT, player);
        PacketContainer<WrappedPlayOutChat> packetContainer = actionBarPacket.getContainer();

        packetContainer.getBytes().writeField(0, (byte) 2);

        IStructureModifier<Object> chatSerializer = ReflectStructureCache.acquireStructure(NMSUtils.getChatSerializerClass())
                .withType(NMSUtils.getIChatBaseComponentClass())
                .withTarget(null);
        Object chatComponent = chatSerializer.invokeMethod(0,
                "{\"text\":\"" + text + "\"}");
        packetContainer.getObjects(NMSUtils.getIChatBaseComponentClass()).writeField(0, chatComponent);

        HoneyPlugin.get().getPacketLibrary().getPacketManager().writeForcedPacket(player, actionBarPacket, true);
    }

    public void notify(String notification) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {

            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().notify(notification);
        }
    }

    public void notify(HoneyPlayer player, String notification, ViolationService.CheckKey checkKey, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, CheckContextContainer.DevelopmentData developmentData) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {

            if (!hasNotifyPermission(observer.getKey()) || !player.isOnline() || player.punished) {
                continue;
            }
            observer.getValue().notify(player, notification, setting, packetLimitHandler, developmentData, checkKey);
        }
    }

    public void warn(HoneyPlayer player, String warning, ViolationService.CheckKey checkKey) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {

            if (!hasNotifyPermission(observer.getKey()) || !player.isOnline() || player.punished) {
                continue;
            }
            observer.getValue().warn(player, warning, checkKey);
        }
    }

    private interface NotificationListener {

        void notify(String notification);

        void notify(HoneyPlayer player, String notification, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, CheckContextContainer.DevelopmentData developmentData, ViolationService.CheckKey checkKey);

        void warn(HoneyPlayer player, String notification, ViolationService.CheckKey checkKey);
    }
}
