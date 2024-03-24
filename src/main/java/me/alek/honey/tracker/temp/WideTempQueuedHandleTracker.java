package me.alek.honey.tracker.temp;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.IDRegistry;
import me.alek.honey.tracker.TimedHandleTracker;
import me.alek.honey.tracker.Timespan;

import java.util.*;
import java.util.stream.Collectors;

public class WideTempQueuedHandleTracker implements TimedHandleTracker<Double, HoneyPlayer> {

    private final Map<UUID, TempTimedQueuedTracker> trackerMap = new HashMap<>();
    private final TempTimedQueuedTracker commonTracker = new TempTimedQueuedTracker(false);

    @Override
    public synchronized void remove(HoneyPlayer key) {
        if (trackerMap.containsKey(key.uuid)) {
            TempTimedQueuedTracker tracker = getTracker(key);
            for (Timespan timespan : Timespan.values()) {
                timespan.clearContext(tracker);
            }
            IDRegistry.TRACKER_CONTEXT.remove(tracker.getID());

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

    public boolean hasCollected(HoneyPlayer player, Timespan timespan) {
        return getTracker(player).getQueue(timespan).isCollected();
    }

    private double getExceedingTimedNode(TempQueuedTracker tracker, Timespan timespan) {
        TimedNode<Double> node = tracker.getQueue(timespan).asTimedQueue().peek();
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

    public double getPlayerRate(HoneyPlayer player, Timespan timespan, boolean update) {
        return getPlayerTotal(player, timespan, update) / timespan.getTimespan();
    }

    public double getPlayerRateAcceleration(HoneyPlayer player, boolean average, boolean update) {
        return getPlayerRate(player, average ? Timespan.TWO_SECOND : Timespan.ONE_SECOND, update);
    }

    public double getPlayerRateAccelerationQuotient(HoneyPlayer player, Timespan timespan, boolean average, boolean update) {
        double accel = getPlayerRateAcceleration(player, average, update);

        double deltaTotal = getPlayerTotal(player, timespan, update) - accel;
        if (deltaTotal == 0) return 0;
        double delta = timespan.getTimespan() - 1;

        return accel / (deltaTotal / delta);
    }

    public double getPlayerTotal(HoneyPlayer player, Timespan timespan, boolean update) {
        if (update) getTracker(player).clearExceedingNodes(timespan, System.nanoTime());

        double total = getTracker(player).getTotal(timespan);
        if (update) total -= getExceedingTimedNode(player, timespan);

        return total;
    }

    public double[] getStatistics(HoneyPlayer player) {
        double[] statistics = new double[Timespan.Used.USED.size()];
        int i = 0;
        for (Timespan timespan : Timespan.Used.USED) {
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
        double playerTotal = getPlayerTotal(key, timespan, update);
        return Math.max(0, playerTotal / getAverageWithout(timespan, playerTotal, update));
    }

    @Override
    public double getTimeToTotalQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return Math.max(0, getPlayerTotal(key, timespan, update) / getTotal(timespan, update));
    }

    @Override
    public double getAverage(Timespan timespan) {
        return getTotal(timespan, true) / getTrackerSize();
    }

    public double getAverageWithout(HoneyPlayer player, Timespan timespan, boolean update) {
        return getAverageWithout(timespan, getPlayerTotal(player, timespan, update), update);
    }

    public double getAverageWithout(Timespan timespan, double playerTotal, boolean update) {
        if (getTrackerSize() == 1) return playerTotal;

        return (getTotal(timespan, update) - playerTotal) / (getTrackerSize() - 1);
    }

    public double getTotal(Timespan timespan, boolean update) {
        if (update) commonTracker.clearExceedingNodes(timespan, System.nanoTime());

        double total = commonTracker.getTotal(timespan);
        if (update) total -= getExceedingTimedNode(commonTracker, timespan);

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
