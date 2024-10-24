package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.HoneyTracker;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.IDRegistry;

public class TempIntervalTracker implements HoneyTracker {

    protected final NotSyncTempIntervalQueue[] tickDurationQueues = new NotSyncTempIntervalQueue[Timespan.values().length];
    private final int id;
    private final boolean fixed;

    public TempIntervalTracker(boolean hasId, boolean fixed) {
        if (hasId) {
            this.id = IDRegistry.TRACKER_CONTEXT.acquire();
        } else {
            this.id = -1;
        }
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

    public void add(long amount) {
        add(new TimedNode<>(amount));
    }

    public void add(TimedNode<Long> node) {
        for (NotSyncTempIntervalQueue queue : tickDurationQueues) {
            queue.add(node);
        }
    }

    public long getTotal(Timespan timespan) {
        return getQueue(timespan).getTotal();
    }

    public void clearExceedingNodes(Timespan timespan, long time) {
        getQueue(timespan).clearExceedingNodes(time);
    }

    public synchronized NotSyncTempIntervalQueue getQueue(Timespan timespan) {
        if (tickDurationQueues.length < timespan.ordinal()) {
            setup();
        }

        NotSyncTempIntervalQueue queue = tickDurationQueues[timespan.ordinal()];
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
