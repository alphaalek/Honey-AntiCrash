package me.alek.honey.tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public enum Timespan {
    FIVE_SECOND(5),
    TEN_SECOND(10),
    FIFTEEN_SECOND(15),
    HALF_MINUTE(30);

    public static class TimespanContext {

        private final TempQueue fixed = new TempQueue(true);
        private final TempQueue timed = new TempQueue(false);
        private final AtomicInteger total;
        private final int timespan;

        TimespanContext(Timespan timespan, Supplier<AtomicInteger> total) {
            this.timespan = timespan.timespan;
            this.total = new AtomicInteger(1000 * timespan.timespan);

            fixed.prepareFixed(timespan.timespan, 100);
            timed.prepareTimed(timespan, total);
        }

        public TempQueue getQueue(HoneyTracker tracker) {
            if (tracker instanceof TempFixedQueuedTracker) return fixed;
            if (tracker instanceof TempTimedQueuedTracker) return timed;

            throw new UnsupportedOperationException();
        }

        public AtomicInteger getTotal() {
            return total;
        }

        public int getTimespan() {
            return timespan;
        }
    }

    private final Map<HoneyTracker, TimespanContext> contextMap = new HashMap<>();

    private final int timespan;

    Timespan(int timespan) {
        this.timespan = timespan;
    }

    private TimespanContext getContext(HoneyTracker tracker) {
        return contextMap.computeIfAbsent(tracker, (d) -> new TimespanContext(this, () -> getTotal(tracker)));
    }

    public TempQueue getQueue(HoneyTracker tracker) {
        return getContext(tracker).getQueue(tracker);
    }

    public AtomicInteger getTotal(HoneyTracker tracker) {
        return getContext(tracker).getTotal();
    }

    public int getTimespan() {
        return timespan;
    }

    public void clearContext(HoneyTracker tracker) {
        contextMap.remove(tracker);
    }
}
