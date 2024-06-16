package me.alek.honey.tracker;

import java.util.*;

public class RegistryHandleTracker<T> implements MarkableHandleTracker<Double, T> {

    private static final int FROM_LONG_MAX_BEFORE_RESET = (int) Math.pow(10, 6);

    private final Map<T, Long> checkHandleTimeMap = new HashMap<>();
    private final Map<T, Integer> checkHandleIndexMap = new HashMap<>();
    private final Map<T, List<TimedNode<T>>> checkHandleRegistry = new HashMap<>();
    private long key;
    private long totalIndex;
    private long totalTime;

    public synchronized List<TimedNode<T>> getRegistryTracking(T key) {
        return checkHandleRegistry.computeIfAbsent(key, (d) -> new ArrayList<>());
    }

    @Override
    public synchronized long registerTimeStart(T key) {
        List<TimedNode<T>> tracking = getRegistryTracking(key);

        TimedNode<T> node;
        tracking.add(node = new TimedNode<>(key, this.key++));
        if (this.key == Long.MAX_VALUE) {
            this.key = 0;
        }

        return node.getID();
    }

    @Override
    public synchronized void registerTimeStop(T key, long registryKey) {
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
        checkHandleIndexMap.put(key, checkHandleIndexMap.getOrDefault(key, 0) + 1);
        checkHandleTimeMap.put(key, checkHandleTimeMap.getOrDefault(key, 0L) + deltaHandleTime);
        totalIndex++;
        totalTime += deltaHandleTime;
        if (totalTime > Long.MAX_VALUE - FROM_LONG_MAX_BEFORE_RESET) {
            totalTime = totalTime / totalIndex;
            totalIndex = 1;
        }
    }

    @Override
    public double getAverageTimePer(T key) {
        return ((double)getCheckHandleTime(key)) / getCheckHandleIndex(key);
    }

    @Override
    public double getAverageTimePerToAverageQuotient(T key) {
        return getAverageTimePer(key) / (((double)totalTime) / totalIndex);
    }

    public long getCheckHandleTime(T key) {
        return checkHandleTimeMap.getOrDefault(key, 0L);
    }

    public int getCheckHandleIndex(T key) {
        return checkHandleIndexMap.getOrDefault(key, 0);
    }

    @Override
    public void remove(T key) {
        checkHandleTimeMap.remove(key);
    }

    public double getTimeToAverageQuotient(T key) {
        return ((double)getCheckHandleTime(key)) / getAverage();
    }

    public double getTimeToTotalQuotient(T key) {
        return ((double)getCheckHandleTime(key)) / getTotal();
    }

    public Set<T> getTrackerNames() {
        return checkHandleTimeMap.keySet();
    }

    public int getTrackerSize() {
        return checkHandleTimeMap.size();
    }

    public double getAverage() {
        return getTotal() / getTrackerSize();
    }

    public double getTotal() {
        return totalTime;
    }

    @Override
    public Set<T> getTrackers() {
        return checkHandleTimeMap.keySet();
    }

    @Override
    public List<T> getTrackersOrdered() {
        List<T> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<T>() {

            @Override
            public int compare(T o1, T o2) {
                return Double.compare(getCheckHandleTime(o1), getCheckHandleTime(o2));
            }
        });
        return trackersList;
    }

    @Override
    public void clear() {
        checkHandleTimeMap.clear();
        checkHandleRegistry.clear();
        checkHandleIndexMap.clear();
        totalTime = 0;
        totalIndex = 0;
        key = 0;
    }

    @Override
    public int getID() {
        return -1;
    }
}
