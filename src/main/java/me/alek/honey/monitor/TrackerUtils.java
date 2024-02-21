package me.alek.honey.monitor;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

public class TrackerUtils {

    private static final TrackerAccessor serverThreadTrackerAccessor = new TrackerAccessor(HoneyPlugin.get().getServerThreadHandleTracker());
    private static final TrackerAccessor nettyPacketTrackerAccessor = new TrackerAccessor(HoneyPlugin.get().getNettyPacketHandleTracker());

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

        public double getOfAverage(Player player, Timespan timespan) {
            return NumberConversions.format(tracker.getTimeToAverageQuotient(player, timespan) * 100, 1);
        }

        public double getOfAverageWithout(Player player, Timespan timespan) {
            return NumberConversions.format(tracker.getTimeToAverageWithoutQuotient(player, timespan) * 100, 1);
        }

        public double getOfTotal(Player player, Timespan timespan) {
            return NumberConversions.format(tracker.getTimeToTotalQuotient(player, timespan) * 100, 1);
        }

        public double getOfPacketTask(Player player, Timespan timespan) {
            return NumberConversions.format(tracker.getHalfServerThreadQuotient(player, timespan) * 100, 1);
        }

        public String getOfPacketTaskFormatted(Player player, Timespan timespan) {
            double value = getOfPacketTask(player, timespan);
            return value + "%" + (value < 100 ? "" : "§c⚠");
        }

        public void clearExceedingNodes(Player player) {
            tracker.clearExceedingNodes(player);
        }

        public void clearExceedingNodes(Player player, Timespan timespan) {
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
