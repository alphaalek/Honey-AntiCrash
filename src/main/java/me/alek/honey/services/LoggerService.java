package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.tracker.temp.TempTimedQueuedTracker;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.DecayingWindowCounter;
import me.alek.honey.utility.NumberConversions;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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
    private final TempTimedQueuedTracker packetRatePerQueueExecution = new TempTimedQueuedTracker(false);
    private final AtomicInteger currentQueueExecutionPacket = new AtomicInteger(0);


    private final FuzzyPacketAdapter packetLoggerListener = new FuzzyPacketAdapter() {

        @Override
        @SuppressWarnings("unchecked")
        public void onPacketReceive(HoneyPlayer player, PacketContainer packetContainer) {
            if (monitorManager.hasMonitorers()) {
                // notify monitor observers
                logToMonitors(player, packetContainer);

                long time = System.currentTimeMillis();
                // register to packet rate
                packetRate.computeIfAbsent(player.uuid, (d) -> new DecayingWindowCounter(1000, 2000, 10000))
                        .count(time);
                totalPacketRate.count(time);
            }

            // log the packet
            if (enabled) logPacket(packetContainer, player.name);
        }
    };

    public LoggerService() {
        reload();
    }

    public void reload() {
        enabled = ConfigurationOptions.PacketLogger.ENABLED;
        detailed = ConfigurationOptions.PacketLogger.DETAILED;
        debug = ConfigurationOptions.PacketLogger.DEBUG;
        ignoredPlayers = ConfigurationOptions.PacketLogger.IGNORED_PLAYERS;
        ignoredPackets = ConfigurationOptions.PacketLogger.IGNORED_PACKETS;

        boolean isRegistered = HoneyPlugin.get().getPacketLibrary().isFuzzyRegistered(packetLoggerListener);
        if (!isRegistered) {
            HoneyPlugin.get().getPacketLibrary().addFuzzyListeners(packetLoggerListener, ListenerPriority.LOWEST, () -> PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketState.PLAY, PacketBound.CLIENT));
        }
    }

    public Logger getLogger() {
        return HoneyPlugin.get().getPlugin().getLogger();
    }

    public int getPacketRate(HoneyPlayer player) {
        return packetRate.get(player.uuid).size(0);
    }

    public int getTotalPacketRate() {
        return totalPacketRate.size(0);
    }

    public String getTotalPacketRateGrade(int packetRate) {
        int prevRate = totalPacketRate.size(1, 2);
        return getPacketRateGrade(packetRate, prevRate);
    }

    public String getPacketRateGrade(HoneyPlayer player, int packetRate) {
        int prevRate = (int) (this.packetRate.get(player.uuid).size(1, 2) * 0.965);
        return getPacketRateGrade(packetRate, prevRate);
    }

    public String getPacketRateGrade(int rate, int prevRate) {
        double grade = ((double)rate) / (((double)prevRate) / 8);
        if (prevRate == 0) grade = 1;

        if (grade > 1.025) return "§a+" + NumberConversions.format(Math.abs(1 - grade) * 100) + "%";
        if (grade > 0.975) return "§e~0%";
        else return "§c-" + NumberConversions.format(Math.abs(1 - grade) * 100) + "%";
    }

    public void removePacketData(HoneyPlayer player) {
        recentPackets.remove(player.uuid);
        packetRate.remove(player.uuid);
    }

    public double getPacketRatePerQueueExecution(Timespan timespan) {
        return packetRatePerQueueExecution.getTotal(timespan) / packetRatePerQueueExecution.getLength(timespan);
    }

    public void registerQueueExecutionPacket() {
        currentQueueExecutionPacket.addAndGet(1);
    }

    public void registerQueueExecutionStop() {
        packetRatePerQueueExecution.add(currentQueueExecutionPacket.get(), System.currentTimeMillis());
        currentQueueExecutionPacket.set(0);
    }

    public void logToMonitors(HoneyPlayer player, PacketContainer<?> packet) {
        if (monitorManager.isMonitored(player) && ConfigurationOptions.Monitor.ENABLE_PACKET_LOGGER
                && (packet == null || !ConfigurationOptions.Monitor.IGNORED_PACKETS.contains(packet.getHandle().getClass().getSimpleName()))) {
            synchronized (this) {
                LinkedList<PacketTypeEnum> recentPackets = LoggerService.this.recentPackets.computeIfAbsent(player.uuid, (d) -> new LinkedList<>());

                PacketTypeEnum packetType = recentPackets.peek();
                PacketTypeEnum sentPacketType = Optional.ofNullable(packet).map(PacketContainer::getType).orElse(null);
                if (packetType != sentPacketType) {
                    if (packetType == null) packetType = sentPacketType;

                    int packetRate = getPacketRate(player);
                    monitorManager.sendPlayerInfo(player, "§8[§6§l!§8] §e" + player.name
                            + " §8(" + notificationService.formatPing(player) + "ms§8) §7"
                            + packetRate + " pps §8(" + getPacketRateGrade(player, packetRate) + "§8)"
                            + " §7Packet: " + packetType + (recentPackets.size() <= 1 ? "" : " §f(x" + recentPackets.size() + ")"));
                    recentPackets.clear();
                }
                if (packet != null) recentPackets.add(sentPacketType);
            }
        }
        monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_HONEY_HANDLE);
    }

    public void dumpPackets(HoneyPlayer player, List<PacketContainer<?>> packets) {
        getLogger().info("Packet dump of " + packets.size() + " packets from " + player.name);

        long time = System.currentTimeMillis();
        for (PacketContainer<?> packet : packets) {
            logPacket(packet, player.name, true, time, false);
        }
    }

    public void dumpAutoHeavyPacket(PacketContainer<?> packet) {
        getLogger().info("Auto dump for packet sent by " + packet.getPlayer().name + ": " + packet.getName()
                + " (" + packet.getEventloopStateTimings().getTimeForMillisFormatted(EventloopState.SERVER_HANDLE) + "ms) " + packet.getAll().readFieldEntries());
    }

    public <WP extends WrappedPacket<WP>> void logPacket(PacketContainer<WP> packetContainer, String playerName) {
        logPacket(packetContainer, playerName, this.detailed, 0, true);
    }

    public <WP extends WrappedPacket<WP>> void logPacket(PacketContainer<WP> packetContainer, String playerName, boolean detailed, long comparingTime, boolean canIgnore) {
        if (canIgnore && (ignoredPlayers.contains(playerName) || ignoredPackets.contains(packetContainer.getName()))) {
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append(packetContainer.getTime());

        if (comparingTime > packetContainer.getTime()) {
            message.append(" (" + (comparingTime - packetContainer.getTime()) + "ms ago)");
        }

        message.append(": " + playerName + " sent " + packetContainer.getName());
        if (detailed)
            message.append(": " + packetContainer.getAll().readFieldEntries());

        getLogger().info(message.toString());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }
}
