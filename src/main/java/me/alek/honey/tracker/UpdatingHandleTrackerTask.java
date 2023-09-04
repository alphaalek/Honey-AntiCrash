package me.alek.honey.tracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class UpdatingHandleTrackerTask<I extends Number, T> implements HandleTracker<I, T> {

    public UpdatingHandleTrackerTask(long updateInterval) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(this::update, 0L, updateInterval / 20, TimeUnit.SECONDS);
    }

    public abstract void update();
}
