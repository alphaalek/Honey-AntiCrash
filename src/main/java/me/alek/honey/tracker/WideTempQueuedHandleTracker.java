package me.alek.honey.tracker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WideTempQueuedHandleTracker implements TimedHandleTracker<Integer, Player> {

    private final Map<UUID, TempTimedQueuedTracker> trackerMap = new HashMap<>();
    private final TempTimedQueuedTracker commonTracker = new TempTimedQueuedTracker();

    @Override
    public void remove(Player key) {
        trackerMap.remove(key.getUniqueId());
    }

    public TempTimedQueuedTracker getTracker(Player player) {
        return trackerMap.computeIfAbsent(player.getUniqueId(), (d) -> new TempTimedQueuedTracker());
    }

    @Override
    public void registerTime(Player key, Integer value) {
        getTracker(key).add(value);
        commonTracker.add(value);
    }

    public int getPlayerTotal(Player player, Timespan timespan) {
        return getTracker(player).getTotal(timespan);
    }

    public double[] getStatistics(Player player) {
        double[] statistics = new double[Timespan.values().length];
        int i = 0;
        for (Timespan timespan : Timespan.values()) {
            statistics[i++] = getTimeToAverageQuotient(player, timespan);
        }
        return statistics;
    }

    @Override
    public double getTimeToAverageQuotient(Player key, Timespan timespan) {
        return getPlayerTotal(key, timespan) / getAverage(timespan);
    }

    @Override
    public double getTimeToTotalQuotient(Player key, Timespan timespan) {
        return getPlayerTotal(key, timespan) / getTotal(timespan);
    }

    @Override
    public double getAverage(Timespan timespan) {
        return getTotal(timespan) / getTrackerSize();
    }

    public double getTotal(Timespan timespan) {
        return commonTracker.getTotal(timespan);
    }

    @Override
    public int getTrackerSize() {
        return trackerMap.size();
    }

    @Override
    public Set<Player> getTrackers() {
        return trackerMap.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }

    @Override
    public List<Player> getTrackersOrdered() {
        List<Player> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return Double.compare(getPlayerTotal(o1, Timespan.FIVE_SECOND), getPlayerTotal(o2, Timespan.FIVE_SECOND));
            }
        });
        return trackersList;
    }

    @Override
    public void clear() {
        trackerMap.clear();
    }
}
