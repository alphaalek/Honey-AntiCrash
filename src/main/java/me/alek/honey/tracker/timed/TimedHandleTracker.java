package me.alek.honey.tracker.timed;

import me.alek.honey.tracker.HandleTracker;

public interface TimedHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    double getTimeToAverageQuotient(T key, Timespan timespan, boolean update);

    double getTimeToAverageWithoutQuotient(T key, Timespan timespan, boolean update);

    double getTimeToTotalQuotient(T key, Timespan timespan, boolean update);

    double getAverage(Timespan timespan);

    double getTotal(Timespan timespan, boolean update);
}
