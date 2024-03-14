package me.alek.honey.tracker.temp;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.tracker.TrackerIDRegistry;
import me.alek.honey.tracker.timed.TimedHandleTracker;
import me.alek.honey.tracker.timed.Timespan;

import java.util.*;
import java.util.stream.Collectors;

public class WideTempQueuedHandleTracker implements TimedHandleTracker<Double, HoneyPlayer> {

    private final Map<UUID, TempTimedQueuedTracker> trackerMap = new HashMap<>();
    private final TempTimedQueuedTracker commonTracker = new TempTimedQueuedTracker(false);

    @Override
    public synchronized void remove(HoneyPlayer key) {
        if (trackerMap.containsKey(key.uuid)) {
            for (Timespan timespan : Timespan.values()) {
                timespan.clearContext(getTracker(key));
            }
            TrackerIDRegistry.remove(getTracker(key).getID());
            trackerMap.remove(key.uuid);
        }
        for (UUID uuid : Sets.newHashSet(trackerMap.keySet())) {
            if (HoneyPlayer.getPlayer(uuid) == null) trackerMap.remove(uuid);
        }
    }

    public TempTimedQueuedTracker getPropertyTracker(HoneyPlayer player) {
        return player.timedQueuedTracker;
    }

    public void setPropertyTracker(HoneyPlayer player, TempTimedQueuedTracker tracker) {
        player.timedQueuedTracker = tracker;
    }

    public synchronized TempTimedQueuedTracker getTracker(HoneyPlayer player) {
        if (getPropertyTracker(player) == null) {
            setPropertyTracker(player, trackerMap.computeIfAbsent(player.uuid, (d) -> new TempTimedQueuedTracker(true)));
        }
        return getPropertyTracker(player);
    }

    public void clearExceedingNodes(HoneyPlayer player) {
        long time = System.nanoTime();
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

        long time = System.nanoTime();
        getTracker(key).add(value, time);
        commonTracker.add(value, time);
    }

    private double getExceedingTimedNode(TempQueuedTracker tracker, Timespan timespan) {
        TempQueue.TimedNode node = tracker.getQueue(timespan).asTimedQueue().peek();
        long diff;
        if (node != null && node.getValue() > (diff = Math.abs((timespan.getNanoTimespan() - (System.nanoTime() - node.getInsertionInstant()))))) {
            return node.getValue() - diff;
        }
        return 0;
    }

    private double getExceedingTimedNode(HoneyPlayer player, Timespan timespan) {
        return getExceedingTimedNode(getTracker(player), timespan);
    }

    public double getHalfServerThreadQuotient(HoneyPlayer player, Timespan timespan, boolean update) {
        return getPlayerTotal(player, timespan, update) / timespan.getNanoTimespan() * 2;
    }

    public double getPlayerTotal(HoneyPlayer player, Timespan timespan, boolean update) {
        if (update) getTracker(player).clearExceedingNodes(timespan, System.nanoTime());

        double total = getTracker(player).getTotal(timespan);
        if (update) total -= getExceedingTimedNode(player, timespan);

        return total;
    }

    public double[] getStatistics(HoneyPlayer player) {
        double[] statistics = new double[Timespan.values().length];
        int i = 0;
        for (Timespan timespan : Timespan.values()) {
            statistics[i++] = getTimeToAverageWithoutQuotient(player, timespan, true);
        }
        return statistics;
    }

    @Override
    public double getTimeToAverageQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return Math.max(0, getPlayerTotal(key, timespan, update) / getAverage(timespan));
    }

    @Override
    public double getTimeToAverageWithoutQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return Math.max(0, getPlayerTotal(key, timespan, update) / getAverageWithout(key, timespan, update));
    }

    @Override
    public double getTimeToTotalQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return Math.max(0, getPlayerTotal(key, timespan, update) / getTotal(timespan));
    }

    @Override
    public double getAverage(Timespan timespan) {
        return getTotal(timespan) / getTrackerSize();
    }

    public double getAverageWithout(HoneyPlayer player, Timespan timespan, boolean update) {
        if (getTrackerSize() == 1) return Math.max(getTotal(timespan), getPlayerTotal(player, timespan, update));

        return (getTotal(timespan) - getPlayerTotal(player, timespan, update)) / (getTrackerSize() - 1);
    }

    public double getTotal(Timespan timespan) {
        commonTracker.clearExceedingNodes(timespan, System.nanoTime());
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
                .filter(p -> p != null && p.player.isOnline())
                .collect(Collectors.toSet());
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        List<HoneyPlayer> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<HoneyPlayer>() {
            @Override
            public int compare(HoneyPlayer o1, HoneyPlayer o2) {
                return Double.compare(getPlayerTotal(o1, Timespan.TEN_SECOND, true), getPlayerTotal(o2, Timespan.TEN_SECOND, true));
            }
        });
        return trackersList;
    }

    @Override
    public void clear() {
        trackerMap.clear();
    }

    @Override
    public int getID() {
        return -1;
    }
}
