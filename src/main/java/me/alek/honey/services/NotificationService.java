package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.ValidationContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.util.messages.MessageFormat;
import me.alek.honey.util.messages.MessageOffer;
import me.alek.honey.util.messages.Placeholders;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutChat;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.counter.DecayingListeningCounter;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.util.TPSProvider;
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
    private final Map<UUID, DecayingListeningCounter> cancelledPackets = new HashMap<>();
    private final Map<UUID, DecayingListeningCounter> postponedPackets = new HashMap<>();

    public static String PREFIX;
    private MessageFormat violationFormat;
    private MessageFormat punishFormat;
    private String packetFilterPre;
    private MessageFormat packetFilterCancelFormat;
    private MessageFormat packetFilterPostponeFormat;
    private MessageFormat quarantinedAsyncWorkerFormat;

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

        packetFilterPre = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.Messages.PACKET_FILTER_PRE);
        packetFilterCancelFormat = MessageFormat.load(ConfigurationOptions.Messages.PACKET_FILTER_CANCEL);
        packetFilterPostponeFormat = MessageFormat.load(ConfigurationOptions.Messages.PACKET_FILTER_POSTPONE);

        quarantinedAsyncWorkerFormat = MessageFormat.load(ConfigurationOptions.Messages.QUARANTINED_ASYNC_WORKER);
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
        observerMap.put(observer, new NotificationListener() {
            @Override
            public void notify(String notification) {
                observer.sendMessage(getAppropriateMessage(observer, notification));
            }

            @Override
            public void notify(HoneyPlayer player, String type, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, ValidationContextContainer.DevelopmentData developmentData, ViolationService.HoneyKey honeyKey) {
                MessageOffer offer = getOffer(player, type, honeyKey);
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
            public void warn(HoneyPlayer player, String type, ViolationService.HoneyKey honeyKey) {
                observer.sendMessage(getMessage(punishFormat, getOffer(player, type, honeyKey)));
            }
        });
    }

    public MessageOffer getOffer(HoneyPlayer player, String type, ViolationService.HoneyKey honeyKey) {
        return new MessageOffer.Builder()
                .offer(Placeholders.PLAYER, () -> player.name)
                .offer(Placeholders.NETWORK, () -> formatNetworkStats(player))
                .offer(Placeholders.CHECK, () -> type + (honeyKey != null && honeyKey.getIdentifier() != null ? " (" + honeyKey.getIdentifier() + ")" : ""))
                .build();
    }

    public String getMessage(MessageFormat format, MessageOffer offer) {
        return format.getMessage(offer);
    }

    public void unsubscribe(CommandSender observer) {
        observerMap.remove(observer);

        if (observer instanceof Player) {
            cancelledPackets.remove(((Player)observer).getUniqueId());
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
        Object minecraftServer = NMS.getMinecraftServer();

        IStructureModifier<double[]> minecraftServerModifier = ReflectStructureCache.acquireStructure(NMS.getMinecraftServerClass())
                .withType(double[].class).withTarget(minecraftServer);

        return (int) minecraftServerModifier.readField(0)[0];
    }

    public int getPing(HoneyPlayer player) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17)) {
            IStructureModifier<Integer> craftPlayerModifier =
                    ReflectStructureCache.acquireStructure(NMS.getCraftPlayerClass())
                    .withInts().withTarget(player.player);

            return craftPlayerModifier.invokeMethod(null, "getPing", 0);
        }
        IStructureModifier<Object> craftPlayerModifier =
                ReflectStructureCache.acquireStructure(NMS.getCraftPlayerClass())
                .withType(NMS.getEntityPlayerClass()).withTarget(player.player);

        Object entityPlayer = craftPlayerModifier.invokeMethod(null, "getHandle", 0);

        IStructureModifier<Integer> entityPlayerModifier =
                ReflectStructureCache.acquireStructure(entityPlayer.getClass())
                .withInts().withTarget(entityPlayer);

        return entityPlayerModifier.readField(null, "ping", 0);
    }

    public String formatPing(HoneyPlayer player) {
        int ping = Math.max(0, getPing(player));
        return ((ping < 150) ? "§a" : (ping < 500) ? "§e" : "§c") + ping;
    }

    private String formatNetworkStats(HoneyPlayer player) {
        String ping = formatPing(player);
        String tps = TPSProvider.getTPS(Timespan.FIVE_SECOND);

        return "§8(" + ping + "ms§7/" + tps + "tps§8)§7";
    }

    private String formatPercentages(double[] percentages) {
        StringBuilder message = new StringBuilder();

        for (double percentage : percentages) {
            if (percentage == 0) percentage = 1;
            message.append(", ").append(NumberUtils.format(percentage * 100, 2)).append("%");
        }
        return message.substring(2, message.length());
    }

    public void applyQuotients(HoneyPlayer player, ValidationContextContainer.DevelopmentData developmentData) {
        WideTempQueuedHandleTracker serverThreadHandleTracker = HoneyPlugin.get().getExecutionQueueProxy();
        WideTempQueuedHandleTracker nettyThreadHandleTracker = HoneyPlugin.get().getNettyPacketTracker();

        String serverThreadPoint = formatPercentages(serverThreadHandleTracker.getStatistics(player));
        String nettyThreadPoint = formatPercentages(nettyThreadHandleTracker.getStatistics(player));

        developmentData.setPoint(6, "STQ", serverThreadPoint);
        developmentData.setPoint(7, "NTQ", nettyThreadPoint);

        developmentData.addInternalData("TPS=" + ChatColor.stripColor(TPSProvider.getTPS(Timespan.FIVE_SECOND)));
        developmentData.addInternalData("Ping=" + HoneyPlugin.get().getNotificationService().getPing(player));
    }

    public String formatNotification(ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler) {
        if (setting == null) {
            return "";
        }
        if (packetLimitHandler instanceof ViolationService.PacketIntervalHandler) {
            ViolationService.PacketIntervalHandler packetIntervalHandler = (ViolationService.PacketIntervalHandler) packetLimitHandler;
            return "§7flags: §e" + packetIntervalHandler.flaggedCopy + "§7/§e" + setting.maxPerInterval + "§7, §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls, §e"
                    + (setting.interval < 1000 ? setting.interval + "§7ms" : ((double)setting.interval)/1000 + "§7s");
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
        return "§7flags: §e" + packetLimitHandler.getViolations() + "§7vls/§e" + setting.vlsAmount + "§7vls";
    }

    public void showPacketFilterActionBar(HoneyPlayer player, boolean increment, boolean incrementCancelled) {
        if (!ConfigurationOptions.Messages.PACKET_FILTER_ENABLED
                || !player.isBukkitReady()
                || !player.isOnlineDirect()) {
            return;
        }

        DecayingListeningCounter cancelled = cancelledPackets.computeIfAbsent(player.uuid,
                (d) -> new DecayingListeningCounter(() -> (long) 5000));
        DecayingListeningCounter postponed = postponedPackets.computeIfAbsent(player.uuid,
                (d) -> new DecayingListeningCounter(() -> (long) 5000));

        if (increment) {
            if (incrementCancelled) {
                postponed.collect();
                cancelled.count();
            } else {
                cancelled.collect();
                postponed.count();
            }
        } else {
            cancelled.collect();
            postponed.collect();
        }

        int postponedSize = postponed.size();
        int cancelledSize = cancelled.size();

        if (postponedSize < ConfigurationOptions.Messages.PACKET_FILTER_MIN_POSTPONE
                && cancelledSize < ConfigurationOptions.Messages.PACKET_FILTER_MIN_CANCEL) {
            return;
        }

        StringBuilder messageBuilder = new StringBuilder(packetFilterPre);
        if (postponedSize >= ConfigurationOptions.Messages.PACKET_FILTER_MIN_POSTPONE) {
            messageBuilder.append(" ").append(packetFilterPostponeFormat.getMessage(
                    new MessageOffer.Builder()
                            .offer(Placeholders.AMOUNT, () -> postponedSize)
                            .offer(Placeholders.CURRENT_AMOUNT, () -> player.postponedPackets)
                            .build()));
        }
        if (cancelledSize >= ConfigurationOptions.Messages.PACKET_FILTER_MIN_CANCEL) {
            messageBuilder.append(" ").append(packetFilterCancelFormat.getMessage(
                    new MessageOffer.Builder()
                            .offer(Placeholders.AMOUNT, () -> cancelledSize)
                            .build()));
        }

        sendActionBar(player, messageBuilder.toString());
    }

    public void showQuarantinedAsyncWorker(HoneyPlayer player, long time) {
        if (!ConfigurationOptions.Messages.QUARANTINED_ASYNC_WORKER_ENABLED
                || !player.isBukkitReady()
                || !player.isOnlineDirect()) {
            return;
        }

        String message = quarantinedAsyncWorkerFormat.getMessage(
                new MessageOffer.Builder()
                        .offer(Placeholders.TIME, () -> time)
                        .build()
        );

        sendActionBar(player, message);
    }

    private static IStructureModifier<Object> chatSerializer;
    private static Class<Object> IchatBaseComponentClass;

    public void sendActionBar(HoneyPlayer player, String text) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            player.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
            return;
        }

        WrappedPacket<WrappedPlayOutChat> actionBarPacket = PacketFactory.createNewPacket(PacketTypes.Play.Server.PLAYER_CHAT, player);
        PacketContainer<WrappedPlayOutChat> packetContainer = actionBarPacket.getContainer();

        packetContainer.getBytes().writeField(0, (byte) 2);

        if (IchatBaseComponentClass == null) {
            IchatBaseComponentClass = NMS.getIChatBaseComponentClass();
        }
        if (chatSerializer == null) {
            chatSerializer = ReflectStructureCache.acquireStructure(NMS.getChatSerializerClass())
                    .withType(IchatBaseComponentClass)
                    .withTarget(null);
        }
        Object chatComponent = chatSerializer.invokeMethod(null, 0, "{\"text\":\"" + text + "\"}");
        packetContainer.getObjectsOf(IchatBaseComponentClass).writeField(0, chatComponent);

        try {
            HoneyPlugin.get().getPacketLibrary().getPacketManager().writeForcedPacket(player, actionBarPacket);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void notify(String notification) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {

            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().notify(notification);
        }
    }

    public void notify(HoneyPlayer player, String notification, ViolationService.HoneyKey honeyKey, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, ValidationContextContainer.DevelopmentData developmentData) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {

            if (!hasNotifyPermission(observer.getKey()) || !player.isOnlineDirect() || player.punished) {
                continue;
            }
            observer.getValue().notify(player, notification, setting, packetLimitHandler, developmentData, honeyKey);
        }
    }

    public void warn(HoneyPlayer player, String warning, ViolationService.HoneyKey honeyKey) {
        for (Map.Entry<CommandSender, NotificationListener> observer : observerMap.entrySet()) {

            if (!hasNotifyPermission(observer.getKey()) || player.punished) {
                continue;
            }

            observer.getValue().warn(player, warning, honeyKey);
        }
    }

    private interface NotificationListener {

        void notify(String notification);

        void notify(HoneyPlayer player, String notification, ConfigurationOptions.VlSetting setting, ViolationService.AbstractPacketLimitHandler packetLimitHandler, ValidationContextContainer.DevelopmentData developmentData, ViolationService.HoneyKey honeyKey);

        void warn(HoneyPlayer player, String notification, ViolationService.HoneyKey honeyKey);
    }
}
