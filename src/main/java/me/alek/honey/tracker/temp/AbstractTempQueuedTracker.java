package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.HoneyTracker;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;

public abstract class AbstractTempQueuedTracker implements HoneyTracker {

    protected final TempQueue[] tickDurationQueues = new TempQueue[Timespan.values().length];
    private final int id;

    public AbstractTempQueuedTracker(boolean hasId) {
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
            tickDurationQueues[i++] = timespan.getQueue(this);
        }
    }

    public void add(double amount) {
        add(new TimedNode<>(amount));
    }

    public void add(TimedNode<Double> node) {
        for (TempQueue queue : tickDurationQueues) {
            queue.add(node);
        }
    }

    public double getTotal(Timespan timespan) {
        return getQueue(timespan).getTotal();
    }

    public void clearExceedingNodes(Timespan timespan, long time) {
        getQueue(timespan).clearExceedingNodes(time);
    }

    public synchronized TempQueue getQueue(Timespan timespan) {
        if (tickDurationQueues.length < timespan.ordinal()) {
            setup();
        }
        TempQueue queue = tickDurationQueues[timespan.ordinal()];
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
