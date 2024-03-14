package me.alek.honey.tracker.statistic;

import me.alek.honey.tracker.statistic.StatedHandleTracker;
import me.alek.honey.tracker.statistic.StatisticHandleTracker;
import me.alek.honey.tracker.statistic.StatisticProvider;

import java.util.*;

public class WrappedStatisticHandleTrackerDelegate<T> implements StatisticHandleTracker<Double, T> {

    private final int[] statisticInSecondsIndices;
    private final Map<T, StatisticProvider> statisticProviders = new HashMap<>();

    private final StatedHandleTracker<Double, T> handleTracker;

    public WrappedStatisticHandleTrackerDelegate(StatedHandleTracker<Double, T> handleTracker, int... statisticInSecondsIndices) {
        this.handleTracker = handleTracker;
        this.statisticInSecondsIndices = statisticInSecondsIndices;
    }

    public synchronized StatisticProvider getStatisticProvider(T key) {
        return statisticProviders.computeIfAbsent(key, (d) -> new StatisticProvider(statisticInSecondsIndices));
    }

    public Double[] getStatistics(T key) {
        Double[] statistics = new Double[statisticInSecondsIndices.length];

        for (int i = 0; i < statisticInSecondsIndices.length; i++) {
            statistics[i] = getStatistic(key, i);
        }
        return statistics;
    }

    @Override
    public Double getStatisticDirectly(T key, int index) {
        return getStatisticProvider(key).getStatisticDirectly(index);
    }

    public Double getStatistic(T key, int index) {
        return getStatisticProvider(key).getStatistic(index);
    }

    @Override
    public Double tryUpdateAndGetStatistic(T key, int index) {
        putStatistic(key, index, handleTracker.getTimeToAverageQuotient(key));
        forceUpdateStatistic(key, index);
        return getStatistic(key, index);
    }

    @Override
    public Double[] tryUpdateAndGetStatistics(T key) {
        Double[] statistics = new Double[statisticInSecondsIndices.length];

        for (int i = statisticInSecondsIndices.length - 1; i >= 0; i--) {
            statistics[statisticInSecondsIndices.length - i - 1] = tryUpdateAndGetStatistic(key, i);
        }
        return statistics;
    }

    public void updateStatistics(T key) {
        getStatisticProvider(key).checkUpdateStatistics();
    }

    @Override
    public void updateStatistic(T key, int index) {
        getStatisticProvider(key).forceUpdateStatistic(index);
    }

    private void forceUpdateStatistic(T key, int index) {
        getStatisticProvider(key).forceUpdateStatistic(index);
    }

    public void putStatistic(T key, int index, Double value) {
        getStatisticProvider(key).putStatistic(index, value);
    }

    public void registerToStatistics(T key, Double value) {
        for (int i = 0; i < statisticInSecondsIndices.length; i++) {
            putStatistic(key, i, value);
        }
    }

    @Override
    public void clear() {
        statisticProviders.clear();
    }

    @Override
    public void remove(T key) {
        statisticProviders.remove(key);
    }

    @Override
    public void registerTime(T key, Double value) {
        handleTracker.registerTime(key, value);
    }

    @Override
    public int getTrackerSize() {
        return handleTracker.getTrackerSize();
    }

    @Override
    public Set<T> getTrackers() {
        return handleTracker.getTrackers();
    }

    @Override
    public List<T> getTrackersOrdered() {
        return handleTracker.getTrackersOrdered();
    }

    @Override
    public int getID() {
        return -1;
    }
}
