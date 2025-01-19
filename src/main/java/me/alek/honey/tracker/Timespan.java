package me.alek.honey.tracker;

import me.alek.honey.tracker.temp.AsyncTempIntervalQueue;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import me.alek.honey.util.Array;
import org.bukkit.command.CommandSender;

import java.util.*;

public enum Timespan {
    ONE_SECOND(1, false),
    FIVE_SECOND(5, true),
    TEN_SECOND(10, true),
    FIFTEEN_SECOND(15, true),
    HALF_MINUTE(30, true),
    ;

    public static class TimespanContext {

        private final AsyncTempIntervalQueue fixed;
        private final AsyncTempIntervalQueue timed;
        private final int timespan;
        private final HoneyTracker tracker;

        TimespanContext(Timespan timespan, HoneyTracker tracker) {
            this.timespan = timespan.timespan;
            this.tracker = tracker;

            fixed = new AsyncTempIntervalQueue(timespan, true);
            timed = new AsyncTempIntervalQueue(timespan, false);

            fixed.prepareFixed(timespan, 10 * this.timespan, 100);
        }

        public HoneyTracker getTracker() {
            return tracker;
        }

        public boolean isFixed(HoneyTracker tracker) {
            if (!(tracker instanceof TempIntervalTracker)) {
                throw new UnsupportedOperationException();
            }

            TempIntervalTracker tempQueuedTracker = (TempIntervalTracker) tracker;
            return tempQueuedTracker.isFixed();
        }

        public AsyncTempIntervalQueue getQueue(HoneyTracker tracker) {
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

    public static final Set<Timespan> USED_FOR_TRACKERS = new HashSet<>();

    static {
        for (Timespan timespan : Timespan.values()) {
            if (timespan.isUsed()) {
                USED_FOR_TRACKERS.add(timespan);
            }
        }
    }

    public static final long MILLIS_TO_NANO_MULTIPLIER = (long) Math.pow(10, 6);
    public static final long TO_MILLIS_MULTIPLIER = (long) Math.pow(10, 3);
    public static final long TO_NANO_MULTIPLIER = (long) Math.pow(10, 9);
    private final Map<HoneyTracker, TimespanContext> noIDContextMap = new HashMap<>();
    private final Array<TimespanContext> IDContext = new Array<>();
    private final int timespan;
    private final boolean used;

    Timespan(int timespan, boolean used) {
        this.timespan = timespan;
        this.used = used;
    }

    public boolean isUsed() {
        return used;
    }

    public static void debug(CommandSender sender) {
        for (Timespan timespan : Timespan.values()) {
            sender.sendMessage("Timespan " + timespan);
            sender.sendMessage("No ids: " + timespan.noIDContextMap.size());
            sender.sendMessage("Ids: " + Arrays.stream(timespan.IDContext.getHandle()).filter(Objects::nonNull).count());
            sender.sendMessage("Ids: " + timespan.IDContext);
        }
    }

    private synchronized TimespanContext getContext(HoneyTracker tracker) {
        if (tracker.getID() == -1) {
            return noIDContextMap.computeIfAbsent(tracker, (d) -> new TimespanContext(this, tracker));
        }

        return IDContext.get(tracker.getID(), () -> new TimespanContext(this, tracker));
    }

    public AsyncTempIntervalQueue getQueue(HoneyTracker tracker) {
        return getContext(tracker).getQueue(tracker);
    }

    public long getTotal(HoneyTracker tracker) {
        return getContext(tracker).getTotal(tracker);
    }

    public int getTimespan() {
        return timespan;
    }

    public long getMilliTimespan() {
        return timespan * TO_MILLIS_MULTIPLIER;
    }

    public long getNanoTimespan() {
        return timespan * TO_NANO_MULTIPLIER;
    }

    public synchronized void clearContext(HoneyTracker tracker) {
        noIDContextMap.remove(tracker);

        if (tracker.getID() != -1) {
            IDContext.remove(tracker.getID());
        }
    }
}
