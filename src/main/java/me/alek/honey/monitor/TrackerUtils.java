package me.alek.honey.monitor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.NumberUtils;

public class TrackerUtils {

    private static final TrackerAccessor serverThreadTrackerAccessor = new TrackerAccessor(HoneyPlugin.get().getServerThreadTracker());
    private static final TrackerAccessor nettyPacketTrackerAccessor = new TrackerAccessor(HoneyPlugin.get().getNettyPacketTracker());

    public static TrackerAccessor getServer() {
        return serverThreadTrackerAccessor;
    }

    public static TrackerAccessor getNetty() {
        return nettyPacketTrackerAccessor;
    }

    public static class TrackerAccessor {

        private final WideTempQueuedHandleTracker tracker;

        public TrackerAccessor(WideTempQueuedHandleTracker tracker) {
            this.tracker = tracker;
        }

        public double getOfAverage(HoneyPlayer player, Timespan timespan) {
            return NumberUtils.format(tracker.getTimeToAverageQuotient(player, timespan, true) * 100, 1);
        }

        public double getOfAverageWithout(HoneyPlayer player, Timespan timespan) {
            return NumberUtils.format(tracker.getTimeToAverageWithoutQuotient(player, timespan, true) * 100, 1);
        }

        public double getOfTotal(HoneyPlayer player, Timespan timespan) {
            return NumberUtils.format(tracker.getTimeToTotalQuotient(player, timespan, true) * 100, 1);
        }

        public double getOfPacketTask(HoneyPlayer player, Timespan timespan) {
            return NumberUtils.format(tracker.getHalfServerThreadQuotient(player, timespan, true) * 100, 1);
        }

        public String getOfPacketTaskFormatted(HoneyPlayer player, Timespan timespan) {
            double value = getOfPacketTask(player, timespan);
            return value + "%" + (value < 100 ? "" : "§c⚠");
        }

        public void clearExceedingNodes(HoneyPlayer player) {
            tracker.clearExceedingNodes(player);
        }

        public void clearExceedingNodes(HoneyPlayer player, Timespan timespan) {
            tracker.clearExceedingNodes(player, timespan, System.currentTimeMillis());
        }

        public void clearExceedingNodes(boolean alsoPlayers) {
            tracker.clearExceedingNodes(alsoPlayers, System.currentTimeMillis());
        }

        public void clearExceedingNodes(Timespan timespan, boolean alsoPlayers) {
            tracker.clearExceedingNodes(timespan, alsoPlayers, System.currentTimeMillis());
        }
    }
}
