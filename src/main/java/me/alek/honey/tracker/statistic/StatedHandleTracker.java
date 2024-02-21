package me.alek.honey.tracker.statistic;

import me.alek.honey.tracker.HandleTracker;

public interface StatedHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    double getTimeToAverageQuotient(T key);

    double getTimeToTotalQuotient(T key);

    double getAverage();

    double getTotal();
}
