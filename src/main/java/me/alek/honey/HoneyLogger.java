package me.alek.honey;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.WrappedPacketFactoryCollection;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.counter.DecayingWindowCounter;
import me.alek.honey.util.statics.NumberUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class HoneyLogger {

    private static final Map<Class<?>, Class<?>> BOXED = new HashMap<Class<?>, Class<?>>() {{
        put(Integer.class, int.class);
        put(Float.class, float.class);
        put(Short.class, short.class);
        put(Byte.class, byte.class);
        put(Double.class, double.class);
        put(Long.class, long.class);
        put(Boolean.class, boolean.class);
        put(Character.class, char.class);
    }};
    private static final int MAX_PACKET_LOGGING_DATA = 5000;
    private static final Supplier<DecayingWindowCounter> DECAYING_WINDOW_COUNTER  = () -> new DecayingWindowCounter(1000, 2000, 10000);

    private boolean enabled;
    private boolean detailed;
    private boolean debug;
    private List<String> ignoredPlayers;
    private List<String> ignoredPackets;
    private final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();
    private final Notifications notificationService = HoneyPlugin.get().getNotificationService();
    private final Map<UUID, Queue<PacketType>> recentPackets = new HashMap<>();
    private final Map<UUID, DecayingWindowCounter> packetRate = new HashMap<>();
    private final DecayingWindowCounter totalPacketRate = new DecayingWindowCounter(1000, 2000, 10000);
    private final TempIntervalTracker packetRatePerQueueExecution = new TempIntervalTracker(false, false);
    private final AtomicInteger currentQueueExecutionPacket = new AtomicInteger(0);

    private final class PacketLoggerHolder<WP extends SourcedWrappedPacket<WP>> {

        private final PacketListenerAdapter<WP> packetLogger = new PacketListenerAdapter.InboundCommon<WP>() {

            @Override
            public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) {
                if (packet.isLogged()) {
                    return;
                }
                packet.log();

                if (monitorManager.hasMonitorers()) {
                    if (player.linkedBukkit()) {
                        long time = System.currentTimeMillis();
                        // register to packet rate
                        synchronized (packetRate) {
                            packetRate.computeIfAbsent(player.uuid, __ -> DECAYING_WINDOW_COUNTER.get())
                                    .count(time);
                        }
                        totalPacketRate.count(time);
                    }

                    // notify monitor observers
                    logToMonitors(player, packet);
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

    private final PacketLoggerHolder<?> holder = new PacketLoggerHolder<>();

    public HoneyLogger() {
        reload(true);
    }

    public void reload() {
        reload(false);
    }

    public void reload(boolean firstCall) {
        this.enabled = ConfigurationOptions.PacketLogger.ENABLED;
        this.detailed = ConfigurationOptions.PacketLogger.DETAILED;
        this.debug = ConfigurationOptions.PacketLogger.DEBUG;
        this.ignoredPlayers = ConfigurationOptions.PacketLogger.IGNORED_PLAYERS;
        this.ignoredPackets = ConfigurationOptions.PacketLogger.IGNORED_PACKETS;

        if (!firstCall) {
            // remove all previously registered loggers
            for (PacketContainer.PipelineState state : PacketContainer.PipelineState.values()) {
                HoneyPlugin.get().getPacketLibrary().removeListener(this.holder.packetLogger, state);
            }
        }

        List<PacketType> initialByteTypes = new ArrayList<>();
        List<PacketType> finalByteTypes = new ArrayList<>();
        List<PacketType> objectTypes = new ArrayList<>();
        for (PacketType type : PacketTypes.getTypesAvailableForBound(PacketBound.SERVER)) {
            if (WrappedPacketFactoryCollection.hasByteWrappedPacket(type, PacketContainer.PipelineState.INITIAL_BYTE)) {
                initialByteTypes.add(type);
            } else if (WrappedPacketFactoryCollection.hasByteWrappedPacket(type, PacketContainer.PipelineState.FINAL_BYTE)) {
                finalByteTypes.add(type);
            } else {
                // fallback to object wrapped
                objectTypes.add(type);
            }
        }

        HoneyPlugin.get().getPacketLibrary().addListener(this.holder.packetLogger, PacketContainer.PipelineState.INITIAL_BYTE, ListenerPriority.HIGHEST, initialByteTypes);
        HoneyPlugin.get().getPacketLibrary().addListener(this.holder.packetLogger, PacketContainer.PipelineState.FINAL_BYTE, ListenerPriority.HIGHEST, finalByteTypes);
        HoneyPlugin.get().getPacketLibrary().addListener(this.holder.packetLogger, PacketContainer.PipelineState.OBJECT, ListenerPriority.HIGHEST, objectTypes);
    }

    public Logger getLogger() {
        return HoneyPlugin.get().getPlugin().getLogger();
    }

    public int getPacketRate(HoneyPlayer player) {
        synchronized (this.packetRate) {
            return this.packetRate.getOrDefault(player.uuid, DecayingWindowCounter.EMPTY).size(0);
        }
    }

    public int getTotalPacketRate() {
        return this.totalPacketRate.size(0);
    }

    public String getTotalPacketRateGrade(int packetRate) {
        synchronized (this.packetRate) {
            int prevRate = this.totalPacketRate.size(1, 2);
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
        double grade = ((double) rate) / (((double) prevRate) / 8);
        if (prevRate == 0) grade = 1;

        if (grade > 1.025) return "§a+" + NumberUtils.format(Math.abs(1 - grade) * 100) + "%";
        if (grade > 0.975) return "§e~0%";
        else return "§c-" + NumberUtils.format(Math.abs(1 - grade) * 100) + "%";
    }

    public void removePacketData(HoneyPlayer player) {
        this.recentPackets.remove(player.uuid);
        this.packetRate.remove(player.uuid);
    }

    public double getPacketRatePerQueueExecution(Timespan timespan) {
        int length = this.packetRatePerQueueExecution.getLength(timespan);
        if (length == 0) {
            return 0;
        }

        return ((double) this.packetRatePerQueueExecution.getTotal(timespan)) / length;
    }

    public void registerQueueExecutionPacket() {
        this.currentQueueExecutionPacket.addAndGet(1);
    }

    public void registerQueueExecutionStop() {
        this.packetRatePerQueueExecution.add(currentQueueExecutionPacket.get());
        this.currentQueueExecutionPacket.set(0);
    }

    public void logToMonitors(HoneyPlayer player, PacketContainer<?> packet) {
        if (player.linkedBukkit() && this.monitorManager.isMonitored(player) && ConfigurationOptions.Monitor.ENABLE_PACKET_LOGGER
                && (packet == null || !ConfigurationOptions.Monitor.IGNORED_PACKETS.contains(packet.getType().toString()))) {
            synchronized (this) {
                Queue<PacketType> recentPackets = HoneyLogger.this.recentPackets.computeIfAbsent(player.uuid, (d) -> new LinkedList<>());

                PacketType packetType = recentPackets.peek();
                PacketType sentPacketType = Optional.ofNullable(packet).map(PacketContainer::getType).orElse(null);
                if (packetType != sentPacketType) {
                    if (packetType == null) packetType = sentPacketType;

                    int packetRate = getPacketRate(player);
                    this.monitorManager.sendPlayerInfo(player, "§8[§6§l!§8] §e" + player.name
                            + " §8(" + this.notificationService.formatPing(player) + "ms§8) §7"
                            + packetRate + " pps §8(" + getPacketRateGrade(player, packetRate) + "§8)"
                            + " §7Packet: " + packetType + (recentPackets.size() <= 1 ? "" : " §f(x" + recentPackets.size() + ")"));
                    recentPackets.clear();
                }

                if (packet != null) {
                    recentPackets.add(sentPacketType);
                }
            }
        }

        this.monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_HONEY_HANDLE);
    }

    public void dumpAutoHeavyPacket(PacketContainer<?> packet) {
        if (PacketTypes.isUnknown(packet.getType())) {
            return;
        }

        getLogger().info(
                "Auto dump for packet sent by "
                        + packet.getPlayerName() + ": "
                        + formatPacketType(packet.getType())
                        + " (" + packet.getEventloopStateTimings().getTimeForMillisFormatted(LifecycleState.SERVER_HANDLE) + "ms) "
                        + filterTooMuchData(formatPacketEntries(packet)));
    }

    public <WP extends SourcedWrappedPacket<WP>> void logPacket(PacketContainer<WP> packet, String playerName) {
        logPacket(packet, playerName, this.detailed, 0);
    }

    public <WP extends SourcedWrappedPacket<WP>> void logPacket(PacketContainer<WP> packet, String playerName, boolean detailed, long comparingTime) {
        if (this.ignoredPlayers.contains(playerName)
                || this.ignoredPackets.contains(packet.getType().toString())) {
            return;
        }

        StringBuilder message = new StringBuilder();
        if (comparingTime > packet.getTimeCreated()) {
            message.append("(")
                   .append(comparingTime - packet.getTimeCreated())
                   .append("ms ago) ");
        }
        message.append(playerName)
               .append(" sent ")
               .append(formatPacketType(packet.getType()))
               .append(" (")
               .append(packet.getAnyWrapped().getState().toString().toLowerCase().split("_")[0].charAt(0))
               .append(")");

        IDRegistry states = WrappedPacketFactoryCollection.getPipelineStates(packet.getType());
        if (packet.getAnyWrapped().isAbstract() && !states.hasID(packet.getAnyWrapped().getState().ordinal())) {
            message.append(" (invoked)");
        }

        if (detailed && !PacketTypes.isUnknown(packet.getType())) {
            message.append(" ")
                   .append(filterTooMuchData(formatPacketEntries(packet)));
        }

        getLogger().info(message.toString());
    }

    public String filterTooMuchData(Object data) {
        byte[] bytes = String.valueOf(data).getBytes();
        if (ConfigurationOptions.PacketLogger.FILTER_TOO_MUCH_DATA
                && bytes.length > MAX_PACKET_LOGGING_DATA) {
            return new String(Arrays.copyOfRange(bytes, 0, MAX_PACKET_LOGGING_DATA))
                    + " (!) Too much data (!)";
        }

        return data.toString();
    }

    public Map<String, Object> getPacketEntries(PacketContainer<?> packet) {
        if (PacketTypes.isUnknown(packet.getType())) {
            return new HashMap<>();
        }

        Map<String, Object> entries = new HashMap<>();
        for (Map.Entry<String, Object> entry : packet.getAnyWrapped().getProperties().entrySet()) {
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

    public String formatPacketType(PacketType type) {
        return type.toString() + (ConfigurationOptions.PacketLogger.EARLY && !type.getState().equals(PacketState.PLAY) ? "<" + type.getState().toString().toLowerCase() + ">" : "");
    }

    public String formatPacketEntries(PacketContainer<? extends WrappedPacket<?>> packet) {
        return formatPacketEntries(getPacketEntries(packet));
    }

    public String formatPacketEntries(Map<String, Object> entries) {
        if (entries.isEmpty()) {
            return "<empty>";
        }

        Iterator<Map.Entry<String, Object>> i = entries.entrySet().iterator();
        if (!i.hasNext()) {
            return "<empty>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ; ) {
            Map.Entry<String, Object> e = i.next();
            sb.append(e.getKey())
                    .append("=");
            try {
                Object val = e.getValue();
                sb.append(val.toString()
                                .replace("\t", "\\t")
                                .replace("\n", "\\n"))
                        .append("<")
                        .append(BOXED.getOrDefault(val.getClass(), val.getClass()).getSimpleName())
                        .append(">");
            } catch (Exception ignored) {
                sb.append("null");
            }

            if (!i.hasNext()) {
                sb.append('}');
                break;
            }

            sb.append(", ");
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
