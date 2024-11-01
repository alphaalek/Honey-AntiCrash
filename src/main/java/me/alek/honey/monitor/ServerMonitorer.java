package me.alek.honey.monitor;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.util.TPSProvider;

public class ServerMonitorer implements ScheduledMonitorer {

    @Override
    public MonitorType getType() {
        return MonitorType.SERVER;
    }

    @Override
    public String getName() {
        return "server monitor";
    }

    private String getPacketTaskPercentage(Timespan timespan) {
        return NumberUtils.format(
                (((double)HoneyPlugin.get().getExecutionQueueProxy().getTotal(timespan, true)) / timespan.getNanoTimespan()) * 100, 1) + "%";
    }

    private double getPacketRatePerQueueExecution(Timespan timespan) {
        double rate = NumberUtils.format(
                HoneyPlugin.get().getLogger().getPacketRatePerQueueExecution(timespan),1);

        return Double.isNaN(rate) ? 0 : rate;
    }

    @Override
    public String getMessage() {
        HoneyPlugin.get().getExecutionQueueProxy().clearExceedingNodes(false, System.currentTimeMillis());

        int packetRate;
        return "§6*§8TPS: §7" + TPSProvider.getTPS(Timespan.TEN_SECOND) + "§7, " + TPSProvider.getTPS(Timespan.FIFTEEN_SECOND) + "§8, "
                + "p.r.: §7" + (packetRate = HoneyPlugin.get().getLogger().getTotalPacketRate()) + " §7pps §8(" + HoneyPlugin.get().getLogger().getTotalPacketRateGrade(packetRate) + "§8)§7, " + getPacketRatePerQueueExecution(Timespan.TEN_SECOND) + " ppqe§8, "
                + "§8o.s.t.: §7" + getPacketTaskPercentage(Timespan.TEN_SECOND) + ", " + getPacketTaskPercentage(Timespan.FIFTEEN_SECOND);
    }
}
