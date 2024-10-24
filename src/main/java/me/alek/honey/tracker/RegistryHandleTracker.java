package me.alek.honey.tracker;

import com.google.common.collect.Sets;
import me.alek.honey.util.Identifiable;
import me.alek.honey.util.Array;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class RegistryHandleTracker<T extends Identifiable> implements KeyedHandleTracker<Double, T> {

    private final Array<Long> handleTime = new Array<>();
    private final Array<Integer> handleIndices = new Array<>();
    private final Array<List<TimedNode<T>>> handleRegistry = new Array<>();
    private final Array<T> keys = new Array<>();
    private int key;
    private long totalIndex;
    private long totalTime;

    public synchronized List<TimedNode<T>> getRegistryTracking(T key) {
        if (!keys.has(key.getID())) {
            keys.set(key.getID(), key);
        }
        return handleRegistry.get(key.getID(), ArrayList::new);
    }

    @Override
    public synchronized int registerTimeStart(T key) {
        List<TimedNode<T>> tracking = getRegistryTracking(key);

        TimedNode<T> node;
        tracking.add(node = new TimedNode<>(key, this.key++));
        if (this.key == Integer.MAX_VALUE) {
            this.key = 0;
        }

        return node.getID();
    }

    @Override
    public synchronized void registerTimeStop(T key, int registryKey) {
        List<TimedNode<T>> tracking = getRegistryTracking(key);
        if (tracking.isEmpty()) {
            return;
        }

        TimedNode<T> trackedNode = null;
        for (int i = 0; i < tracking.size() && trackedNode == null; i++) {
            TimedNode<T> node = tracking.get(i);
            if (node.getID() == registryKey) {
                trackedNode = node;
                tracking.remove(node);
            }
        }
        if (trackedNode == null) {
            return;
        }

        long deltaHandleTime = System.nanoTime() - trackedNode.getInsertionInstant();
        if (totalTime + deltaHandleTime == Long.MAX_VALUE) {
            totalTime = totalTime / totalIndex;
            totalIndex = 1;
        }
        handleIndices.set(key, getHandleIndices(key) + 1);
        handleTime.set(key, getHandleTime(key) + deltaHandleTime);
        totalIndex++;
        totalTime += deltaHandleTime;
    }

    @Override
    public double getAverageTimePer(T key) {
        return ((double) getHandleTime(key)) / getHandleIndices(key);
    }

    @Override
    public double getAverageTimePerToAverageQuotient(T key) {
        return getAverageTimePer(key) / (((double)totalTime) / totalIndex);
    }

    public long getHandleTime(T key) {
        return handleTime.getOr(key, () -> 0L);
    }

    public int getHandleIndices(T key) {
        return handleIndices.getOr(key, () -> 0);
    }

    @Override
    public void remove(T key) {
        handleTime.remove(key);
    }

    @Override
    public boolean has(T key) {
        return handleRegistry.has(key);
    }

    public double getTimeToAverageQuotient(T key) {
        return ((double) getHandleTime(key)) / getAverage();
    }

    public double getTimeToTotalQuotient(T key) {
        return ((double) getHandleTime(key)) / getTotal();
    }

    public int getTrackerSize() {
        return handleTime.size();
    }

    public double getAverage() {
        return getTotal() / getTrackerSize();
    }

    public double getTotal() {
        return totalTime;
    }

    @Override
    public Set<T> getTrackers() {
        return Sets.newHashSet(keys.compact());
    }

    @Override
    public List<T> getTrackersOrdered() {
        List<T> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return Double.compare(getHandleTime(o1), getHandleTime(o2));
            }
        });
        return trackersList;
    }

    @Override
    public void clear() {
        handleTime.clear();
        handleRegistry.clear();
        handleIndices.clear();
        totalTime = 0;
        totalIndex = 0;
        key = 0;
    }

    @Override
    public int getID() {
        return -1;
    }
}
