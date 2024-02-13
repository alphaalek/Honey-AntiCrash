package me.alek.honey.netty.packet;

import com.google.common.eventbus.EventBus;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketEventloopStateTimings {

    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();
    private final Map<EventloopState, Long> times = new HashMap<>();
    private EventloopState currentState = EventloopState.NO_STATE;
    private long currentStateStart;
    private long totalTime;

    public void startTimings(EventloopState state) {
        startTimings(state, System.nanoTime());
    }

    public boolean shouldIgnore(EventloopState state) {
        return state != EventloopState.SERVER_HANDLE && !monitorManager.isPacketsMonitored();
    }

    public synchronized void startTimings(EventloopState state, long time) {
        if (shouldIgnore(state)) return;

        if (currentState != EventloopState.NO_STATE) stopTimings();

        currentStateStart = time;
        currentState = state;
    }

    public void stopTimings() {
        stopTimings(System.nanoTime());
    }

    public synchronized void stopTimings(long time) {
        if (shouldIgnore(currentState)) return;

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
        return /* getTimeFor(EventloopState.SERVER_DECODE) */ + getTimeFor(EventloopState.SERVER_HANDLE);
    }

    public long getHoneyTime() {
        return /* getTimeFor(EventloopState.HONEY_DECODE) */ + getTimeFor(EventloopState.HONEY_HANDLE);
    }

    public double toMillis(long value) {
        return ((double) value) / 1_000_000;
    }

    public EventloopState getCurrentState() {
        return currentState;
    }
}