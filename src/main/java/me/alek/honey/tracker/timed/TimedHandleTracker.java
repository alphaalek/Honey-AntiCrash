package me.alek.honey.tracker.timed;

import me.alek.honey.tracker.HandleTracker;
import me.alek.honey.tracker.timed.Timespan;

public interface TimedHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    double getTimeToAverageQuotient(T key, Timespan timespan);

    double getTimeToAverageWithoutQuotient(T key, Timespan timespan);

    double getTimeToTotalQuotient(T key, Timespan timespan);

    double getAverage(Timespan timespan);

    double getTotal(Timespan timespan);
}
