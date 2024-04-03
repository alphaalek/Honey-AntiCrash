package me.alek.honey.compat;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.Timespan;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class SparkTPSProvider {

    private static final Object registrationLock = new Object();
    private static Spark SPARK = getSpark();

    private static boolean hasSpark() {
        return HoneyPlugin.get().getPlugin().getServer().getPluginManager().isPluginEnabled("spark");
    }

    public static boolean canUseSpark() {
        return hasSpark() && getSpark() != null;
    }

    public static Spark getSpark() {
        if (!hasSpark()) {
            return null;
        }

        try {
            synchronized (registrationLock) {
                return SPARK != null ? SPARK : (SPARK = initRegistration());
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public static Spark initRegistration() {
        RegisteredServiceProvider<Spark> provider = Bukkit.getServicesManager().getRegistration(Spark.class);
        if (provider != null) {
            return provider.getProvider();
        }

        throw new UnsupportedCompatabilityException();
    }

    public static double getTPS(StatisticWindow.TicksPerSecond window) {
        if (!canUseSpark()) {
            throw new UnsupportedCompatabilityException();
        }

        DoubleStatistic<StatisticWindow.TicksPerSecond> TPS = SPARK.tps();
        if (TPS == null) {
            return -1;
        }

        return TPS.poll(window);
    }

    public static double getTPS(Timespan timespan) {
        return getTPS(fromTimespan(timespan));
    }

    public static StatisticWindow.TicksPerSecond fromTimespan(Timespan timespan) {
        switch (timespan) {
            case ONE_SECOND:
            case TWO_SECOND:
            case FIVE_SECOND:
                return StatisticWindow.TicksPerSecond.SECONDS_5;
            case TEN_SECOND:
            case FIFTEEN_SECOND:
                return StatisticWindow.TicksPerSecond.SECONDS_10;
            case HALF_MINUTE:
                return StatisticWindow.TicksPerSecond.MINUTES_1;
        }
        return null;
    }
}
