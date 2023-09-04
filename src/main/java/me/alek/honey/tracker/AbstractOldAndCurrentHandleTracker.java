package me.alek.honey.tracker;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.tracker.impl.PlayerServerThreadHandleTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public abstract class AbstractOldAndCurrentHandleTracker extends UpdatingHandleTrackerTask<Double, Player> implements StatisticHandleTracker<Double, Player> {

    private final Map<UUID, Double> oldTimeTrackerMap = new HashMap<>();
    private final Map<UUID, Double> currentTimeTrackerMap = new HashMap<>();

    private double lastEvaluatedAverage;

    private final AtomicDouble oldFlatMean = new AtomicDouble();
    private final AtomicDouble currentFlatMean = new AtomicDouble();

    private final StatisticHandleTracker<Double, Player> statisticHandleTracker;

    public AbstractOldAndCurrentHandleTracker(JavaPlugin plugin, long updateInterval, int... statisticInSecondsIndices) {
        super(plugin, updateInterval);

        this.statisticHandleTracker = new StatisticWrappedHandleTracker<>(this, statisticInSecondsIndices);
    }

    public AbstractOldAndCurrentHandleTracker(JavaPlugin plugin, long updateInterval, StatisticHandleTracker<Double, Player> statisticHandleTracker) {
        super(plugin, updateInterval);

        this.statisticHandleTracker = statisticHandleTracker;
    }

    @Override
    public StatisticProvider getStatisticProvider(Player key) {
        return statisticHandleTracker.getStatisticProvider(key);
    }

    @Override
    public Double[] getStatistics(Player key) {
        return statisticHandleTracker.getStatistics(key);
    }

    @Override
    public Double getStatistic(Player key, int index) {
        return statisticHandleTracker.getStatistic(key, index);
    }

    @Override
    public Double tryUpdateAndGetStatistic(Player key, int index) {
        return statisticHandleTracker.tryUpdateAndGetStatistic(key, index);
    }

    @Override
    public Double[] tryUpdateAndGetStatistics(Player key) {
        return statisticHandleTracker.tryUpdateAndGetStatistics(key);
    }

    @Override
    public void updateStatistics(Player key) {
        statisticHandleTracker.updateStatistics(key);
    }

    @Override
    public void updateStatistic(Player key, int index) {
        statisticHandleTracker.updateStatistic(key, index);
    }

    @Override
    public void putStatistic(Player key, int index, Double value) {
        statisticHandleTracker.putStatistic(key, index, value);
    }

    @Override
    public void registerToStatistics(Player key, Double value) {
        statisticHandleTracker.registerToStatistics(key, value);
    }

    @Override
    public void remove(Player player) {
        statisticHandleTracker.remove(player);

        UUID uuid = player.getUniqueId();
        oldTimeTrackerMap.remove(uuid);
        currentTimeTrackerMap.remove(uuid);
    }

    @Override
    public synchronized void registerTime(Player player, Double value) {
        double priorTotalTime = currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);

        currentFlatMean.addAndGet(value);
        currentTimeTrackerMap.put(player.getUniqueId(), priorTotalTime + value);
    }

    public synchronized double getTimeToAverageQuotient(Player player) {
        final double average = getAverageWithout(player);
        if (average == 0) return 0;

        return Math.max(getTimeForOldTrack(player) / average, getTimeForCurrentTrack(player) / average);
    }

    @Override
    public double getTimeToTotalQuotient(Player player) {
        return getTimeForOldTrack(player) / getTotal();
    }

    public synchronized double getTimeForOldTrack(Player player) {
        return oldTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);
    }

    public synchronized double getTimeForCurrentTrack(Player player) {
        return currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);
    }

    public int getOldTrackerSize() {
        return oldTimeTrackerMap.size();
    }

    public int getTrackerSize() {
        return currentTimeTrackerMap.size();
    }

    public double getAverageWithout(Player player) {
        if (getTrackerSize() == 1 || getOldTrackerSize() == 1) return getAverage();

        return Math.max((oldFlatMean.get() - getTimeForOldTrack(player)) / (getOldTrackerSize() - 1), (currentFlatMean.get() - getTimeForCurrentTrack(player)) / (getTrackerSize() - 1));
    }

    @Override
    public double getAverage() {
        if (getTrackerSize() == 0) return lastEvaluatedAverage;

        return Math.max(lastEvaluatedAverage, currentFlatMean.get() / getTrackerSize());
    }

    @Override
    public Double getTotal() {
        return oldFlatMean.get();
    }

    @Override
    public synchronized void update() {
        int trackerSize = getTrackerSize();
        if (trackerSize == 0) return;

        double totalReferencedTime = 0;

        // update the average
        Iterator<UUID> keySetIterator = currentTimeTrackerMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
            UUID uuid = keySetIterator.next();

            double time = currentTimeTrackerMap.get(uuid);
            oldTimeTrackerMap.put(uuid, time);

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                keySetIterator.remove();
                remove(player);
                continue;
            }
            totalReferencedTime += time;
        }
        currentTimeTrackerMap.clear();

        if (totalReferencedTime != currentFlatMean.get()) currentFlatMean.set(totalReferencedTime);
        lastEvaluatedAverage = currentFlatMean.get() / getOldTrackerSize();

        oldFlatMean.set(currentFlatMean.get());
        currentFlatMean.set(0);

        // update the statistics post change
        for (Player player : Bukkit.getOnlinePlayers()) {
            registerToStatistics(player, getTimeToAverageQuotient(player));
            updateStatistics(player);
        }
    }

    @Override
    public Set<Player> getTrackers() {
        Set<Player> trackerPlayers = new HashSet<>();
        for (UUID uuid : oldTimeTrackerMap.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            trackerPlayers.add(player);
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
