package me.alek.honey.netty.packet;

import me.alek.honey.util.statics.NumberUtils;

public class PacketLifecycleTimings {

    public static final double NANOS_TO_MILLIS_MULTIPLIER = Math.pow(10, -6);
    private static final int LENGTH = LifecycleState.values().length;

    private final long[] times = new long[LENGTH];
    private LifecycleState currentState = LifecycleState.NO_STATE;
    private long currentStateStart;
    private long totalTime;

    public void startTimings(LifecycleState state) {
        startTimings(state, System.nanoTime());
    }

    public synchronized void startTimings(LifecycleState state, long time) {
        if (this.currentState != LifecycleState.NO_STATE) {
            stopTimings();
        }

        this.currentStateStart = time;
        this.currentState = state;
    }

    public void stopTimings() {
        stopTimings(System.nanoTime());
    }

    public synchronized void stopTimings(long time) {
        if (this.currentState == LifecycleState.NO_STATE) {
            return;
        }

        long deltaTime = time - this.currentStateStart;
        this.times[this.currentState.ordinal()] = getTimeFor(this.currentState) + deltaTime;
        this.totalTime += deltaTime;

        this.currentState = LifecycleState.NO_STATE;
    }

    public long getTotalTime() {
        return this.totalTime;
    }

    public long getTimeFor(LifecycleState state) {
        return this.times[state.ordinal()];
}

    public double getTimeForMillis(LifecycleState state) {
        return toMillis(getTimeFor(state));
    }

    public double getTimeForMillisFormatted(LifecycleState state) {
        return NumberUtils.format(getTimeForMillis(state), 2);
    }

    private double toMillis(long value) {
        return ((double)value) * NANOS_TO_MILLIS_MULTIPLIER;
    }

    public LifecycleState getCurrentState() {
        return this.currentState;
    }
}