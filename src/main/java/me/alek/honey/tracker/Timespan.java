package me.alek.honey.tracker;

import me.alek.honey.tracker.temp.TempFixedQueuedTracker;
import me.alek.honey.tracker.temp.TempQueue;
import me.alek.honey.tracker.temp.TempTimedQueuedTracker;
import me.alek.honey.utility.Array;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public enum Timespan {
    ONE_SECOND(1, false),
    TWO_SECOND(2, false),
    FIVE_SECOND(5, true),
    TEN_SECOND(10, true),
    FIFTEEN_SECOND(15, true),
    HALF_MINUTE(30, true),
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

    public static class Used {
        public static final Set<Timespan> USED = new HashSet<>();
    }

    public static final double MILLIS_TO_NANO_MULTIPLIER = Math.pow(10, 6);
    public static final double TO_NANO_MULTIPLIER = Math.pow(10, 9);
    private final Map<HoneyTracker, TimespanContext> noIDContextMap = new HashMap<>();
    private final Array<TimespanContext> IDContext = new Array<>();
    private final int timespan;

    Timespan(int timespan, boolean used) {
        this.timespan = timespan;
        if (used) Used.USED.add(this);
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
        return (long) (timespan * TO_NANO_MULTIPLIER);
    }

    public synchronized void clearContext(HoneyTracker tracker) {
        noIDContextMap.remove(tracker);

        if (tracker.getID() != -1) {
            IDContext.remove(tracker.getID());
        }
    }
}
