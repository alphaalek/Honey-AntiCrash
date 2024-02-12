package me.alek.honey.netty.packet;

import java.util.HashMap;
import java.util.Map;

public class PacketEventloopStateTimings {

    private final Map<EventloopState, Long> times = new HashMap<>();
    private EventloopState currentState = EventloopState.NO_STATE;
    private long currentStateStart;

    public void startTimings(EventloopState state) {
        startTimings(state, System.currentTimeMillis());
    }

    public synchronized void startTimings(EventloopState state, long time) {
        if (state == EventloopState.NO_STATE) return;

        if (currentState != EventloopState.NO_STATE) stopTimings();

        currentStateStart = time;
        currentState = state;
    }

    public void stopTimings() {
        stopTimings(System.currentTimeMillis());
    }

    public synchronized void stopTimings(long time) {
        if (currentState == EventloopState.NO_STATE) return;

        times.put(currentState, getTimeFor(currentState) + time - currentStateStart);
        currentState = EventloopState.NO_STATE;
    }

    public long getTimeFor(EventloopState state) {
        return times.getOrDefault(state, 0L);
    }

    public EventloopState getCurrentState() {
        return currentState;
    }
}
