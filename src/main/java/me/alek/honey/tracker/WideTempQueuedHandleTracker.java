package me.alek.honey.tracker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.omg.CORBA.UNKNOWN;

import java.util.*;
import java.util.stream.Collectors;

public class WideTempQueuedHandleTracker implements TimedHandleTracker<Double, Player> {

    private final Map<UUID, TempTimedQueuedTracker> trackerMap = new HashMap<>();
    private final TempTimedQueuedTracker commonTracker = new TempTimedQueuedTracker();

    @Override
    public synchronized void remove(Player key) {
        if (trackerMap.containsKey(key.getUniqueId())) {
            for (Timespan timespan : Timespan.values()) {
                timespan.clearContext(getTracker(key));
            }
            trackerMap.remove(key.getUniqueId());
        }
    }

    public synchronized TempTimedQueuedTracker getTracker(Player player) {
        return trackerMap.computeIfAbsent(player.getUniqueId(), (d) -> new TempTimedQueuedTracker());
    }

    public void clearExceedingNodes(Player player) {
        long time = System.currentTimeMillis();
        for (Timespan timespan : Timespan.values()) {
            clearExceedingNodes(player, timespan, time);
        }
    }

    public void clearExceedingNodes(Player player, Timespan timespan, long time) {
        getTracker(player).clearExceedingNodes(timespan, time);
        commonTracker.clearExceedingNodes(timespan, time);
    }

    public void clearExceedingNodes(boolean alsoPlayers, long time) {
        for (Timespan timespan : Timespan.values()) {
            clearExceedingNodes(timespan, alsoPlayers, time);
        }
    }

    public void clearExceedingNodes(Timespan timespan, boolean alsoPlayers, long time) {
        commonTracker.clearExceedingNodes(timespan, time);
        if (alsoPlayers) {
            for (TempTimedQueuedTracker tracker : trackerMap.values()) {
                tracker.clearExceedingNodes(timespan, time);
            }
        }
    }

    @Override
    public synchronized void registerTime(Player key, Double value) {
        if (value <= 0) return;

        long time = System.currentTimeMillis();
        getTracker(key).add(value, time);
        commonTracker.add(value, time);
    }

    private double getExceedingTimedNode(TempQueuedTracker tracker, Timespan timespan) {
        TempQueue.TimedNode node = tracker.getQueue(timespan).asTimedQueue().peek();
        long diff;
        if (node != null && node.getValue() > (diff = Math.abs((timespan.getTimespan() * 1000L - (System.currentTimeMillis() - node.getInsertionInstant()))))) {
            return node.getValue() - diff;
        }
        return 0;
    }

    private double getExceedingTimedNode(Player player, Timespan timespan) {
        return getExceedingTimedNode(getTracker(player), timespan);
    }

    public double getHalfServerThreadQuotient(Player player, Timespan timespan) {
        return getPlayerTotal(player, timespan) / (1000 * timespan.getTimespan()) * 2;
    }

    public double getPlayerTotal(Player player, Timespan timespan) {
        getTracker(player).clearExceedingNodes(timespan, System.currentTimeMillis());
        return getTracker(player).getTotal(timespan) - getExceedingTimedNode(player, timespan);
    }

    public double[] getStatistics(Player player) {
        double[] statistics = new double[Timespan.values().length];
        int i = 0;
        for (Timespan timespan : Timespan.values()) {
            statistics[i++] = getTimeToAverageWithoutQuotient(player, timespan);
        }
        return statistics;
    }

    @Override
    public double getTimeToAverageQuotient(Player key, Timespan timespan) {
        return Math.max(0, getPlayerTotal(key, timespan) / getAverage(timespan));
    }

    @Override
    public double getTimeToAverageWithoutQuotient(Player key, Timespan timespan) {
        return Math.max(0, getPlayerTotal(key, timespan) / getAverageWithout(key, timespan));
    }

    @Override
    public double getTimeToTotalQuotient(Player key, Timespan timespan) {
        return Math.max(0, getPlayerTotal(key, timespan) / getTotal(timespan));
    }

    @Override
    public double getAverage(Timespan timespan) {
        return getTotal(timespan) / getTrackerSize();
    }

    public double getAverageWithout(Player player, Timespan timespan) {
        if (getTrackerSize() == 1) return Math.max(getTotal(timespan), getPlayerTotal(player, timespan));

        return (getTotal(timespan) - getPlayerTotal(player, timespan)) / (getTrackerSize() - 1);
    }

    public double getTotal(Timespan timespan) {
        commonTracker.clearExceedingNodes(timespan, System.currentTimeMillis());
        return commonTracker.getTotal(timespan) - getExceedingTimedNode(commonTracker, timespan);
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
                return Double.compare(getPlayerTotal(o1, Timespan.TEN_SECOND), getPlayerTotal(o2, Timespan.TEN_SECOND));
            }
        });
        return trackersList;
    }

    @Override
    public void clear() {
        trackerMap.clear();
    }
}
