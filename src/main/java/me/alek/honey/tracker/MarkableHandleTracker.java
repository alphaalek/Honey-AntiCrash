package me.alek.honey.tracker;

import me.alek.honey.tracker.statistic.StatedHandleTracker;

public interface MarkableHandleTracker<I extends Number, T> extends StatedHandleTracker<I, T> {

    default void registerTime(T key, I value) {
        throw new UnsupportedOperationException();
    }

    int registerTimeStart(T key);

    void registerTimeStop(T key, int registryKey);

    double getAverageTimePer(T key);

    double getAverageTimePerToAverageQuotient(T key);
}
