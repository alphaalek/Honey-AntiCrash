package me.alek.honey.tracker.updated;

import me.alek.honey.tracker.HandleTracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class UpdatingHandleTrackerTask<I extends Number, T> implements HandleTracker<I, T> {

    private final long updateInterval;

    public UpdatingHandleTrackerTask(long updateInterval) {
        this.updateInterval = updateInterval;
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(this::update, 0L, updateInterval / 20, TimeUnit.SECONDS);
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public abstract void update();
}
