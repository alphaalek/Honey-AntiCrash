package me.alek.honey.tracker;

import me.alek.honey.tracker.temp.TempFixedQueuedTracker;
import me.alek.honey.tracker.temp.TempQueue;
import me.alek.honey.tracker.temp.TempTimedQueuedTracker;
import me.alek.honey.utility.Array;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public enum Timespan {
    TEN_SECOND(10),
    FIFTEEN_SECOND(15),
    HALF_MINUTE(30),
    MINUTE(60),
    ;

    public static class TimespanContext {

        private final TempQueue fixed = new TempQueue(true);
        private final TempQueue timed = new TempQueue(false);
        private final AtomicLong fixedTotal = new AtomicLong();
        private final AtomicLong timedTotal = new AtomicLong();
        private final int timespan;
        private final HoneyTracker tracker;

        TimespanContext(Timespan timespan, HoneyTracker tracker) {
            this.timespan = timespan.timespan;
            this.tracker = tracker;

            fixed.prepareFixed(timespan, 10 * this.timespan, this.fixedTotal, 100);
            timed.prepareTimed(timespan, this.timedTotal);
        }

        public HoneyTracker getTracker() {
            return tracker;
        }

        public boolean isFixed(HoneyTracker tracker) {
            if (tracker instanceof TempFixedQueuedTracker) return true;
            if (tracker instanceof TempTimedQueuedTracker) return false;

            throw new UnsupportedOperationException();
        }

        public TempQueue getQueue(HoneyTracker tracker) {
            if (isFixed(tracker)) return fixed;
            else return timed;
        }

        public AtomicLong getTotal(HoneyTracker tracker) {
            if (isFixed(tracker)) return fixedTotal;
            else return timedTotal;
        }

        public int getTimespan() {
            return timespan;
        }
    }

    private static final double NANO_MULTIPLIER = Math.pow(10, 9);
    private final Map<HoneyTracker, TimespanContext> noIDContextMap = new HashMap<>();
    private final Array<TimespanContext> IDContext = new Array<>();
    private final int timespan;

    Timespan(int timespan) {
        this.timespan = timespan;
    }

    private synchronized TimespanContext getContext(HoneyTracker tracker) {
        if (tracker.getID() == -1)
            return noIDContextMap.computeIfAbsent(tracker, (d) -> new TimespanContext(this, tracker));

        return IDContext.get(tracker.getID(), () -> new TimespanContext(this, tracker));
    }

    public TempQueue getQueue(HoneyTracker tracker) {
        return getContext(tracker).getQueue(tracker);
    }

    public AtomicLong getTotal(HoneyTracker tracker) {
        return getContext(tracker).getTotal(tracker);
    }

    public int getTimespan() {
        return timespan;
    }

    public long getNanoTimespan() {
        return (long) (timespan * NANO_MULTIPLIER);
    }

    public synchronized void clearContext(HoneyTracker tracker) {
        noIDContextMap.remove(tracker);

        if (tracker.getID() != -1) {
            IDContext.remove(tracker.getID());
        }
    }
}
