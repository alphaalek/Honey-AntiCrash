package me.alek.honey.tracker;

import me.alek.honey.HoneyPlayer;

public interface TimedManualHandleTracker<I extends Number, F extends Number, T> extends TimedHandleTracker<I, F, T> {

    void clearExceedingNodes(HoneyPlayer player);

    void clearExceedingNodes(HoneyPlayer player, Timespan timespan, long time);

    void clearExceedingNodes(boolean alsoPlayers, long time);

    void clearExceedingNodes(Timespan timespan, boolean alsoPlayers, long time);
}
