package me.alek.honey.tracker.task;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class PacketTaskFuture<V> extends FutureTask<V> {

    private final UUID uuid;

    public PacketTaskFuture(Runnable runnable, UUID uuid) {
        super(runnable, null);
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void run() {
        PacketTaskHandler.registerTask(this);
        try {
            super.run();
        } catch (Exception ex) {
            PacketTaskHandler.unregisterTask(uuid);
            throw ex;
        }
        PacketTaskHandler.unregisterTask(uuid);
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        try {
             return super.get();
        } catch (Exception ex)  {
            return null;
        }
    }
}
