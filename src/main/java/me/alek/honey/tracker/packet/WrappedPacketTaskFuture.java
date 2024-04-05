package me.alek.honey.tracker.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.concurrent.FutureTask;

public class WrappedPacketTaskFuture<V> extends FutureTask<V> {

    private final HoneyPlayer player;
    private final PacketContainer<? extends WrappedPacket<?>> packet;
    private final long submitted = System.nanoTime();

    public WrappedPacketTaskFuture(Runnable runnable, HoneyPlayer player, PacketContainer<? extends WrappedPacket<?>> packet) {
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

    public long getSubmitted() {
        return submitted;
    }

    @Override
    public synchronized void run() {
        if (!player.isOnlineWeak() || player.punished) {
            super.set(null);
            return;
        }

        PacketTaskHandler.registerTask(this);
        try {
            super.run();
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when running packet tick task.");
            ex.printStackTrace();
        }
        packet.invokeServerCallback();

        PacketTaskHandler.unregisterTask(packet);
    }
}
