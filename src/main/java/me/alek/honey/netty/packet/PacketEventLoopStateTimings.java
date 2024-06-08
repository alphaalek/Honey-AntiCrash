package me.alek.honey.netty.packet;

import me.alek.honey.util.NumberUtils;

public class PacketEventLoopStateTimings {

    private static final int LENGTH = EventloopState.values().length;
    private final long[] times = new long[LENGTH];
    private EventloopState currentState = EventloopState.NO_STATE;
    private long currentStateStart;
    private long totalTime;
    public static final double NANOS_TO_MILLIS_MULTIPLIER = Math.pow(10, -6);

    public void startTimings(EventloopState state) {
        startTimings(state, System.nanoTime());
    }

    public synchronized void startTimings(EventloopState state, long time) {
        if (currentState != EventloopState.NO_STATE) {
            stopTimings();
        }

        currentStateStart = time;
        currentState = state;
    }

    public void stopTimings() {
        stopTimings(System.nanoTime());
    }

    public synchronized void stopTimings(long time) {
        if (currentState == EventloopState.NO_STATE) {
            return;
        }

        long deltaTime = time - currentStateStart;
        times[currentState.ordinal()] = getTimeFor(currentState) + deltaTime;
        totalTime += deltaTime;

        currentState = EventloopState.NO_STATE;
    }

    public String formatPerformance() {
        return NumberUtils.format(getPerformance(), 2) + "%";
    }

    public double getPerformance() {
        return (((double) getHoneyTime()) / getTotalTime()) * 100;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getTimeFor(EventloopState state) {
        return times[state.ordinal()];
}

    public double getTimeForMillis(EventloopState state) {
        return toMillis(getTimeFor(state));
    }

    public double getTimeForMillisFormatted(EventloopState state) {
        return NumberUtils.format(getTimeForMillis(state), 2);
    }

    public long getServerTime() {
        return getTimeFor(EventloopState.SERVER_DECODE) + getTimeFor(EventloopState.SERVER_HANDLE);
    }

    public long getHoneyTime() {
        return getTimeFor(EventloopState.HONEY_DECODE) + getTimeFor(EventloopState.HONEY_HANDLE);
    }

    private double toMillis(long value) {
        return ((double)value) * NANOS_TO_MILLIS_MULTIPLIER;
    }

    public EventloopState getCurrentState() {
        return currentState;
    }
}