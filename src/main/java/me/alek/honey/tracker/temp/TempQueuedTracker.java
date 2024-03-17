package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.HoneyTracker;
import me.alek.honey.IDRegistry;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public abstract class TempQueuedTracker implements HoneyTracker {

    protected final Tuple2<AtomicLong, TempQueue>[] tickDurationQueues = new Tuple2[Timespan.values().length];
    private final int id;

    public TempQueuedTracker(boolean hasId) {
        if (hasId) id = IDRegistry.TRACKER_CONTEXT.acquireID();
        else id = -1;

        setup();
    }

    public int getID() {
        return id;
    }

    private synchronized void setup() {
        int i = 0;
        for (Timespan timespan : Timespan.values()) {
            tickDurationQueues[i++] = new Tuple2<>(timespan.getTotal(this), timespan.getQueue(this));
        }
    }

    public void clearExceedingNodes(Timespan timespan, long time) {
        Tuple2<AtomicLong, TempQueue> queue = getTuple(timespan);
        queue.getT2().clearExceedingNodes(time);
    }

    public double getTotal(Timespan timespan) {
        Tuple2<AtomicLong, TempQueue> queue = getTuple(timespan);
        return queue.getT1().get();
    }

    public TempQueue getQueue(Timespan timespan) {
        Tuple2<AtomicLong, TempQueue> queue = getTuple(timespan);
        return queue.getT2();
    }

    public synchronized Tuple2<AtomicLong, TempQueue> getTuple(Timespan timespan) {
        Tuple2<AtomicLong, TempQueue> q;
        if (timespan == Timespan.TEN_SECOND && tickDurationQueues.length > 0
                && ((q = tickDurationQueues[0]).getT2().getTimespan().equals(Timespan.TEN_SECOND)))
            return q;

        for (Tuple2<AtomicLong, TempQueue> queue : tickDurationQueues) {
            if (timespan.getQueue(this).equals(queue.getT2()))
                return queue;
        }
        setup();
        return getTuple(timespan);
    }

    public synchronized int getLength(Timespan timespan) {
        for (TempQueue queue : Arrays.stream(tickDurationQueues).map(Tuple2::getT2).collect(Collectors.toSet())) {
            if (queue.getTimespan() == timespan)
                return queue.getQueueSize();
        }
        setup();
        return getLength(timespan);
    }
}
