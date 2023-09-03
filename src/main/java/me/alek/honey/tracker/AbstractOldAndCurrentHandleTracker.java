package me.alek.honey.tracker;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public abstract class AbstractOldAndCurrentHandleTracker extends UpdatingHandleTrackerTask<Double, Player> implements StatisticHandleTracker<Double, Player> {

    private final Map<UUID, Double> oldTimeTrackerMap = new HashMap<>();
    private final Map<UUID, Double> currentTimeTrackerMap = new HashMap<>();

    private double recentAverage;

    private final AtomicDouble oldSize = new AtomicDouble();
    private final AtomicDouble currentSize = new AtomicDouble();

    private final int[] statisticInSecondsIndices;
    private final Map<UUID, StatisticProvider> statisticProviders = new HashMap<>();

    public AbstractOldAndCurrentHandleTracker(JavaPlugin plugin, long updateInterval, int... statisticInSecondsIndices) {
        super(plugin, updateInterval);

        this.statisticInSecondsIndices = statisticInSecondsIndices;
    }

    public synchronized StatisticProvider getStatisticProvider(Player player) {
        return statisticProviders.computeIfAbsent(player.getUniqueId(), (d) -> new StatisticProvider(statisticInSecondsIndices));
    }

    public double[] getStatistics(Player player) {
        double[] statistics = new double[statisticInSecondsIndices.length];

        for (int i = 0; i < statisticInSecondsIndices.length; i++) {
            statistics[i] = getStatistic(player, i);
        }
        return statistics;
    }

    public double getStatistic(Player player, int index) {
        return getStatisticProvider(player).getStatistic(index);
    }

    public void updateStatistics(Player player) {
        getStatisticProvider(player).updateStatistics();
    }

    public void putStatistic(Player player, int index, double value) {
        getStatisticProvider(player).putStatistic(index, value);
    }

    public void registerToStatistics(Player player, double value) {
        for (int i = 0; i < statisticInSecondsIndices.length; i++) {
            putStatistic(player, i, value);
        }
    }

    @Override
    public void remove(Player player) {
        remove(player.getUniqueId());
    }

    public synchronized void remove(UUID uuid) {
        oldTimeTrackerMap.remove(uuid);
        currentTimeTrackerMap.remove(uuid);
        statisticProviders.remove(uuid);
    }

    @Override
    public synchronized void registerTime(Player player, Double value) {
        double priorTotalTime = currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d);

        currentSize.addAndGet(value);
        currentTimeTrackerMap.put(player.getUniqueId(), priorTotalTime + value);
    }

    public synchronized double getTimeToAverageQuotient(Player player) {
        double time = getTimeForOldTrack(player);
        if (time == 0d) return 1;

        return time / getAverage();
    }

    @Override
    public double getTimeToTotalQuotient(Player player) {
        double time = getTimeForOldTrack(player);
        if (time == 0d) return 1;

        return time / getTotal();
    }

    public synchronized int getTimeForOldTrack(Player player) {
        return (int) oldTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d).doubleValue();
    }

    public synchronized int getTimeForCurrentTrack(Player player) {
        return (int) currentTimeTrackerMap.getOrDefault(player.getUniqueId(), 0d).doubleValue();
    }

    public int getTrackerSize() {
        return oldTimeTrackerMap.size();
    }

    @Override
    public synchronized void update() {
        int trackerSize = currentTimeTrackerMap.size();
        int totalReferencedTime = 0;

        List<Player> players = new ArrayList<>();
        // update the average
        Iterator<UUID> keySetIterator = currentTimeTrackerMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
            UUID uuid = keySetIterator.next();

            double time = currentTimeTrackerMap.get(uuid);
            oldTimeTrackerMap.put(uuid, time);

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                keySetIterator.remove();
                remove(uuid);
                continue;
            }
            totalReferencedTime += time;

            players.add(player);
        }
        currentTimeTrackerMap.clear();

        if (totalReferencedTime != currentSize.get()) currentSize.set(totalReferencedTime);
        recentAverage = currentSize.get() / (trackerSize == 0 ? 1 : trackerSize);

        oldSize.set(currentSize.get());
        currentSize.set(0);

        // update the statistics
        for (Player player : players) {
            registerToStatistics(player, getTimeToAverageQuotient(player));
            updateStatistics(player);
        }
    }

    @Override
    public double getAverage() {
        return recentAverage;
    }

    @Override
    public Double getTotal() {
        return oldSize.get();
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
