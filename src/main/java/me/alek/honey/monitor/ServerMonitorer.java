package me.alek.honey.monitor;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.utility.NumberConversions;

public class ServerMonitorer implements Monitorer {

    @Override
    public MonitorType getType() {
        return MonitorType.SERVER;
    }

    @Override
    public String getName() {
        return "Server";
    }

    private String getTPS(Timespan timespan) {
        double value = NumberConversions.format(HoneyPlugin.get().getTickloop().evaluateTPS(timespan), 1);
        return (value < 13 ? "§c" : value < 17 ? "§e" : "§a") + value;
    }

    private String getPacketTaskPercentage(Timespan timespan) {
        return NumberConversions.format((Math.min(1, HoneyPlugin.get().getServerThreadHandleTracker().getTotal(timespan) / (timespan.getTimespan() * 1000)) * 100), 1) + "%";
    }

    private double getPacketRatePerQueueExecution(Timespan timespan) {
        double rate = NumberConversions.format(HoneyPlugin.get().getLoggerService().getPacketRatePerQueueExecution(timespan),1);
        return Double.isNaN(rate) ? 0 : rate;
    }

    @Override
    public String getMessage() {
        int packetRate;
        return "§6*§8TPS: §7" + getTPS(Timespan.FIVE_SECOND) + ", " + getTPS(Timespan.TEN_SECOND) + "§8, "
                + "p.r.: §7" + (packetRate = HoneyPlugin.get().getLoggerService().getTotalPacketRate()) + " §7pps §8(" + HoneyPlugin.get().getLoggerService().getTotalPacketRateGrade(packetRate) + "§8)§7, " + getPacketRatePerQueueExecution(Timespan.FIVE_SECOND) + " ppqe§8, "
                + "§8o.s.t.: §7" + getPacketTaskPercentage(Timespan.FIVE_SECOND) + ", " + getPacketTaskPercentage(Timespan.TEN_SECOND);
    }
}
