package me.alek.honey.compat.spark;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.ServiceProvider;
import me.alek.honey.compat.ServiceValidator;
import me.alek.honey.compat.UnsupportedCompatabilityException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.LazyInit;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;

public class SparkTPSProvider implements ServiceProvider<Spark> {

    public static class SparkValidator implements ServiceValidator {

        public static final ServiceValidator VALIDATOR = new SparkValidator();

        public LazyInit<Boolean> find() {
            return new LazyInit<>(() -> HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin("spark") != null
                    && HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin("spark").isEnabled());
        }
    }

    public static final ServiceProvider<Spark> PROVIDER = new SparkTPSProvider();

    private static final Object registrationLock = new Object();
    private static Spark SPARK = null;

    public Spark get() {
        if (!SparkValidator.VALIDATOR.find().get()) {
            return null;
        }

        try {
            synchronized (registrationLock) {
                if (SPARK != null) {
                    return SPARK;
                }
                SPARK = ServiceProvider.initRegistration(Spark.class);

                return SPARK;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public static double getTPS(StatisticWindow.TicksPerSecond window) {
        if (!SparkValidator.VALIDATOR.find().get()) {
            throw new UnsupportedCompatabilityException();
        }

        DoubleStatistic<StatisticWindow.TicksPerSecond> TPS = PROVIDER.get().tps();
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
