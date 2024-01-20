package me.alek.honey.monitor;

import jdk.nashorn.internal.ir.TernaryNode;
import me.alek.honey.HoneyPlugin;
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

    private double getToAverage(Timespan timespan) {
        return NumberConversions.format(serverThreadTracker.getTimeToAverageQuotient(player, timespan) * 100, 1);
    }

    private double getToAverageWithout(Timespan timespan) {
        return NumberConversions.format(serverThreadTracker.getTimeToAverageWithoutQuotient(player, timespan) * 100, 1);
    }

    private double getToTotal(Timespan timespan) {
        return NumberConversions.format(serverThreadTracker.getTimeToTotalQuotient(player, timespan) * 100, 1);
    }

    private String getToVanilla(Timespan timespan) {
        double value = NumberConversions.format(serverThreadTracker.getPlayerTotal(player, timespan) / (375 * (timespan.ordinal() + 1)) * 100, 1);
        return value + "%" + (value < 250 ? "" : "§c⚠");
    }

    @Override
    public String getMessage() {
        return "§6*§8STQ: §8avg. w.o.: §7" + getToAverageWithout(Timespan.FIVE_SECOND) + "%§8, §7" + getToAverageWithout(Timespan.TEN_SECOND) + "%§8, "
                + "t.h.v.: §7" + getToVanilla(Timespan.FIVE_SECOND) + "§8, §7" + getToVanilla(Timespan.TEN_SECOND) + "§8, "
                + "t.t.: §7" + getToTotal(Timespan.FIVE_SECOND) + "%§8, §7" + getToTotal(Timespan.TEN_SECOND) + "%";
    }
}
