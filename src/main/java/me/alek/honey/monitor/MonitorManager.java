package me.alek.honey.monitor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MonitorManager {

    public static final Monitorer SERVER_MONITOR = new ServerMonitorer();

    boolean packetsMonitored; // cache the value
    private final Set<Monitorer> packetMonitorers = new HashSet<>();
    private final Map<Monitorer, List<MonitorObserver>> monitorObservers = new HashMap<>();
    private final Map<UUID, Monitorer> playerMonitorerMap = new HashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public MonitorManager() {
        schedule();
    }

    public void schedule() {
        this.executorService.scheduleAtFixedRate(() -> {
            try {
                for (Monitorer monitorer : this.monitorObservers.keySet()) {
                    if (monitorer instanceof ScheduledMonitorer) {
                        ScheduledMonitorer scheduledMonitorer = (ScheduledMonitorer) monitorer;

                        String message = scheduledMonitorer.getMessage();
                        for (MonitorObserver observer : this.monitorObservers.get(monitorer)) {
                            observer.sendInfo(message);
                        }
                    }
                }
            } catch (Exception ex) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Error occurred for monitor tick", ex);
            }
        }, 0L, ConfigurationOptions.Monitor.MESSAGE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public Monitorer getMonitorer(HoneyPlayer player) {
        return this.playerMonitorerMap.computeIfAbsent(player.uuid, (d) -> new PlayerMonitorer(player));
    }

    public boolean isMonitored(HoneyPlayer player) {
        return this.monitorObservers.containsKey(getMonitorer(player));
    }

    public boolean isPacketsMonitored() {
        return this.packetsMonitored;
    }

    public boolean isServerMonitored() {
        return this.monitorObservers.containsKey(SERVER_MONITOR) && !this.monitorObservers.get(SERVER_MONITOR).isEmpty();
    }

    public void removeFromMonitors(HoneyPlayer player) {
        this.monitorObservers.remove(getMonitorer(player));
        this.playerMonitorerMap.remove(player.uuid);
        removeMonitor(player.bukkit);
    }

    public boolean hasMonitorers() {
        return !this.monitorObservers.isEmpty();
    }

    public Monitorer removeIfHasMonitor(CommandSender sender, boolean onlyStop) {
        Monitorer monitorer = removeMonitor(sender);
        if (monitorer != null) {
            sender.sendMessage(Lang.getFormatted("monitor.stopping", monitorer.getName()));
        } else if (onlyStop) {
            sender.sendMessage(Lang.getFormatted("monitor.stop.no.active"));
        }

        return monitorer;
    }

    public <WP extends SourcedWrappedPacket<WP>> void setupPacketMonitor(CommandSender sender, FlagRunner.Env<WP> flags) {
        removeIfHasMonitor(sender, false);

        Monitorer monitorer = new PacketMonitorer<>(flags);
        setupMonitor(sender, monitorer);
        this.packetMonitorers.add(monitorer);
        this.packetsMonitored = true;
    }

    public void setupPlayerMonitor(CommandSender sender, HoneyPlayer player) {
        if (removeIfHasMonitor(sender, false) == getMonitorer(player)) {
            return;
        }

        setupMonitor(sender, getMonitorer(player));
    }

    public void setupServerMonitor(CommandSender sender) {
        if (removeIfHasMonitor(sender, false) == SERVER_MONITOR) {
            return;
        }

        setupMonitor(sender, SERVER_MONITOR);
    }

    public void sendServerInfo(String message) {
        sendInfo(SERVER_MONITOR, message);
    }

    public void sendPlayerInfo(HoneyPlayer player, String message) {
        sendInfo(getMonitorer(player), message);
    }

    public void sendInfo(Monitorer monitorer, String message) {
        if (!this.monitorObservers.containsKey(monitorer)) return;

        for (MonitorObserver observer : this.monitorObservers.get(monitorer)) {
            observer.sendInfo(message);
        }
    }

    public void sendPacketInfo(PacketContainer<?> packet, SendProperty property) {
        if (this.packetMonitorers.isEmpty()) {
            return;
        }

        for (Monitorer packetMonitorer : this.packetMonitorers) {
            if (!(packetMonitorer instanceof IndependentMonitorer)) {
                return;
            }

            @SuppressWarnings("unchecked")
            IndependentMonitorer<PacketContainer<?>> independentMonitorer = (IndependentMonitorer<PacketContainer<?>>) packetMonitorer;
            if (!independentMonitorer.getSendProperties().contains(property)) {
                continue;
            }

            String message = independentMonitorer.getMessage(packet);
            if (message == null) {
                continue;
            }

            for (MonitorObserver observer : this.monitorObservers.get(independentMonitorer)) {
                observer.sendInfo(message);
            }
        }
    }

    public void setupMonitor(CommandSender sender, Monitorer monitorer) {
        List<MonitorObserver> observers = this.monitorObservers.computeIfAbsent(monitorer, __ -> new ArrayList<>());
        observers.add(new MonitorObserver() {
            @Override
            public CommandSender getSender() {
                return sender;
            }

            @Override
            public void sendInfo(String message) {
                sender.sendMessage(message);
            }
        });

        sender.sendMessage(Lang.getFormatted("monitor.start", monitorer.getName()));
    }

    public Monitorer removeMonitor(CommandSender sender) {
        for (Monitorer monitorer : Sets.newHashSet(this.monitorObservers.keySet())) {

            List<MonitorObserver> observers = this.monitorObservers.get(monitorer);
            for (MonitorObserver observer : Lists.newArrayList(observers)) {

                if (observer.getSender().equals(sender)) {
                    observers.remove(observer);
                    if (observers.isEmpty()) {
                        this.monitorObservers.remove(monitorer);
                    }
                    if (monitorer.getType() == MonitorType.PACKETS) {
                        this.packetMonitorers.remove(monitorer);
                    }
                    this.packetsMonitored = !this.packetMonitorers.isEmpty();

                    return monitorer;
                }
            }
        }

        return null;
    }

    private interface MonitorObserver {

        CommandSender getSender();

        void sendInfo(String message);
    }
}
