package me.alek.honey.tracker.temp;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.tracker.TimedManualHandleTracker;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.IDRegistry;

import java.util.*;
import java.util.stream.Collectors;

public class TempIntervalTrackerManager implements TimedManualHandleTracker<Long, Double, HoneyPlayer> {

    private final Map<UUID, TempIntervalTracker> trackerMap = new HashMap<>();
    private final TempIntervalTracker commonTracker = new TempIntervalTracker(false, false);

    @Override
    public synchronized void remove(HoneyPlayer key) {
        if (trackerMap.containsKey(key.uuid)) {
            TempIntervalTracker tracker = getTracker(key);
            for (Timespan timespan : Timespan.values()) {
                timespan.clearContext(tracker);
            }
            IDRegistry.TRACKER_CONTEXT.remove(tracker.getID());

            trackerMap.remove(key.uuid);
        }
        for (UUID uuid : Sets.newHashSet(trackerMap.keySet())) {
            if (HoneyPlayer.getPlayer(uuid) == null) {
                trackerMap.remove(uuid);
            }
        }
    }

    @Override
    public boolean has(HoneyPlayer key) {
        return trackerMap.containsKey(key.uuid);
    }

    public TempIntervalTracker getPropertyTracker(HoneyPlayer player) {
        return player.timedQueuedTracker;
    }

    public void setPropertyTracker(HoneyPlayer player, TempIntervalTracker tracker) {
        player.timedQueuedTracker = tracker;
    }

    public synchronized TempIntervalTracker getTracker(HoneyPlayer player) {
        if (getPropertyTracker(player) == null) {
            setPropertyTracker(player, trackerMap.computeIfAbsent(player.uuid, (d) -> new TempIntervalTracker(true, false)));
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
            for (TempIntervalTracker tracker : trackerMap.values()) {
                tracker.clearExceedingNodes(timespan, time);
            }
        }
    }

    @Override
    public synchronized void registerTime(HoneyPlayer key, Long value) {
        if (value <= 0) return;

        TimedNode<Long> node = new TimedNode<>(value);
        getTracker(key).add(node);
        commonTracker.add(node);
    }

    public boolean hasCollected(HoneyPlayer player, Timespan timespan) {
        return getTracker(player).getQueue(timespan).isCollected();
    }

    private long getExceedingTimedNode(TempIntervalTracker tracker, Timespan timespan, long time) {
        TimedNode<Long> node = tracker.getQueue(timespan).asTimedQueue().peek();
        long diff;
        if (node != null && node.getValue() > (diff = Math.abs((timespan.getNanoTimespan() - (time - node.getInsertionInstant()))))) {
            return node.getValue() - diff;
        }
        return 0;
    }

    private long getExceedingTimedNode(TempIntervalTracker tracker, Timespan timespan) {
        return getExceedingTimedNode(tracker, timespan, System.nanoTime());
    }

    private long getExceedingTimedNode(HoneyPlayer player, Timespan timespan) {
        return getExceedingTimedNode(getTracker(player), timespan);
    }

    private Long getEndedNode(HoneyPlayer player, Timespan timespan, boolean update, boolean last) {
        Deque<TimedNode<Long>> queue = getTracker(player).getQueue(timespan).asTimedQueue();
        TimedNode<Long> node;
        if (last) {
            node = queue.peekLast();
        } else {
            node = queue.peekFirst();
        }

        if (node == null) {
            return 0L;
        }

        if (update && System.nanoTime() > node.getInsertionInstant() + timespan.getNanoTimespan()) {
            return 0L;
        }

        return node.getValue();
    }

    public Long getRecentNode(HoneyPlayer player, Timespan timespan, boolean update) {
        return getEndedNode(player, timespan, update, false);
    }

    public Long getOldestNode(HoneyPlayer player, Timespan timespan, boolean update) {
        return getEndedNode(player, timespan, update, true);
    }

    public Double getOfTotalQuotient(HoneyPlayer player, Timespan timespan, boolean update) {
        return ((double) getPlayerTotal(player, timespan, update)) / timespan.getNanoTimespan();
    }

    public Double getPlayerRate(HoneyPlayer player, Timespan timespan, boolean update) {
        return ((double) getPlayerTotal(player, timespan, update)) / timespan.getTimespan();
    }

    public Double getPlayerRateAcceleration(HoneyPlayer player, boolean update) {
        return getPlayerRate(player, Timespan.ONE_SECOND, update);
    }

    public Double getPlayerRateAccelerationQuotient(HoneyPlayer player, Timespan timespan, boolean update) {
        double accel = getPlayerRateAcceleration(player, update);

        double deltaTotal = getPlayerTotal(player, timespan, update) - accel;
        if (deltaTotal == 0) {
            return 0d;
        }

        return accel / (deltaTotal / (timespan.getTimespan() - 1));
    }

    @Override
    public Long getPlayerTotal(HoneyPlayer player, Timespan timespan, boolean update) {
        long time = 0;
        TempIntervalTracker tracker = getTracker(player);
        if (update) {
            tracker.clearExceedingNodes(timespan, time = System.nanoTime());
        }

        long total = tracker.getTotal(timespan);
        if (update) {
            total -= getExceedingTimedNode(tracker, timespan, time);
        }

        return total;
    }

    public double[] getStatistics(HoneyPlayer player) {
        double[] statistics = new double[Timespan.USED_FOR_TRACKERS.size()];
        int i = 0;
        for (Timespan timespan : Timespan.USED_FOR_TRACKERS) {
            statistics[i++] = getTimeToAverageWithoutQuotient(player, timespan, true);
        }
        return statistics;
    }

    @Override
    public Double getTimeToAverageQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        double average = getAverage(timespan);
        if (average == 0) {
            return 0d;
        }

        return getPlayerTotal(key, timespan, update) / average;
    }

    @Override
    public Double getTimeToAverageWithoutQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        long playerTotal = getPlayerTotal(key, timespan, update);

        return playerTotal / getAverageWithout(timespan, playerTotal, update);
    }

    @Override
    public Double getTimeToTotalQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        long total = getTotal(timespan, update);
        if (total == 0) {
            return 0d;
        }

        return ((double) getPlayerTotal(key, timespan, update)) / total;
    }

    @Override
    public Double getAverage(Timespan timespan) {
        return (double) getTotal(timespan, true) / getTrackerSize();
    }

    public double getAverageWithout(HoneyPlayer player, Timespan timespan, boolean update) {
        return getAverageWithout(timespan, getPlayerTotal(player, timespan, update), update);
    }

    public double getAverageWithout(Timespan timespan, long playerTotal, boolean update) {
        if (getTrackerSize() == 1) {
            return playerTotal;
        }

        return (getTotal(timespan, update) - playerTotal) / (getTrackerSize() - 1);
    }

    public Long getTotal(Timespan timespan, boolean update) {
        if (update) {
            commonTracker.clearExceedingNodes(timespan, System.nanoTime());
        }

        long total = commonTracker.getTotal(timespan);
        if (update) {
            total -= getExceedingTimedNode(commonTracker, timespan);
        }

        return total;
    }

    @Override
    public int getTrackerSize() {
        return trackerMap.size();
    }

    @Override
    public Set<HoneyPlayer> getTrackers() {
        return trackerMap.keySet().stream()
                .map(HoneyPlayer::getPlayer)
                .filter(p -> p != null && p.isOnlineBukkit())
                .collect(Collectors.toSet());
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        List<HoneyPlayer> trackersList = new ArrayList<>(getTrackers());
        trackersList.removeIf(player -> getPlayerTotal(player, Timespan.FIVE_SECOND, false) == 0);
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
