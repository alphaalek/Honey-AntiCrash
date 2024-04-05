package me.alek.honey.tracker;

import me.alek.honey.tracker.temp.impl.TempFixedQueuedTracker;
import me.alek.honey.tracker.temp.TempQueue;
import me.alek.honey.tracker.temp.impl.TempTimedQueuedTracker;
import me.alek.honey.util.Array;
import org.bukkit.command.CommandSender;

import java.util.*;

public enum Timespan {
    ONE_SECOND(1, false),
    TWO_SECOND(2, false),
    FIVE_SECOND(5, true),
    TEN_SECOND(10, true),
    FIFTEEN_SECOND(15, true),
    HALF_MINUTE(30, true),
    ;

    public static class TimespanContext {

        private final TempQueue fixed;
        private final TempQueue timed;
        private final int timespan;
        private final HoneyTracker tracker;

        TimespanContext(Timespan timespan, HoneyTracker tracker) {
            this.timespan = timespan.timespan;
            this.tracker = tracker;

            fixed = new TempQueue(timespan, true);
            timed = new TempQueue(timespan, false);

            fixed.prepareFixed(timespan, 10 * this.timespan, 100);
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

        public long getTotal(HoneyTracker tracker) {
            if (isFixed(tracker)) return fixed.getTotal();
            else return timed.getTotal();
        }

        public int getTimespan() {
            return timespan;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (fixed.getTotal() != 10L * this.timespan * 100L) {
                builder.append("Fixed{total=" + fixed.getTotal() + ",queueSize=" + fixed.getQueueSize() + "}");
            }
            if (timed.getQueueSize() > 0) {
                builder.append("Timed{total=" + timed.getTotal() + ",queueSize=" + timed.getQueueSize() + "}");
            }

            if (builder.toString().isEmpty()) {
                return "No data";
            }
            return builder.toString();
        }
    }

    public static class Used {
        public static final Set<Timespan> USED_FOR_TRACKERS = new HashSet<>();
    }

    public static final double MILLIS_TO_NANO_MULTIPLIER = Math.pow(10, 6);
    public static final double TO_NANO_MULTIPLIER = Math.pow(10, 9);
    private final Map<HoneyTracker, TimespanContext> noIDContextMap = new HashMap<>();
    private final Array<TimespanContext> IDContext = new Array<>();
    private final int timespan;

    Timespan(int timespan, boolean used) {
        this.timespan = timespan;

        if (used) {
            Used.USED_FOR_TRACKERS.add(this);
        }
    }

    public static void debug(CommandSender sender) {
        for (Timespan timespan : Timespan.values()) {
            sender.sendMessage("Timespan " + timespan);
            sender.sendMessage("No ids: " + timespan.noIDContextMap.size());
            sender.sendMessage("Ids: " + Arrays.stream(timespan.IDContext.toRealArray()).filter(Objects::nonNull).count());
            sender.sendMessage("Ids: " + timespan.IDContext);
        }
    }

    private synchronized TimespanContext getContext(HoneyTracker tracker) {
        if (tracker.getID() == -1) {
            return noIDContextMap.computeIfAbsent(tracker, (d) -> new TimespanContext(this, tracker));
        }

        return IDContext.get(tracker.getID(), () -> new TimespanContext(this, tracker));
    }

    public TempQueue getQueue(HoneyTracker tracker) {
        return getContext(tracker).getQueue(tracker);
    }

    public long getTotal(HoneyTracker tracker) {
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
