package me.alek.honey.tracker;

import me.alek.honey.HoneyPlayer;

public interface TimedHandleTracker<I extends Number, F extends Number, T> extends HandleTracker<I, T> {

    boolean hasCollected(T key, Timespan timespan);

    F getOfTotalQuotient(T key, Timespan timespan, boolean update);

    F getTimeToAverageQuotient(T key, Timespan timespan, boolean update);

    F getTimeToAverageWithoutQuotient(T key, Timespan timespan, boolean update);

    F getTimeToTotalQuotient(T key, Timespan timespan, boolean update);

    F getAverage(Timespan timespan);

    I getTotal(Timespan timespan, boolean update);

    I getRecentNode(HoneyPlayer player, Timespan timespan, boolean update);

    I getOldestNode(HoneyPlayer player, Timespan timespan, boolean update);

    F getPlayerRate(HoneyPlayer player, Timespan timespan, boolean update);

    F getPlayerRateAcceleration(HoneyPlayer player, boolean update);

    F getPlayerRateAccelerationQuotient(HoneyPlayer player, Timespan timespan, boolean update);

    I getPlayerTotal(HoneyPlayer player, Timespan timespan, boolean update);

    double[] getStatistics(HoneyPlayer player);
}
