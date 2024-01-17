package me.alek.honey.tracker;

import java.util.List;
import java.util.Set;

public interface HandleTracker<I extends Number, T> extends HoneyTracker {

    void remove(T key);

    void registerTime(T key, I value);

    int getTrackerSize();

    Set<T> getTrackers();

    List<T> getTrackersOrdered();

    void clear();
}
