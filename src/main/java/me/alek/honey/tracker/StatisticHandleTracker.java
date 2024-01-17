package me.alek.honey.tracker;

public interface StatisticHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    StatisticProvider getStatisticProvider(T key);

    I[] getStatistics(T key);

    I getStatisticDirectly(T key, int index);

    I getStatistic(T key, int index);

    I tryUpdateAndGetStatistic(T key, int index);

    I[] tryUpdateAndGetStatistics(T key);

    void updateStatistics(T key);

    void updateStatistic(T key, int index);

    void putStatistic(T key, int index, I value);

    void registerToStatistics(T key, I value);

}
