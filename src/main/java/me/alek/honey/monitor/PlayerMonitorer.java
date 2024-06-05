package me.alek.honey.monitor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.lang.Lang;
import me.alek.honey.tracker.Timespan;

public class PlayerMonitorer implements ScheduledMonitorer {

    private final HoneyPlayer player;

    public PlayerMonitorer(HoneyPlayer player) {
        this.player = player;
    }

    @Override
    public MonitorType getType() {
        return MonitorType.PLAYER;
    }

    @Override
    public String getName() {
        return Lang.getFormatted("monitor.name.player", player.name);
    }

    private double getOfAverageWithout(Timespan timespan) {
        return TrackerUtils.getServer().getOfAverageWithout(player, timespan);
    }

    private double getOfTotal(Timespan timespan) {
        return TrackerUtils.getServer().getOfTotal(player, timespan);
    }

    private String getOfPacketTask(Timespan timespan) {
        return TrackerUtils.getServer().getOfPacketTaskFormatted(player, timespan);
    }

    @Override
    public String getMessage() {
        HoneyPlugin.get().getServerThreadTracker().clearExceedingNodes(player);

        return "§6*§8STQ: §8o.a.w.o.: §7" + getOfAverageWithout(Timespan.TEN_SECOND) + "%§8, §7" + getOfAverageWithout(Timespan.FIFTEEN_SECOND) + "%§8, "
                + "o.h.s.t.: §7" + getOfPacketTask(Timespan.TEN_SECOND) + "§8, §7" + getOfPacketTask(Timespan.FIFTEEN_SECOND) + "§8, "
                + "o.t.: §7" + getOfTotal(Timespan.TEN_SECOND) + "%§8, §7" + getOfTotal(Timespan.FIFTEEN_SECOND) + "%";
    }
}
