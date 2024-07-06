package me.alek.honey.tracker;

import me.alek.honey.HoneyPlayer;

public interface TimedHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    boolean hasCollected(T key, Timespan timespan);

    I getOfTotalQuotient(T key, Timespan timespan, boolean update);

    I getTimeToAverageQuotient(T key, Timespan timespan, boolean update);

    I getTimeToAverageWithoutQuotient(T key, Timespan timespan, boolean update);

    I getTimeToTotalQuotient(T key, Timespan timespan, boolean update);

    I getAverage(Timespan timespan);

    I getTotal(Timespan timespan, boolean update);

    I getRecentNode(HoneyPlayer player, Timespan timespan, boolean update);

    I getOldestNode(HoneyPlayer player, Timespan timespan, boolean update);

    I getPlayerRate(HoneyPlayer player, Timespan timespan, boolean update);

    I getPlayerRateAcceleration(HoneyPlayer player, boolean average, boolean update);

    I getPlayerRateAccelerationQuotient(HoneyPlayer player, Timespan timespan, boolean average, boolean update);

    I getPlayerTotal(HoneyPlayer player, Timespan timespan, boolean update);

    I[] getStatistics(HoneyPlayer player);
}
