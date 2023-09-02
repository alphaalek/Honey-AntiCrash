package me.alek.honey.tracker;

import java.util.HashMap;
import java.util.Map;

public class StatisticProvider implements Statistic<Double> {

    private StatisticWindow DEFAULT_STATISTIC_WINDOW;

    private final int[] statisticInSecondsIndices;
    private final Map<Integer, StatisticWindow> statisticWindows = new HashMap<>();

    public StatisticProvider(int... statisticInSecondsIndices) {
        this.statisticInSecondsIndices = statisticInSecondsIndices;

        for (int i = 0; i < statisticInSecondsIndices.length; i++) {

            StatisticWindow statisticWindow = new StatisticWindow(i, statisticInSecondsIndices[i]);
            if (DEFAULT_STATISTIC_WINDOW == null) DEFAULT_STATISTIC_WINDOW = statisticWindow;

            statisticWindows.put(i, statisticWindow);
        }
    }

    private StatisticWindow getStatisticWindow(int index) {
        return statisticWindows.getOrDefault(index, DEFAULT_STATISTIC_WINDOW);
    }

    @Override
    public Double getStatistic(int index) {
        return getStatisticWindow(index).average();
    }

    @Override
    public void putStatistic(int index, Double value) {
        getStatisticWindow(index).putMean(value);
    }


    public void updateStatistics() {
        for (StatisticWindow statisticWindow : statisticWindows.values()) {

            if (statisticWindow.shouldUpdate()) {
                statisticWindow.updateAverage();
            }
        }
    }

    public int[] getStatisticInSecondsIndices() {
        return statisticInSecondsIndices;
    }

    private class StatisticWindow {

        private final int statisticInSeconds;
        private final int statisticWindowIndex;

        private long lastStatisticUpdate;

        private double average;

        private int currentMeanSize;
        private double currentTotalFlatMean;

        public StatisticWindow(int statisticWindowIndex, int statisticInSeconds) {
            this.statisticWindowIndex = statisticWindowIndex;
            this.statisticInSeconds = statisticInSeconds;
        }

        public boolean shouldUpdate() {
            return (System.currentTimeMillis() - lastStatisticUpdate) / 1_000 >= statisticInSeconds;
        }

        public void putMean(double value) {
            currentMeanSize++;
            currentTotalFlatMean += value;
        }

        private double getAverageInBelowStatistic() {
            if (statisticWindowIndex == 0) return 1d;

            return getStatisticWindow(statisticWindowIndex - 1).average();
        }

        private double evaluateAverage() {
            if (currentMeanSize == 0 || currentTotalFlatMean == 0) return getAverageInBelowStatistic();

            return currentTotalFlatMean / currentMeanSize;
        }

        public void updateAverage() {
            average = evaluateAverage();
            currentMeanSize = 0;
            currentTotalFlatMean = 0;

            lastStatisticUpdate = System.currentTimeMillis();
        }

        public double average() {
            if (average == 0) return evaluateAverage();

            return average;
        }
    }
}
