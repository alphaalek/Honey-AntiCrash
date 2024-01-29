package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.NetProtocol;
import me.alek.honey.tracker.TempTimedQueuedTracker;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.utility.DecayingWindowCounter;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggerService {

    private boolean enabled;
    private boolean detailed;
    private boolean debug;
    private List<String> ignoredPlayers;
    private List<String> ignoredPackets;
    private final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();
    private final NotificationService notificationService = HoneyPlugin.get().getNotificationService();

    private final Map<UUID, LinkedList<PacketTypeEnum>> recentPackets = new HashMap<>();
    private final Map<UUID, DecayingWindowCounter> packetRate = new HashMap<>();
    private final DecayingWindowCounter totalPacketRate = new DecayingWindowCounter(1000, 2000, 10000);
    private final TempTimedQueuedTracker packetRatePerQueueExecution = new TempTimedQueuedTracker();
    private final AtomicInteger currentQueueExecutionPacket = new AtomicInteger(0);


    private final FuzzyPacketAdapter packetLoggerListener = new FuzzyPacketAdapter() {

        @Override
        @SuppressWarnings("unchecked")
        public void onPacketReceive(Player player, PacketContainer packetContainer) {
            // notify monitor observers
            logToMonitors(player, packetContainer);

            long time = System.currentTimeMillis();
            // register to packet rate
            packetRate.computeIfAbsent(player.getUniqueId(), (d) -> new DecayingWindowCounter(1000, 2000, 10000))
                    .count(time);
            totalPacketRate.count(time);

            // log the packet
            if (enabled) logPacket(packetContainer, player.getName());
        }
    };

    public void logToMonitors(Player player, PacketContainer packetContainer) {
        if (monitorManager.isMonitored(player)) {
            synchronized (this) {
                LinkedList<PacketTypeEnum> recentPackets = LoggerService.this.recentPackets.computeIfAbsent(player.getUniqueId(), (d) -> new LinkedList<>());

                PacketTypeEnum packetType;
                PacketTypeEnum sentPacketType = Optional.ofNullable(packetContainer).map(PacketContainer::getType).orElse(null);
                if ((packetType = recentPackets.peek()) != sentPacketType) {
                    if (packetType == null) packetType = sentPacketType;

                    int packetRate = getPacketRate(player);
                    monitorManager.sendPlayerInfo(player, "§8[§6§l!§8] §e" + player.getName()
                            + " §8(" + notificationService.formatPing(player) + "ms§8) §7"
                            + packetRate + " pps §8(" + getPacketRateGrade(player, packetRate) + "§8)"
                            + " §7Packet: " + packetType + (recentPackets.size() <= 1 ? "" : " §f(x" + recentPackets.size() + ")"));
                    recentPackets.clear();
                }
                if (packetContainer != null) recentPackets.add(sentPacketType);
            }
        }
    }

    public void removePacketData(Player player) {
        recentPackets.remove(player.getUniqueId());
        packetRate.remove(player.getUniqueId());
    }

    public int getPacketRate(Player player) {
        return packetRate.get(player.getUniqueId()).size(0);
    }

    public int getTotalPacketRate() {
        return totalPacketRate.size(0);
    }

    public String getTotalPacketRateGrade(int packetRate) {
        int prevRate = totalPacketRate.size(1, 2);
        return getPacketRateGrade(packetRate, prevRate);
    }

    public String getPacketRateGrade(Player player, int packetRate) {
        int prevRate = (int) (this.packetRate.get(player.getUniqueId()).size(1, 2) * 0.965);
        return getPacketRateGrade(packetRate, prevRate);
    }

    public String getPacketRateGrade(int rate, int prevRate) {
        double grade = ((double)rate) / (((double)prevRate) / 8);
        if (prevRate == 0) grade = 1;

        if (grade > 1.025) return "§a+" + NumberConversions.format(Math.abs(1 - grade) * 100) + "%";
        if (grade > 0.975) return "§e~0%";
        else return "§c-" + NumberConversions.format(Math.abs(1 - grade) * 100) + "%";
    }

    public double getPacketRatePerQueueExecution(Timespan timespan) {
        return packetRatePerQueueExecution.getTotal(timespan) / packetRatePerQueueExecution.getLength(timespan);
    }

    public void registerQueueExecutionPacket() {
        currentQueueExecutionPacket.addAndGet(1);
    }

    public void registerQueueExecutionStop() {
        packetRatePerQueueExecution.add(currentQueueExecutionPacket.get());
        currentQueueExecutionPacket.set(0);
    }

    public LoggerService() {
        reload();
    }

    public enum LoggingStrategy {
        FLAG, VIOLATE
    }

    public void reload() {
        enabled = ConfigurationOptions.PacketLogger.ENABLED;
        detailed = ConfigurationOptions.PacketLogger.DETAILED;
        debug = ConfigurationOptions.PacketLogger.DEBUG;
        ignoredPlayers = ConfigurationOptions.PacketLogger.IGNORED_PLAYERS;
        ignoredPackets = ConfigurationOptions.PacketLogger.IGNORED_PACKETS;

        boolean isRegistered = HoneyPlugin.get().getPacketLibrary().isFuzzyRegistered(packetLoggerListener);
        if (!isRegistered) {
            HoneyPlugin.get().getPacketLibrary().addFuzzyListeners(packetLoggerListener, ListenerPriority.HIGHEST, () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, NetProtocol.getProtocol()));
        }
    }

    public void logDebug(LoggingStrategy strategy, Object handle, String playerName, String violationName) {
        if (!debug) return;
        if (ignoredPlayers.contains(playerName) || ignoredPackets.contains(handle.getClass().getSimpleName())) {
            return;
        }
        String logType = (strategy == LoggingStrategy.FLAG) ? "flagged" : "violated";

        if (logType.equals("flagged")) {
            if (violationName.equals("Netty.G") || violationName.equals("Netty.F") || violationName.equals("Rotation.C") || violationName.contains("Flood")) return;
        }

        HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: " + logType + " " + playerName + " for " + violationName);
    }

    public <WP extends WrappedPacket<WP>> void logPacket(PacketContainer<WP> packetContainer, String playerName) {
        if (ignoredPlayers.contains(playerName) || ignoredPackets.contains(packetContainer.getHandle().getClass().getSimpleName())) {
            return;
        }
        StringBuilder message = new StringBuilder()
                .append(playerName)
                .append(" sent ")
                .append(packetContainer.getHandle().getClass().getSimpleName());
        if (detailed) {
            message
                .append(": ")
                .append(packetContainer.getAll().readFieldValues());
        }
        HoneyPlugin.get().getPlugin().getLogger().info(message.toString());
    }

    public void logPunish(String playerName, String violationName) {
        if (!debug) return;
        HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: Kicking " + playerName + " for " + violationName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public List<String> getIgnoredPlayers() {
        return ignoredPlayers;
    }

    public List<String> getIgnoredPackets() {
        return ignoredPackets;
    }
}
