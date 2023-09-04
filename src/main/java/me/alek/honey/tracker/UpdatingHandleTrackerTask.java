package me.alek.honey.tracker;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class UpdatingHandleTrackerTask<I extends Number, T> implements HandleTracker<I, T> {

    public UpdatingHandleTrackerTask(JavaPlugin plugin, long updateInterval) {

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (this) { update(); }
            }
        };
        runnable.runTaskTimerAsynchronously(plugin, 0L, updateInterval);
    }

    public abstract void update();

}
