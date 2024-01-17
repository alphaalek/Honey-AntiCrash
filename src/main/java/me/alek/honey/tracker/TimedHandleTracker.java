package me.alek.honey.tracker;

public interface TimedHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    double getTimeToAverageQuotient(T key, Timespan timespan);

    double getTimeToTotalQuotient(T key, Timespan timespan);

    double getAverage(Timespan timespan);

    double getTotal(Timespan timespan);
}
