package me.alek.honey.tracker;

public class ServerThreadTrackLimiter {

    private static final double MAX_PER = 60 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
    private static final double MAX_PER_TICKLOOP = 125 * Timespan.MILLIS_TO_NANO_MULTIPLIER;

    private int lastTickloop;
    private double tickloopTime;

    public double getTime(int tickloop, double time) {
        if (lastTickloop != tickloop) {
            this.tickloopTime = 0;
        }
        lastTickloop = tickloop;
        this.tickloopTime += time;

        if (this.tickloopTime > MAX_PER_TICKLOOP) {
            return 0;
        }

        if (time > MAX_PER) {
            return MAX_PER;
        }

        return time;
    }
}
