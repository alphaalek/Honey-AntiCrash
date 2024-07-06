package me.alek.honey.tracker.temp.dummy;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.tracker.TimedManualHandleTracker;
import me.alek.honey.tracker.Timespan;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DummyTracker implements TimedManualHandleTracker<Double, HoneyPlayer> {

    @Override
    public void remove(HoneyPlayer key) {

    }

    @Override
    public boolean has(HoneyPlayer key) {
        return false;
    }

    @Override
    public void registerTime(HoneyPlayer key, Double value) {

    }

    @Override
    public int getTrackerSize() {
        return 0;
    }

    @Override
    public Set<HoneyPlayer> getTrackers() {
        return Collections.emptySet();
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        return Collections.emptyList();
    }

    @Override
    public void clear() {

    }

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public boolean hasCollected(HoneyPlayer key, Timespan timespan) {
        return false;
    }

    @Override
    public Double getOfTotalQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getTimeToAverageQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getTimeToAverageWithoutQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getTimeToTotalQuotient(HoneyPlayer key, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getAverage(Timespan timespan) {
        return 0d;
    }

    @Override
    public Double getTotal(Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getRecentNode(HoneyPlayer player, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getOldestNode(HoneyPlayer player, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getPlayerRate(HoneyPlayer player, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double getPlayerRateAcceleration(HoneyPlayer player, boolean average, boolean update) {
        return 0d;
    }

    @Override
    public Double getPlayerRateAccelerationQuotient(HoneyPlayer player, Timespan timespan, boolean average, boolean update) {
        return 0d;
    }

    @Override
    public Double getPlayerTotal(HoneyPlayer player, Timespan timespan, boolean update) {
        return 0d;
    }

    @Override
    public Double[] getStatistics(HoneyPlayer player) {
        return new Double[0];
    }

    @Override
    public void clearExceedingNodes(HoneyPlayer player) {

    }

    @Override
    public void clearExceedingNodes(HoneyPlayer player, Timespan timespan, long time) {

    }

    @Override
    public void clearExceedingNodes(boolean alsoPlayers, long time) {

    }

    @Override
    public void clearExceedingNodes(Timespan timespan, boolean alsoPlayers, long time) {

    }
}
