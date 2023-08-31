package me.alek.honey.tracker.impl;

import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.UpdatingHandleTrackerTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class CheckHandleTracker extends UpdatingHandleTrackerTask<Double, String> implements MarkableHandleTracker<Double, String> {

    private final AtomicLong totalCheckHandleTime = new AtomicLong(0);
    private final Map<String, Long> checkHandleTimeMap = new HashMap<>();
    private final Map<String, Deque<Long>> checkHandleRegistryDeque = new HashMap<>();

    public CheckHandleTracker(JavaPlugin plugin) {
        super(plugin, 200L);
    }

    public synchronized Deque<Long> getCheckRegistryQueue(String checkName) {
        return checkHandleRegistryDeque.computeIfAbsent(checkName, (d) -> new LinkedList<>());
    }

    @Override
    public synchronized void registerTimeStart(String checkName) {
        Deque<Long> checkRegistryDeque = getCheckRegistryQueue(checkName);

        checkRegistryDeque.add(System.nanoTime());
    }

    @Override
    public synchronized void registerTimeStop(String checkName) {
        Deque<Long> checkRegistryDeque = getCheckRegistryQueue(checkName);

        if (checkRegistryDeque.size() == 0) return;

        long randomUnixTime = checkRegistryDeque.poll();
        long deltaHandleTime = System.nanoTime() - randomUnixTime;

        totalCheckHandleTime.addAndGet(deltaHandleTime);
        checkHandleTimeMap.put(checkName, checkHandleTimeMap.getOrDefault(checkName, 0L) + deltaHandleTime);
    }

    public long getCheckHandleTime(String checkName) {
        return checkHandleTimeMap.getOrDefault(checkName, 0L);
    }

    @Override
    public void remove(String key) {
        checkHandleTimeMap.remove(key);
    }

    public Double getTimeToAverageQuotient(String checkName) {
        return ((double)getCheckHandleTime(checkName)) / getAverage();
    }

    @Override
    public Double getTimeToTotalQuotient(String checkName) {
        return ((double)getCheckHandleTime(checkName)) / getTotal();
    }

    public Set<String> getTrackerNames() {
        return checkHandleTimeMap.keySet();
    }

    public int getTrackerSize() {
        return checkHandleTimeMap.size();
    }

    @Override
    public Double getAverage() {
        return getTotal() / getTrackerSize();
    }

    @Override
    public Double getTotal() {
        return (double) totalCheckHandleTime.get();
    }

    @Override
    public Set<String> getTrackers() {
        return checkHandleTimeMap.keySet();
    }

    @Override
    public List<String> getTrackersOrdered() {
        List<String> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return Double.compare(getCheckHandleTime(o1), getCheckHandleTime(o2));
            }
        });
        return trackersList;
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
