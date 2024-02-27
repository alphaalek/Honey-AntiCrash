package me.alek.honey.netty.packet;

import me.alek.honey.utility.NumberConversions;

import java.util.HashMap;
import java.util.Map;

public class PacketEventloopStateTimings {

    private final Map<EventloopState, Long> times = new HashMap<>();
    private EventloopState currentState = EventloopState.NO_STATE;
    private long currentStateStart;
    private long totalTime;

    public void startTimings(EventloopState state) {
        startTimings(state, System.nanoTime());
    }

    public synchronized void startTimings(EventloopState state, long time) {
        if (currentState != EventloopState.NO_STATE) stopTimings();

        currentStateStart = time;
        currentState = state;
    }

    public void stopTimings() {
        stopTimings(System.nanoTime());
    }

    public synchronized void stopTimings(long time) {
        if (currentState == EventloopState.NO_STATE) return;

        long deltaTime = time - currentStateStart;
        times.put(currentState, getTimeFor(currentState) + deltaTime);
        totalTime += deltaTime;

        currentState = EventloopState.NO_STATE;
    }

    public String formatPerformance() {
        return NumberConversions.format(getPerformance(), 2) + "%";
    }

    public double getPerformance() {
        return (((double) getHoneyTime()) / getTotalTime()) * 100;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getTimeFor(EventloopState state) {
        return times.getOrDefault(state, 0L);
    }

    public double getTimeForMillis(EventloopState state) {
        return toMillis(getTimeFor(state));
    }

    public long getServerTime() {
        return getTimeFor(EventloopState.SERVER_DECODE) + getTimeFor(EventloopState.SERVER_HANDLE);
    }

    public long getHoneyTime() {
        return getTimeFor(EventloopState.HONEY_DECODE) + getTimeFor(EventloopState.HONEY_HANDLE);
    }

    private double toMillis(long value) {
        return Math.pow(value, -6);
    }

    public EventloopState getCurrentState() {
        return currentState;
    }
}