package me.alek.honey.tracker.impl;

import me.alek.honey.tracker.UpdatingHandleTrackerTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PlayerPacketHandleTracker extends UpdatingHandleTrackerTask<Double, Player> {

    private final Map<UUID, Double> oldTimeTrackerMap = new HashMap<>();
    private final Map<UUID, Double> currentTimeTrackerMap = new HashMap<>();

    private double recentAverage;
    private double recentTotal;

    public PlayerPacketHandleTracker(JavaPlugin plugin) {
        super(plugin, 40L);
        update();
    }

    @Override
    public void remove(Player player) {
        oldTimeTrackerMap.remove(player.getUniqueId());
        currentTimeTrackerMap.remove(player.getUniqueId());
    }

    @Override
    public synchronized void registerTime(Player player, Double value) {
        double priorTotalTime = currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);

        currentTimeTrackerMap.put(player.getUniqueId(), priorTotalTime + value);
    }

    public synchronized Double getTimeToAverageQuotient(Player player) {
        double totalTime = getTimeForOldTrack(player);
        if (recentAverage == 0 || totalTime == 0) return 1d;

        return totalTime / recentAverage;
    }

    @Override
    public Double getTimeToTotalQuotient(Player player) {
        return getTimeForOldTrack(player) / getTotal();
    }

    public synchronized double getTimeForOldTrack(Player player) {
        return oldTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);
    }

    public synchronized double getTimeForCurrentTrack(Player player) {
        return currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);
    }

    public Set<UUID> getTrackerPlayers() {
        return oldTimeTrackerMap.keySet();
    }

    public int getTrackerSize() {
        return currentTimeTrackerMap.size();
    }

    @Override
    public synchronized void update() {
        double totalSize = 0;
        int trackerSize = currentTimeTrackerMap.size();

        if (trackerSize == 0) return;

        for (UUID uuid : currentTimeTrackerMap.keySet()) {
            double time = currentTimeTrackerMap.get(uuid);
            oldTimeTrackerMap.put(uuid, time);

            totalSize += time;
        }
        currentTimeTrackerMap.clear();

        recentAverage = (recentTotal = totalSize) / trackerSize;
    }

    @Override
    public Double getAverage() {
        return recentAverage;
    }

    @Override
    public Double getTotal() {
        return recentTotal;
    }

    @Override
    public Set<Player> getTrackers() {
        Set<Player> trackerPlayers = new HashSet<>();
        for (UUID uuid : oldTimeTrackerMap.keySet()) {
            trackerPlayers.add(Bukkit.getPlayer(uuid));
        }
        return trackerPlayers;
    }

    @Override
    public List<Player> getTrackersOrdered() {
        List<Player> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return Double.compare(getTimeForOldTrack(o1), getTimeForOldTrack(o2));
            }
        });
        return trackersList;
    }
}
