package me.alek.honey.monitor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorManager {

    public static final Monitorer SERVER_MONITOR = new ServerMonitorer();

    private final Map<Monitorer, List<MonitorObserver>> playerMonitorObservers = new HashMap<>();
    private final Map<UUID, Monitorer> playerMonitorerMap = new HashMap<>();
    private ScheduledExecutorService executorService;

    public MonitorManager() {
        schedule();
    }

    public void schedule() {
        executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(() -> {
            try {
                for (Monitorer monitorer : playerMonitorObservers.keySet()) {
                    String message = monitorer.getMessage();
                    for (MonitorObserver observer : playerMonitorObservers.get(monitorer)) {
                        observer.sendInfo(message);
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
                Bukkit.getLogger().severe("Monitor executor service did not terminate successfully!");
            }
        } catch (Exception ex) {
            Bukkit.getLogger().severe("Exception occurred when shutting down monitor executor service!");
        }
        schedule();
    }

    public Monitorer getMonitorer(Player player) {
        return playerMonitorerMap.computeIfAbsent(player.getUniqueId(), (d) -> new PlayerMonitorer(player));
    }

    public boolean isMonitored(Player player) {
        return playerMonitorObservers.containsKey(getMonitorer(player));
    }

    public boolean isServerMonitored() {
        return playerMonitorObservers.containsKey(SERVER_MONITOR) && !playerMonitorObservers.get(SERVER_MONITOR).isEmpty();
    }

    public void removeFromMonitors(Player player) {
        playerMonitorObservers.remove(getMonitorer(player));
        playerMonitorerMap.remove(player.getUniqueId());
        removeMonitor(player);
    }

    public void removeIfHasMonitor(CommandSender sender, boolean onlyStop) {
        Monitorer monitorer = removeMonitor(sender);
        if (monitorer != null)
            sender.sendMessage(CommandManager.PREFIX + "§7Stopping monitor of " + monitorer.getName() + ".");
        else if (onlyStop)
            sender.sendMessage(CommandManager.PREFIX + "§7You do not currently monitor anything.");
    }

    public void setupPlayerMonitor(CommandSender sender, Player player) {
        removeIfHasMonitor(sender, false);
        setupMonitor(sender, getMonitorer(player));
    }

    public void setupServerMonitor(CommandSender sender) {
        removeIfHasMonitor(sender, false);
        setupMonitor(sender, SERVER_MONITOR);
    }

    public void sendServerInfo(String message) {
        sendInfo(SERVER_MONITOR, message);
    }

    public void sendPlayerInfo(Player player, String message) {
        sendInfo(getMonitorer(player), message);
    }

    public void sendInfo(Monitorer monitorer, String message) {
        if (!playerMonitorObservers.containsKey(monitorer)) return;

        for (MonitorObserver observer : playerMonitorObservers.get(monitorer)) {
            observer.sendInfo(message);
        }
    }

    public void setupMonitor(CommandSender sender, Monitorer monitorer) {
        List<MonitorObserver> observers = playerMonitorObservers.computeIfAbsent(monitorer, (d) -> new ArrayList<>());
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
        sender.sendMessage(CommandManager.PREFIX + "§7Started monitoring of " + monitorer.getName() +
                ". Use /honey monitor stop if you want to stop the monitoring.");
    }

    public Monitorer removeMonitor(CommandSender sender) {
        for (Monitorer monitorer : Sets.newHashSet(playerMonitorObservers.keySet())) {
            List<MonitorObserver> observers = playerMonitorObservers.get(monitorer);

            for (MonitorObserver observer : Lists.newArrayList(observers)) {

                if (observer.getSender().equals(sender)) {
                    observers.remove(observer);
                    if (observers.isEmpty())
                        playerMonitorObservers.remove(monitorer);

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
