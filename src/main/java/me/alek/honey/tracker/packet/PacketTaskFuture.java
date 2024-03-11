package me.alek.honey.tracker.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.concurrent.FutureTask;

public class PacketTaskFuture<V> extends FutureTask<V> {

    private final HoneyPlayer player;
    private final PacketContainer<? extends WrappedPacket<?>> packet;

    public PacketTaskFuture(Runnable runnable, HoneyPlayer player, PacketContainer<? extends WrappedPacket<?>> packet) {
        super(runnable, null);
        this.player = player;
        this.packet = packet;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public PacketContainer<? extends WrappedPacket<?>> getPacket() {
        return packet;
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
