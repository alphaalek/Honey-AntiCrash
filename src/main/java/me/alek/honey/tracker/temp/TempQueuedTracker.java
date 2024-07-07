package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.HoneyTracker;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.IDRegistry;

import java.io.Serializable;

public class TempQueuedTracker implements HoneyTracker {

    protected final UnsyncTempQueue[] tickDurationQueues = new UnsyncTempQueue[Timespan.values().length];
    private final int id;
    private final boolean fixed;

    public TempQueuedTracker(boolean hasId, boolean fixed) {
        if (hasId) id = IDRegistry.TRACKER_CONTEXT.acquire();
        else id = -1;

        this.fixed = fixed;
        setup();
    }

    public boolean isFixed() {
        return this.fixed;
    }

    public int getID() {
        return id;
    }

    private synchronized void setup() {
        int i = 0;
        for (Timespan timespan : Timespan.values()) {
            tickDurationQueues[i++] = timespan.getQueue(this);
        }
    }

    public void add(double amount) {
        add(new TimedNode<>(amount));
    }

    public void add(TimedNode<Double> node) {
        for (UnsyncTempQueue queue : tickDurationQueues) {
            queue.add(node);
        }
    }

    public double getTotal(Timespan timespan) {
        return getQueue(timespan).getTotal();
    }

    public void clearExceedingNodes(Timespan timespan, long time) {
        getQueue(timespan).clearExceedingNodes(time);
    }

    public synchronized UnsyncTempQueue getQueue(Timespan timespan) {
        if (tickDurationQueues.length < timespan.ordinal()) {
            setup();
        }

        UnsyncTempQueue queue = tickDurationQueues[timespan.ordinal()];
        if (timespan.getQueue(this).equals(queue)) {
            return queue;
        }
        setup();

        return getQueue(timespan);
    }

    public synchronized int getLength(Timespan timespan) {
        return getQueue(timespan).getQueueSize();
    }
}
