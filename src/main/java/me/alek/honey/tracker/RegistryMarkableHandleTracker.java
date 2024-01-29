package me.alek.honey.tracker;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class RegistryMarkableHandleTracker<T> extends UpdatingHandleTrackerTask<Double, T> implements MarkableHandleTracker<Double, T> {

    private final AtomicLong totalCheckHandleTime = new AtomicLong(0);
    private final Map<T, Long> checkHandleTimeMap = new HashMap<>();
    private final Map<T, Deque<Long>> checkHandleRegistryDeque = new HashMap<>();

    public RegistryMarkableHandleTracker(long updateInterval) {
        super(updateInterval);
    }

    public synchronized Deque<Long> getCheckRegistryQueue(T key) {
        return checkHandleRegistryDeque.computeIfAbsent(key, (d) -> new LinkedList<>());
    }

    @Override
    public synchronized void registerTimeStart(T key) {
        Deque<Long> checkRegistryDeque = getCheckRegistryQueue(key);

        checkRegistryDeque.add(System.nanoTime());
    }

    @Override
    public synchronized void registerTimeStop(T key) {
        Deque<Long> checkRegistryDeque = getCheckRegistryQueue(key);

        if (checkRegistryDeque.isEmpty()) return;

        long randomUnixTime = checkRegistryDeque.poll();
        long deltaHandleTime = System.nanoTime() - randomUnixTime;

        totalCheckHandleTime.addAndGet(deltaHandleTime);
        checkHandleTimeMap.put(key, checkHandleTimeMap.getOrDefault(key, 0L) + deltaHandleTime);
    }

    public long getCheckHandleTime(T getKey) {
        return checkHandleTimeMap.getOrDefault(getKey, 0L);
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
        return (double) totalCheckHandleTime.get();
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
        totalCheckHandleTime.set(0);
    }

    @Override
    public synchronized void update() {
        long totalReferencedTime = 0;
        for (long referencedTime : checkHandleTimeMap.values()) {
            totalReferencedTime += referencedTime;
        }
        totalCheckHandleTime.set(totalReferencedTime);
    }
}
