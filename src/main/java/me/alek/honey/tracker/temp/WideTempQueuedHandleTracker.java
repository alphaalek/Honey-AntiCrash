package me.alek.honey.tracker.temp;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.tracker.timed.TimedHandleTracker;
import me.alek.honey.tracker.timed.Timespan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WideTempQueuedHandleTracker implements TimedHandleTracker<Double, HoneyPlayer> {

    private final Map<UUID, TempTimedQueuedTracker> trackerMap = new HashMap<>();
    private final TempTimedQueuedTracker commonTracker = new TempTimedQueuedTracker();

    @Override
    public synchronized void remove(HoneyPlayer key) {
        if (trackerMap.containsKey(key.uuid)) {
            for (Timespan timespan : Timespan.values()) {
                timespan.clearContext(getTracker(key));
            }
            trackerMap.remove(key.uuid);
        }
    }

    public synchronized TempTimedQueuedTracker getTracker(HoneyPlayer player) {
        return trackerMap.computeIfAbsent(player.uuid, (d) -> new TempTimedQueuedTracker());
    }

    public void clearExceedingNodes(HoneyPlayer player) {
        long time = System.currentTimeMillis();
        for (Timespan timespan : Timespan.values()) {
            clearExceedingNodes(player, timespan, time);
        }
    }

    public void clearExceedingNodes(HoneyPlayer player, Timespan timespan, long time) {
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
    public synchronized void registerTime(HoneyPlayer key, Double value) {
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

    private double getExceedingTimedNode(HoneyPlayer player, Timespan timespan) {
        return getExceedingTimedNode(getTracker(player), timespan);
    }

    public double getHalfServerThreadQuotient(HoneyPlayer player, Timespan timespan) {
        return getPlayerTotal(player, timespan) / (Math.pow(10, 9) * timespan.getTimespan()) * 2;
    }

    public double getPlayerTotal(HoneyPlayer player, Timespan timespan) {
        getTracker(player).clearExceedingNodes(timespan, System.currentTimeMillis());
        return getTracker(player).getTotal(timespan) - getExceedingTimedNode(player, timespan);
    }

    public double[] getStatistics(HoneyPlayer player) {
        double[] statistics = new double[Timespan.values().length];
        int i = 0;
        for (Timespan timespan : Timespan.values()) {
            statistics[i++] = getTimeToAverageWithoutQuotient(player, timespan);
        }
        return statistics;
    }

    @Override
    public double getTimeToAverageQuotient(HoneyPlayer key, Timespan timespan) {
        return Math.max(0, getPlayerTotal(key, timespan) / getAverage(timespan));
    }

    @Override
    public double getTimeToAverageWithoutQuotient(HoneyPlayer key, Timespan timespan) {
        return Math.max(0, getPlayerTotal(key, timespan) / getAverageWithout(key, timespan));
    }

    @Override
    public double getTimeToTotalQuotient(HoneyPlayer key, Timespan timespan) {
        return Math.max(0, getPlayerTotal(key, timespan) / getTotal(timespan));
    }

    @Override
    public double getAverage(Timespan timespan) {
        return getTotal(timespan) / getTrackerSize();
    }

    public double getAverageWithout(HoneyPlayer player, Timespan timespan) {
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
    public Set<HoneyPlayer> getTrackers() {
        return trackerMap.keySet().stream()
                .map(HoneyPlayer::getPlayer)
                .filter(p -> p.player.isOnline())
                .collect(Collectors.toSet());
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        List<HoneyPlayer> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<HoneyPlayer>() {
            @Override
            public int compare(HoneyPlayer o1, HoneyPlayer o2) {
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
