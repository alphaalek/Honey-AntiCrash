package me.alek.honey.tracker;

import java.util.List;
import java.util.Set;

public interface HandleTracker<I extends Number, T> {

    void remove(T key);

    void registerTime(T key, I value);

    I getTimeToAverageQuotient(T key);

    I getTimeToTotalQuotient(T key);

    int getTrackerSize();

    I getAverage();

    I getTotal();

    Set<T> getTrackers();

    List<T> getTrackersOrdered();
}
