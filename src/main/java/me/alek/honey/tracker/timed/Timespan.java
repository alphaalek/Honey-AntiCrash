package me.alek.honey.tracker.timed;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.tracker.HoneyTracker;
import me.alek.honey.tracker.temp.TempFixedQueuedTracker;
import me.alek.honey.tracker.temp.TempQueue;
import me.alek.honey.tracker.temp.TempTimedQueuedTracker;

import java.util.HashMap;
import java.util.Map;

public enum Timespan {
    TEN_SECOND(10),
    FIFTEEN_SECOND(15),
    HALF_MINUTE(30),
    MINUTE(60),
    ;

    public static class TimespanContext {

        private final TempQueue fixed = new TempQueue(true);
        private final TempQueue timed = new TempQueue(false);
        private final AtomicDouble fixedTotal = new AtomicDouble();
        private final AtomicDouble timedTotal = new AtomicDouble();;
        private final int timespan;

        TimespanContext(Timespan timespan) {
            this.timespan = timespan.timespan;

            fixed.prepareFixed(timespan, 10 * this.timespan, this.fixedTotal, 100);
            timed.prepareTimed(timespan, this.timedTotal);
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

        public AtomicDouble getTotal(HoneyTracker tracker) {
            if (isFixed(tracker)) return fixedTotal;
            else return timedTotal;
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

    private synchronized TimespanContext getContext(HoneyTracker tracker) {
        return contextMap.computeIfAbsent(tracker, (d) -> new TimespanContext(this));
    }

    public TempQueue getQueue(HoneyTracker tracker) {
        return getContext(tracker).getQueue(tracker);
    }

    public AtomicDouble getTotal(HoneyTracker tracker) {
        return getContext(tracker).getTotal(tracker);
    }

    public int getTimespan() {
        return timespan;
    }

    public synchronized void clearContext(HoneyTracker tracker) {
        contextMap.remove(tracker);
    }
}
