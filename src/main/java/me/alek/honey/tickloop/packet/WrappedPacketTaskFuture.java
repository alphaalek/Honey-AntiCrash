package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.concurrent.FutureTask;

public class WrappedPacketTaskFuture<V> extends FutureTask<V> {

    private final HoneyPlayer player;
    private final PacketContainer<? extends WrappedPacket<?>> packet;
    private boolean postponed;
    private int tickAdded;
    private final long submitted = System.nanoTime();

    public WrappedPacketTaskFuture(Runnable runnable, HoneyPlayer player, PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        super(runnable, null);
        this.player = player;
        this.packet = packet;
        this.tickAdded = tickAdded;
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

    public boolean isPostponed() {
        return postponed;
    }

    public void setPostponed() {
        postponed = true;
    }

    public int getTickAdded() {
        return tickAdded;
    }

    @Override
    public void run() {
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
