package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import me.alek.honey.util.counter.DecayingWindowCounter;
import me.alek.honey.util.statics.NumberUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoggerService {

    private static final int MAX_PACKET_LOGGING_DATA = 5000;

    private boolean enabled;
    private boolean detailed;
    private boolean debug;
    private List<String> ignoredPlayers;
    private List<String> ignoredPackets;
    private final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();
    private final NotificationService notificationService = HoneyPlugin.get().getNotificationService();
    private final Map<UUID, Queue<PacketType>> recentPackets = new HashMap<>();
    private final Map<UUID, DecayingWindowCounter> packetRate = new HashMap<>();
    private final DecayingWindowCounter totalPacketRate = new DecayingWindowCounter(1000, 2000, 10000);
    private final TempQueuedTracker packetRatePerQueueExecution = new TempQueuedTracker(false, false);
    private final AtomicInteger currentQueueExecutionPacket = new AtomicInteger(0);

    private final class PacketLoggerHolder<WP extends WrappedPacket<WP>> {

        private final PacketAdapter<WP> packetLogger = new PacketAdapter<WP>() {

            @Override
            @SuppressWarnings("unchecked")
            public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) {
                if (monitorManager.hasMonitorers()) {
                    // notify monitor observers
                    logToMonitors(player, packet);

                    if (player.isBukkitReady()) {
                        long time = System.currentTimeMillis();
                        // register to packet rate
                        synchronized (packetRate) {
                            packetRate.computeIfAbsent(player.uuid, (d) -> new DecayingWindowCounter(1000, 2000, 10000))
                                    .count(time);
                        }
                        totalPacketRate.count(time);
                    }
                }

                // log the packet
                if (enabled) {
                    if (!packet.getState().equals(PacketState.PLAY) && !ConfigurationOptions.PacketLogger.EARLY) {
                        return;
                    }

                    logPacket(packet, packet.getPlayerName());
                }
            }
        };
    }

    private final PacketLoggerHolder<? extends WrappedPacket<?>> holder = new PacketLoggerHolder<>();

    public LoggerService() {
        reload();
    }

    public void reload() {
        enabled = ConfigurationOptions.PacketLogger.ENABLED;
        detailed = ConfigurationOptions.PacketLogger.DETAILED;
        debug = ConfigurationOptions.PacketLogger.DEBUG;
        ignoredPlayers = ConfigurationOptions.PacketLogger.IGNORED_PLAYERS;
        ignoredPackets = ConfigurationOptions.PacketLogger.IGNORED_PACKETS;

        boolean isRegistered = HoneyPlugin.get().getPacketLibrary().isRegistered(holder.packetLogger);
        if (!isRegistered) {
            HoneyPlugin.get().getPacketLibrary().addListeners(holder.packetLogger, ListenerPriority.HIGHEST, PacketTypes.getTypesAvailableForBound(PacketBound.SERVER).stream().collect(Collectors.toList()));
        }
    }

    public Logger getLogger() {
        return HoneyPlugin.get().getPlugin().getLogger();
    }

    public int getPacketRate(HoneyPlayer player) {
        synchronized (packetRate) {
            return packetRate.getOrDefault(player.uuid, DecayingWindowCounter.EMPTY).size(0);
        }
    }

    public int getTotalPacketRate() {
        return totalPacketRate.size(0);
    }

    public String getTotalPacketRateGrade(int packetRate) {
        synchronized (this.packetRate) {
            int prevRate = totalPacketRate.size(1, 2);
            return getPacketRateGrade(packetRate, prevRate);
        }
    }

    public String getPacketRateGrade(HoneyPlayer player, int packetRate) {
        synchronized (this.packetRate) {
            int prevRate = (int) (this.packetRate.getOrDefault(player.uuid, DecayingWindowCounter.EMPTY).size(1, 2) * 0.965);
            return getPacketRateGrade(packetRate, prevRate);
        }
    }

    public String getPacketRateGrade(int rate, int prevRate) {
        double grade = ((double)rate) / (((double)prevRate) / 8);
        if (prevRate == 0) grade = 1;

        if (grade > 1.025) return "§a+" + NumberUtils.format(Math.abs(1 - grade) * 100) + "%";
        if (grade > 0.975) return "§e~0%";
        else return "§c-" + NumberUtils.format(Math.abs(1 - grade) * 100) + "%";
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
        packetRatePerQueueExecution.add(currentQueueExecutionPacket.get());
        currentQueueExecutionPacket.set(0);
    }

    public void logToMonitors(HoneyPlayer player, PacketContainer<?> packet) {
        if (player.isBukkitReady() && monitorManager.isMonitored(player) && ConfigurationOptions.Monitor.ENABLE_PACKET_LOGGER
                && (packet == null || !ConfigurationOptions.Monitor.IGNORED_PACKETS.contains(packet.getRaw().getClass().getSimpleName()))) {
            synchronized (this) {
                Queue<PacketType> recentPackets = LoggerService.this.recentPackets.computeIfAbsent(player.uuid, (d) -> new LinkedList<>());

                PacketType packetType = recentPackets.peek();
                PacketType sentPacketType = Optional.ofNullable(packet).map(PacketContainer::getType).orElse(null);
                if (packetType != sentPacketType) {
                    if (packetType == null) packetType = sentPacketType;

                    int packetRate = getPacketRate(player);
                    monitorManager.sendPlayerInfo(player, "§8[§6§l!§8] §e" + player.name
                            + " §8(" + notificationService.formatPing(player) + "ms§8) §7"
                            + packetRate + " pps §8(" + getPacketRateGrade(player, packetRate) + "§8)"
                            + " §7Packet: " + packetType + (recentPackets.size() <= 1 ? "" : " §f(x" + recentPackets.size() + ")"));
                    recentPackets.clear();
                }

                if (packet != null) {
                    recentPackets.add(sentPacketType);
                }
            }
        }

        monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_HONEY_HANDLE);
    }

    public void dumpAutoHeavyPacket(PacketContainer<?> packet) {
        if (PacketTypes.isUnknown(packet.getType())) {
            return;
        }

        getLogger().info(
                "Auto dump for packet sent by "
                + packet.getPlayerName() + ": "
                + packet.getType()
                + " (" + packet.getEventloopStateTimings().getTimeForMillisFormatted(EventloopState.SERVER_HANDLE) + "ms) "
                + filterTooMuchData(packet.getAll().readFieldEntries()));
    }

    public <WP extends WrappedPacket<WP>> void logPacket(PacketContainer<WP> packetContainer, String playerName) {
        logPacket(packetContainer, playerName, this.detailed, 0);
    }

    public <WP extends WrappedPacket<WP>> void logPacket(PacketContainer<WP> packetContainer, String playerName, boolean detailed, long comparingTime) {
        if (ignoredPlayers.contains(playerName)
                || ignoredPackets.contains(packetContainer.getRaw().getClass().getSimpleName())) {
            return;
        }

        StringBuilder message = new StringBuilder();
        if (comparingTime > packetContainer.getTime()) {
            message.append("(" + (comparingTime - packetContainer.getTime()) + "ms ago): ");
        }
        message.append(playerName + " sent " + packetContainer.getType());

        if (detailed && !PacketTypes.isUnknown(packetContainer.getType())) {
            message.append(": " + filterTooMuchData(formatPacketEntries(packetContainer)));
        }

        getLogger().info(message.toString());
    }

    public String filterTooMuchData(Object data) {
        if (ConfigurationOptions.PacketLogger.FILTER_TOO_MUCH_DATA
                && data.toString().getBytes().length > MAX_PACKET_LOGGING_DATA) {
            StringBuilder newData = new StringBuilder();
            String source = data.toString();
            int c = 0, i = 0;
            while (c < MAX_PACKET_LOGGING_DATA) {
                String str = String.valueOf(source.charAt(i++));
                newData.append(str);
                c += str.getBytes().length;
            }

            newData.append(" (!) Too much data (!)");
            return newData.toString();
        }

        return data.toString();
    }

    public Map<String, Object> getPacketEntries(PacketContainer<? extends WrappedPacket<?>> packet) {
        if (PacketTypes.isUnknown(packet.getType())) {
            return new HashMap<>();
        }

        Map<String, Object> entries = new HashMap<>();
        for (Map.Entry<String, Object> entry : packet.getAll().readFieldEntries().entrySet()) {
            if (ConfigurationOptions.PacketLogger.FORMAT_ARRAYS
                    && entry.getValue() != null
                    && entry.getValue().getClass().isArray()) {
                entries.put(entry.getKey(), formatArray(entry.getValue()));
            } else {
                entries.put(entry.getKey(), entry.getValue());
            }
        }

        return entries;
    }

    public String formatPacketEntries(PacketContainer<? extends WrappedPacket<?>> packet) {
        Map<String, Object> entries = getPacketEntries(packet);
        if (entries.isEmpty()) {
            return "<empty>";
        }

        Iterator<Map.Entry<String, Object>> i = entries.entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Map.Entry<String, Object> e = i.next();
            sb.append(e.getKey());
            sb.append('=');
            try {
                sb.append(e.getValue());
            } catch (Exception ex) {
                sb.append("null");
            }

            if (!i.hasNext()) {
                sb.append('}');
                break;
            }

            sb.append(',').append(' ');
        }

        return sb.toString();
    }

    private String formatArray(Object val) {
        if (val instanceof Object[]) {
            return Arrays.toString((Object[]) val);
        } else if (val instanceof int[]) {
            return Arrays.toString((int[]) val);
        } else if (val instanceof byte[]) {
            return Arrays.toString((byte[]) val);
        } else if (val instanceof char[]) {
            return Arrays.toString((char[]) val);
        } else if (val instanceof long[]) {
            return Arrays.toString((long[]) val);
        } else if (val instanceof float[]) {
            return Arrays.toString((float[]) val);
        } else if (val instanceof short[]) {
            return Arrays.toString((short[]) val);
        } else if (val instanceof double[]) {
            return Arrays.toString((double[]) val);
        } else if (val instanceof boolean[]) {
            return Arrays.toString((boolean[]) val);
        }
        return val.toString();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }
}
