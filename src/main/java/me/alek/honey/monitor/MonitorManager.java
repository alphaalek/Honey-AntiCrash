package me.alek.honey.monitor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.NotificationService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorManager {

    public static final Monitorer SERVER_MONITOR = new ServerMonitorer();

    boolean packetsMonitored; // cache the value
    private final Set<Monitorer> packetMonitorers = new HashSet<>();
    private final Map<Monitorer, List<MonitorObserver>> monitorObservers = new HashMap<>();
    private final Map<UUID, Monitorer> playerMonitorerMap = new HashMap<>();
    private ScheduledExecutorService executorService;

    public MonitorManager() {
        schedule();
    }

    public void schedule() {
        executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(() -> {
            try {
                for (Monitorer monitorer : monitorObservers.keySet()) {
                    if (monitorer instanceof ScheduledMonitorer) {
                        ScheduledMonitorer scheduledMonitorer = (ScheduledMonitorer) monitorer;

                        String message = scheduledMonitorer.getMessage();
                        for (MonitorObserver observer : monitorObservers.get(monitorer)) {
                            observer.sendInfo(message);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0L, ConfigurationOptions.Monitor.MESSAGE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void reload() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS)) {
                HoneyPlugin.get().getLoggerService().getLogger().severe(Lang.get("monitor.executor.service.shutdown.error"));
            }
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe(Lang.get("monitor.executor.service.shutdown.exception"));
        }
        schedule();
    }

    public Monitorer getMonitorer(HoneyPlayer player) {
        return playerMonitorerMap.computeIfAbsent(player.uuid, (d) -> new PlayerMonitorer(player));
    }

    public boolean isMonitored(HoneyPlayer player) {
        return monitorObservers.containsKey(getMonitorer(player));
    }

    public boolean isPacketsMonitored() {
        return packetsMonitored;
    }

    public boolean isServerMonitored() {
        return monitorObservers.containsKey(SERVER_MONITOR) && !monitorObservers.get(SERVER_MONITOR).isEmpty();
    }

    public void removeFromMonitors(HoneyPlayer player) {
        monitorObservers.remove(getMonitorer(player));
        playerMonitorerMap.remove(player.uuid);
        removeMonitor(player.player);
    }

    public boolean hasMonitorers() {
        return !monitorObservers.isEmpty();
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

    public void setupPacketMonitor(CommandSender sender, Flags.RuntimeFlags flags) {
        removeIfHasMonitor(sender, false);

        Monitorer monitorer = new PacketMonitorer<>(flags);
        setupMonitor(sender, monitorer);
        packetMonitorers.add(monitorer);
        packetsMonitored = true;
    }

    public void setupPlayerMonitor(CommandSender sender, HoneyPlayer player) {
        if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED
                && player.player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
            sender.sendMessage(Lang.getFormatted("monitor.start.bypass.permission.error", player.name));
            return;
        }

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
        if (!monitorObservers.containsKey(monitorer)) return;

        for (MonitorObserver observer : monitorObservers.get(monitorer)) {
            observer.sendInfo(message);
        }
    }

    public void sendPacketInfo(PacketContainer<? extends WrappedPacket<?>> packetContainer, MonitorSendAt sendAt) {
        if (packetMonitorers.isEmpty()) {
            return;
        }

        for (Monitorer packetMonitorer : packetMonitorers) {
            if (!(packetMonitorer instanceof IndependentMonitorer)) {
                return;
            }

            @SuppressWarnings("unchecked")
            IndependentMonitorer<PacketContainer<? extends WrappedPacket<?>>> independentMonitorer =
                    (IndependentMonitorer<PacketContainer<? extends WrappedPacket<?>>>) packetMonitorer;
            if (!independentMonitorer.getSendAt().equals(sendAt)) {
                continue;
            }

            String message = independentMonitorer.getMessage(packetContainer);
            Bukkit.broadcastMessage(packetMonitorer + " " + message);
            if (message == null) {
                continue;
            }

            for (MonitorObserver observer : monitorObservers.get(independentMonitorer)) {
                observer.sendInfo(message);
            }
        }
    }

    public void setupMonitor(CommandSender sender, Monitorer monitorer) {
        List<MonitorObserver> observers = monitorObservers.computeIfAbsent(monitorer, (d) -> new ArrayList<>());
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
        for (Monitorer monitorer : Sets.newHashSet(monitorObservers.keySet())) {
            List<MonitorObserver> observers = monitorObservers.get(monitorer);

            for (MonitorObserver observer : Lists.newArrayList(observers)) {
                if (observer.getSender().equals(sender)) {

                    observers.remove(observer);
                    if (observers.isEmpty()) {
                        monitorObservers.remove(monitorer);
                    }

                    if (monitorer.getType() == MonitorType.PACKETS) {
                        packetMonitorers.remove(monitorer);
                    }
                    packetsMonitored = !packetMonitorers.isEmpty();

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
