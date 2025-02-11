package me.alek.honey;

import com.google.common.collect.Sets;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import me.alek.honey.compat.Paper;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.impl.play.server.WrappedPlayOutChat;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.tracker.TimedManualHandleTracker;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.TPSProvider;
import me.alek.honey.util.counter.DecayingListeningCounter;
import me.alek.honey.util.messages.MessageFormat;
import me.alek.honey.util.messages.MessageOffer;
import me.alek.honey.util.messages.Placeholders;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ViolationManagement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.management.NotificationListener;
import java.util.*;
import java.util.logging.Level;

public class Notifications {

    public static String PREFIX;

    private final Map<CommandSender, NotificationListener> observerMap = new HashMap<>();
    private final List<CommandSender> untoggledlist = new ArrayList<>();
    private final Map<UUID, DecayingListeningCounter> cancelledPackets = new HashMap<>();
    private final Map<UUID, DecayingListeningCounter> postponedPackets = new HashMap<>();
    private MessageFormat violationFormat;
    private MessageFormat punishFormat;
    private String packetFilterPre;
    private MessageFormat packetFilterCancelFormat;
    private MessageFormat packetFilterPostponeFormat;

    public Notifications() {
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

                    if (!hasNotifyPermission(player) && isSubscribed(player)) {
                        unsubscribe(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(HoneyPlugin.get().getPlugin(), 0L, 120L);

        reload();
    }

    public void setup(HoneyPlayer player) {
        if (hasNotifyPermission(player.bukkit) && !isSubscribed(player.bukkit) && !isUntoggled(player.bukkit)) {
            subscribe(player.bukkit);
        }
    }

    public void reload() {
        PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX);

        this.violationFormat = MessageFormat.load(ConfigurationOptions.Messages.VIOLATION_MESSAGE);
        this.punishFormat = MessageFormat.load(ConfigurationOptions.Messages.PUNISH_MESSAGE);

        this.packetFilterPre = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.Messages.PACKET_FILTER_PRE);
        this.packetFilterCancelFormat = MessageFormat.load(ConfigurationOptions.Messages.PACKET_FILTER_CANCEL);
        this.packetFilterPostponeFormat = MessageFormat.load(ConfigurationOptions.Messages.PACKET_FILTER_POSTPONE);
    }

    public String getPrefix() {
        return PREFIX + "§r";
    }

    public boolean hasNotifyPermission(CommandSender observer) {
        return observer.hasPermission(ConfigurationOptions.General.NOTIFY_PERMISSION);
    }

    public boolean isUntoggled(CommandSender observer) {
        return this.untoggledlist.contains(observer);
    }

    public boolean isSubscribed(CommandSender observer) {
        return this.observerMap.containsKey(observer);
    }

    private String getAppropriateMessage(CommandSender observer, String message) {
        if (observer instanceof Player) return message;

        return ChatColor.stripColor(message);
    }

    public void subscribe(final CommandSender observer) {
        this.observerMap.put(observer, new Notifications.NotificationListener() {
            @Override
            public void notify(String notification) {
                observer.sendMessage(getAppropriateMessage(observer, notification));
            }

            @SuppressWarnings("deprecation")
            @Override
            public void notify(HoneyPlayer player,
                               String type,
                               ConfigurationOptions.ViolationSetting setting,
                               ViolationManagement.AbstractPacketLimitHandler packetLimitHandler,
                               ValidationPipelineContext.DevelopmentData developmentData,
                               HoneyKey honeyKey) {
                MessageOffer offer = getOffer(player, type, honeyKey);
                offer = offer.builder()
                        .offer(Placeholders.CONTEXT, packetLimitHandler::format)
                        .build();
                String message = getMessage(violationFormat, offer);

                if (developmentData == null || developmentData.getDataSet().isEmpty()
                        || !(observer instanceof Player)) {
                    observer.sendMessage(message);
                    return;
                }
                Player observerPlayer = (Player) observer;

                StringBuilder hoverable = new StringBuilder();
                for (String dataSetEntry : developmentData.getDataSet()) {
                    hoverable.append(dataSetEntry).append("\n");
                }
                String hoverText = hoverable.substring(0, hoverable.length() - 1);

                if (Paper.IS_PAPER) {
                    Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(message)
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.hoverEvent(net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT, LegacyComponentSerializer.legacyAmpersand().deserialize(hoverText)));
                    observerPlayer.sendMessage(text);
                } else {
                    TextComponent text = new TextComponent(message);
                    text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(hoverText)}));
                    observerPlayer.spigot().sendMessage(text);
                }
            }

            @Override
            public void warn(HoneyPlayer player, String type, HoneyKey honeyKey) {
                observer.sendMessage(getMessage(punishFormat, getOffer(player, type, honeyKey)));
            }
        });
    }

    public MessageOffer getOffer(HoneyPlayer player, String type, HoneyKey honeyKey) {
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
        this.observerMap.remove(observer);

        if (observer instanceof Player) {
            this.cancelledPackets.remove(((Player) observer).getUniqueId());
        }
    }

    public void untoggle(CommandSender observer) {
        unsubscribe(observer);
        this.untoggledlist.add(observer);
    }

    public void toggle(CommandSender observer) {
        subscribe(observer);
        this.untoggledlist.remove(observer);
    }

    public int getPing(HoneyPlayer player) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17)) {
            IStructureModifier<Integer> craftPlayerModifier =
                    ReflectStructure.Provider.acquireStructure(MinecraftReflection.getCraftPlayerClass())
                            .withInts().withTarget(player.bukkit);

            return craftPlayerModifier.invokeMethod(null, "getPing", 0);
        }
        IStructureModifier<Object> craftPlayerModifier =
                ReflectStructure.Provider.acquireStructure(MinecraftReflection.getCraftPlayerClass())
                        .withType(MinecraftReflection.getEntityPlayerClass()).withTarget(player.bukkit);

        Object entityPlayer = craftPlayerModifier.invokeMethod(null, "getHandle", 0);

        IStructureModifier<Integer> entityPlayerModifier =
                ReflectStructure.Provider.acquireStructure(entityPlayer.getClass())
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

    public void applyQuotients(HoneyPlayer player, ValidationPipelineContext.DevelopmentData developmentData) {
        TimedManualHandleTracker<Long, Double, HoneyPlayer> serverThreadHandleTracker = HoneyPlugin.get().getExecutionQueueProxy();
        TimedManualHandleTracker<Long, Double, HoneyPlayer> nettyThreadHandleTracker = HoneyPlugin.get().getNettyPacketTracker();

        String serverThreadPoint = serverThreadHandleTracker.has(player) ? formatPercentages(serverThreadHandleTracker.getStatistics(player)) : "No data!";
        String nettyThreadPoint = nettyThreadHandleTracker.has(player) ? formatPercentages(nettyThreadHandleTracker.getStatistics(player)) : "No data!";

        developmentData.setPoint(8, "STQ", serverThreadPoint);
        developmentData.setPoint(9, "NTQ", nettyThreadPoint);

        developmentData.addInternalData("TPS=" + ChatColor.stripColor(TPSProvider.getTPS(Timespan.FIVE_SECOND)));
        developmentData.addInternalData("Ping=" + HoneyPlugin.get().getNotificationService().getPing(player));
    }

    public void showPacketFilterActionBar(HoneyPlayer player, boolean increment, boolean incrementCancelled) {
        if (!ConfigurationOptions.Messages.PACKET_FILTER_ENABLED
                || !player.linkedBukkit()
                || !player.isOnlineBukkit()) {
            return;
        }

        DecayingListeningCounter cancelled = this.cancelledPackets.computeIfAbsent(player.uuid,
                (d) -> new DecayingListeningCounter(() -> (long) 5000));
        DecayingListeningCounter postponed = this.postponedPackets.computeIfAbsent(player.uuid,
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

        if ((postponedSize < ConfigurationOptions.Messages.PACKET_FILTER_MIN_POSTPONE || postponedSize == 0)
                && (cancelledSize < ConfigurationOptions.Messages.PACKET_FILTER_MIN_CANCEL || cancelledSize == 0)) {
            return;
        }

        StringBuilder messageBuilder = new StringBuilder(this.packetFilterPre);
        if (postponedSize >= ConfigurationOptions.Messages.PACKET_FILTER_MIN_POSTPONE && postponedSize != 0) {
            messageBuilder.append(" ").append(this.packetFilterPostponeFormat.getMessage(
                    new MessageOffer.Builder()
                            .offer(Placeholders.AMOUNT, () -> postponedSize)
                            .offer(Placeholders.CURRENT_AMOUNT, () -> player.postponedPackets)
                            .build()));
        }
        if (cancelledSize >= ConfigurationOptions.Messages.PACKET_FILTER_MIN_CANCEL && cancelledSize != 0) {
            messageBuilder.append(" ").append(this.packetFilterCancelFormat.getMessage(
                    new MessageOffer.Builder()
                            .offer(Placeholders.AMOUNT, () -> cancelledSize)
                            .build()));
        }

        String text = messageBuilder.toString();
        if (!text.isEmpty()) {
            sendActionBar(player, text);
        }
    }

    private static IStructureModifier<Object> chatSerializer;

    @SuppressWarnings("deprecation")
    public void sendActionBar(HoneyPlayer player, String text) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            if (Paper.IS_PAPER) {
                player.bukkit.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            } else {
                player.bukkit.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
            }
        } else {
            try {
                ObjectWrappedPacket<WrappedPlayOutChat> actionBarPacket = PacketFactory.createNewPacket(PacketTypes.Play.Server.CHAT, player);
                PacketContainer<WrappedPlayOutChat> packetContainer = actionBarPacket.getContainer();

                packetContainer.getStructure().getBytes().writeField(0, (byte) 2);

                if (chatSerializer == null) {
                    chatSerializer = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getChatSerializerClass())
                            .withType(MinecraftReflection.getIChatBaseComponentClass())
                            .withTarget(null);
                }
                Object chatComponent = chatSerializer.invokeMethod(null, 0, "{\"text\":\"" + text + "\"}");
                packetContainer.getStructure().getObjectsOf(MinecraftReflection.getIChatBaseComponentClass()).writeField(0, chatComponent);

                HoneyPlugin.get().getPacketLibrary().getChannelManager().writePacket(player, actionBarPacket);
            } catch (Exception ex) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to send action bar packet", ex);
            }
        }
    }

    public void notify(String notification) {
        for (Map.Entry<CommandSender, NotificationListener> observer : Sets.newHashSet(this.observerMap.entrySet())) {
            if (!hasNotifyPermission(observer.getKey())) {
                continue;
            }
            observer.getValue().notify(notification);
        }
    }

    public void notify(HoneyPlayer player, String notification, HoneyKey honeyKey, ConfigurationOptions.ViolationSetting setting, ViolationManagement.AbstractPacketLimitHandler packetLimitHandler, ValidationPipelineContext.DevelopmentData developmentData) {
        for (Map.Entry<CommandSender, NotificationListener> observer : Sets.newHashSet(this.observerMap.entrySet())) {
            if (!hasNotifyPermission(observer.getKey())
                    || !player.isStreamActive()) {
                continue;
            }

            observer.getValue().notify(player, notification, setting, packetLimitHandler, developmentData, honeyKey);
        }
    }

    public void warn(HoneyPlayer player, String warning, HoneyKey honeyKey) {
        for (Map.Entry<CommandSender, NotificationListener> observer : Sets.newHashSet(this.observerMap.entrySet())) {
            if (!hasNotifyPermission(observer.getKey())
                    || !player.isStreamActive()) {
                continue;
            }

            observer.getValue().warn(player, warning, honeyKey);
        }
    }

    private interface NotificationListener {

        void notify(String notification);

        void notify(HoneyPlayer player, String notification, ConfigurationOptions.ViolationSetting setting, ViolationManagement.AbstractPacketLimitHandler packetLimitHandler, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey honeyKey);

        void warn(HoneyPlayer player, String notification, HoneyKey honeyKey);
    }
}
