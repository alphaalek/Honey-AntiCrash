package me.alek.honey.netty.injector;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PacketHandleTimeTracker {

    private final Map<UUID, Double> oldTimeTrackerMap = new HashMap<>();
    private final Map<UUID, Double> currentTimeTrackerMap = new HashMap<>();
    private double recentAverage;

    public PacketHandleTimeTracker(JavaPlugin plugin) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (this) {
                    recentAverage = evaluateAverage();
                }
            }
        };
        runnable.run();
        runnable.runTaskTimerAsynchronously(plugin, 0L, 30L);
    }

    public synchronized void registerPacketHandleTime(Player player, double time) {
        double priorTotalTime = currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);

        currentTimeTrackerMap.put(player.getUniqueId(), priorTotalTime + time);
    }

    public synchronized double evaluateAverage() {
        double totalSize = 0;
        int trackerSize = currentTimeTrackerMap.size();

        if (trackerSize == 0) return 0;

        for (UUID uuid : currentTimeTrackerMap.keySet()) {
            double time = currentTimeTrackerMap.get(uuid);
            oldTimeTrackerMap.put(uuid, time);

            totalSize += time;
        }
        currentTimeTrackerMap.clear();

        return totalSize / trackerSize;
    }

    public synchronized double getTimeToAverageQuotient(Player player) {
        double totalTime = getTimeForOldTrack(player);
        if (recentAverage == 0 || totalTime == 0) return 1;

        double oldQuotient = totalTime / recentAverage;
        double currentQuotient = getTimeForCurrentTrack(player) / recentAverage;

        return Math.max(oldQuotient, currentQuotient);
    }

    public synchronized double getTimeForOldTrack(Player player) {
        return oldTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);
    }

    public synchronized double getTimeForCurrentTrack(Player player) {
        return currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);
    }

    public double getRecentAverage() {
        return recentAverage;
    }

    public int getTrackSize() {
        return currentTimeTrackerMap.size();
    }

    public synchronized void removePlayer(Player player) {
        oldTimeTrackerMap.remove(player.getUniqueId());
        currentTimeTrackerMap.remove(player.getUniqueId());
    }
}
