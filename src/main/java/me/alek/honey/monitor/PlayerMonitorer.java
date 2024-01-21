package me.alek.honey.monitor;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.TempQueue;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.WideTempQueuedHandleTracker;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

public class PlayerMonitorer implements Monitorer {

    private final WideTempQueuedHandleTracker serverThreadTracker = HoneyPlugin.get().getServerThreadHandleTracker();
    private final Player player;

    public PlayerMonitorer(Player player) {
        this.player = player;
    }

    @Override
    public MonitorType getType() {
        return MonitorType.PLAYER;
    }

    @Override
    public String getName() {
        return player.getName();
    }

    private double getOfAverage(Timespan timespan) {
        return NumberConversions.format(serverThreadTracker.getTimeToAverageQuotient(player, timespan) * 100, 1);
    }

    private double getOfAverageWithout(Timespan timespan) {
        return NumberConversions.format(serverThreadTracker.getTimeToAverageWithoutQuotient(player, timespan) * 100, 1);
    }

    private double getOfTotal(Timespan timespan) {
        return NumberConversions.format(serverThreadTracker.getTimeToTotalQuotient(player, timespan) * 100, 1);
    }

    private String getOfPacketTask(Timespan timespan) {
        TempQueue.TimedNode node = serverThreadTracker.getTracker(player).getQueue(timespan).asTimedQueue().peek();
        long diff;
        double total = serverThreadTracker.getPlayerTotal(player, timespan);
        if (node != null && node.getValue() > (
                diff = Math.abs(
                        (timespan.getTimespan() * 1000 - (System.currentTimeMillis() - node.getInsertionInstant()))
                ))) {
            total -= node.getValue() - diff;
        }

        double value = NumberConversions.format(total / (500 * timespan.getTimespan()) * 100, 1);
        return value + "%" + (value < 100 ? "" : "§c⚠");
    }

    @Override
    public String getMessage() {
        return "§6*§8STQ: §8o.a.w.o.: §7" + getOfAverageWithout(Timespan.FIVE_SECOND) + "%§8, §7" + getOfAverageWithout(Timespan.TEN_SECOND) + "%§8, "
                + "o.h.s.t.: §7" + getOfPacketTask(Timespan.FIVE_SECOND) + "§8, §7" + getOfPacketTask(Timespan.TEN_SECOND) + "§8, "
                + "o.t.: §7" + getOfTotal(Timespan.FIVE_SECOND) + "%§8, §7" + getOfTotal(Timespan.TEN_SECOND) + "%";
    }
}
