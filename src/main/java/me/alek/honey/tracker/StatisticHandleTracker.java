package me.alek.honey.tracker;

public interface StatisticHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    StatisticProvider getStatisticProvider(T key);

    double[] getStatistics(T key);

    double getStatistic(T key, int index);

    void updateStatistics(T key);

}
