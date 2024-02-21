package me.alek.honey.tracker.statistic;

public interface Statistic<I extends Number> {

    I getStatistic(int index);

    I getStatisticDirectly(int index);

    void putStatistic(int index, I value);
}
