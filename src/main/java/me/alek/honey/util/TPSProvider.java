package me.alek.honey.util;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.spark.SparkTPSProvider;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.statics.NumberUtils;

public class TPSProvider {

    public static String getTPS(Timespan timespan) {
        double value = -1;
        if (SparkTPSProvider.SparkValidator.VALIDATOR.find().get()) {
            value = SparkTPSProvider.getTPS(timespan);
        }

        if (value == -1) {
            value = HoneyPlugin.get().getTPSTracker().evaluateTPS(timespan);
        }

        value = NumberUtils.format(value);
        return (value > 20 ? "§a*" : "") + (value < 12 ? "§c" : value < 17 ? "§e" : "§a") + Math.min(20, value);
    }
}
