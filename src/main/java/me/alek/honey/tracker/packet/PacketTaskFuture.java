package me.alek.honey.tracker.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;

import java.util.concurrent.FutureTask;

public class PacketTaskFuture<V> extends FutureTask<V> {

    private final HoneyPlayer player;

    public PacketTaskFuture(Runnable runnable, HoneyPlayer player) {
        super(runnable, null);
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    @Override
    public synchronized void run() {
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
