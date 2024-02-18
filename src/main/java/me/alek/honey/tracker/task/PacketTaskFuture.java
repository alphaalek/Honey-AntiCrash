package me.alek.honey.tracker.task;

import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.FutureTask;

public class PacketTaskFuture<V> extends FutureTask<V> {

    private final UUID uuid;
    private boolean hasRun;

    public PacketTaskFuture(Runnable runnable, UUID uuid) {
        super(runnable, null);
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    @Override
    public synchronized void run() {
        if (hasRun) return;
        hasRun = true;

        PacketTaskHandler.registerTask(this);
        try {
            super.run();
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when running packet task.");
            ex.printStackTrace();
        }
        PacketTaskHandler.unregisterTask();
    }
}
