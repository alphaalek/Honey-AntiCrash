package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.HoneyTracker;
import me.alek.honey.IDRegistry;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.utility.Tuple2;

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

    public double getTotal(Timespan timespan) {
        return getTuple(timespan).getT1().get();
    }

    public void clearExceedingNodes(Timespan timespan, long time) {
        getQueue(timespan).clearExceedingNodes(time);
    }

    public TempQueue getQueue(Timespan timespan) {
        return getTuple(timespan).getT2();
    }

    public synchronized Tuple2<AtomicLong, TempQueue> getTuple(Timespan timespan) {
        if (tickDurationQueues.length < timespan.ordinal()) {
            setup();
        }
        Tuple2<AtomicLong, TempQueue> queue = tickDurationQueues[timespan.ordinal()];
        if (timespan.getQueue(this).equals(queue.getT2())) {
            return queue;
        }
        setup();
        return getTuple(timespan);
    }

    public synchronized int getLength(Timespan timespan) {
        return getTuple(timespan).getT2().getQueueSize();
    }
}
