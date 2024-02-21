package me.alek.honey.tracker.updated;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.tracker.statistic.StatedHandleTracker;
import me.alek.honey.tracker.statistic.StatisticHandleTracker;
import me.alek.honey.tracker.statistic.StatisticProvider;
import me.alek.honey.tracker.statistic.WrappedStatisticHandleTrackerDelegate;
import me.alek.honey.tracker.updated.UpdatingHandleTrackerTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class OldAndCurrentHandleTracker extends UpdatingHandleTrackerTask<Double, Player> implements StatisticHandleTracker<Double, Player>, StatedHandleTracker<Double, Player> {

    private static class TrackerContext {

        private double meanSize;
        private double totalFlatMean;
    }

    private final Map<UUID, TrackerContext> oldTimeTrackerMap = new HashMap<>();
    private final Map<UUID, TrackerContext> currentTimeTrackerMap = new HashMap<>();

    private double lastEvaluatedAverage;

    private final AtomicDouble oldFlatMean = new AtomicDouble();
    private final AtomicDouble currentFlatMean = new AtomicDouble();

    private final StatisticHandleTracker<Double, Player> statisticHandleTracker;

    public OldAndCurrentHandleTracker(long updateInterval, int... statisticInSecondsIndices) {
        super(updateInterval);

        this.statisticHandleTracker = new WrappedStatisticHandleTrackerDelegate<>(this, statisticInSecondsIndices);
    }

    public OldAndCurrentHandleTracker(long updateInterval, StatisticHandleTracker<Double, Player> statisticHandleTracker) {
        super(updateInterval);

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
    public Double getStatisticDirectly(Player key, int index) {
        return statisticHandleTracker.getStatisticDirectly(key, index);
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
        TrackerContext context = currentTimeTrackerMap.getOrDefault(player.getUniqueId(), new TrackerContext());
        context.totalFlatMean += value;
        context.meanSize++;

        currentFlatMean.addAndGet(value);
        currentTimeTrackerMap.put(player.getUniqueId(), context);
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
        return oldTimeTrackerMap.getOrDefault(player.getUniqueId(), new TrackerContext()).totalFlatMean;
    }

    public synchronized double getTimeForCurrentTrack(Player player) {
        return currentTimeTrackerMap.getOrDefault(player.getUniqueId(), new TrackerContext()).totalFlatMean;
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

    public double getTotal() {
        return oldFlatMean.get();
    }

    @Override
    public synchronized void update() {
        int trackerSize = getTrackerSize();
        if (trackerSize == 0) return;

        double totalReferencedTime = 0;
        trackerSize = 0;

        // update the average
        final Iterator<UUID> keySetIterator = currentTimeTrackerMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
            final UUID uuid = keySetIterator.next();
            final Player player = Bukkit.getPlayer(uuid);

            final TrackerContext context = currentTimeTrackerMap.get(uuid);

            if (context.meanSize < 5) continue;
            oldTimeTrackerMap.put(uuid, context);

            if (player == null || !player.isOnline()) {
                keySetIterator.remove();
                remove(player);
                continue;
            }
            trackerSize++;
            totalReferencedTime += context.totalFlatMean;
        }
        currentTimeTrackerMap.clear();

        if (totalReferencedTime != currentFlatMean.get()) currentFlatMean.set(totalReferencedTime);
        lastEvaluatedAverage = currentFlatMean.get() / trackerSize;

        oldFlatMean.set(currentFlatMean.get());
        currentFlatMean.set(0);

        // update the statistics post change
        for (final Player player : Bukkit.getOnlinePlayers()) {
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

    @Override
    public void clear() {
        oldTimeTrackerMap.clear();
        currentTimeTrackerMap.clear();
    }
}
