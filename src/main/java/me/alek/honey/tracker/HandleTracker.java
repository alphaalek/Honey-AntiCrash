package me.alek.honey.tracker;

import java.util.List;
import java.util.Set;

public interface HandleTracker<I extends Number, T> {

    void remove(T key);

    void registerTime(T key, I value);

    double getTimeToAverageQuotient(T key);

    double getTimeToTotalQuotient(T key);

    int getTrackerSize();

    int getOldTrackerSize();

    double getAverage();

    I getTotal();

    Set<T> getTrackers();

    List<T> getTrackersOrdered();
}
