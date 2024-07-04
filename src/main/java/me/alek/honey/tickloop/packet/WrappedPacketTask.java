package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.concurrent.FutureTask;
import java.util.logging.Level;

public class WrappedPacketTask extends FutureTask<Object> {

    private final PacketContainer<? extends WrappedPacket<?>> packet;
    private final int tickAdded;
    private boolean postponed;
    private long started;
    private boolean done;

    public WrappedPacketTask(Runnable runnable, PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        super(runnable, null);
        this.packet = packet;
        this.tickAdded = tickAdded;
    }

    public HoneyPlayer getPlayer() {
        return packet.getPlayer();
    }

    public PacketContainer<? extends WrappedPacket<?>> getPacket() {
        return packet;
    }

    public long getStarted() {
        return started;
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

    public boolean isDone() {
        return done;
    }

    @Override
    public void run() {
        if (!getPlayer().isStreamActive()) {
            super.set(null);
            return;
        }

        started = System.nanoTime();
        PacketTaskHandler.registerTask(this);
        try {
            super.run();
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().log(Level.SEVERE, "Error occurred when processing packet task.", ex);
        }
        done = true;
        packet.invokeServerCallback();

        PacketTaskHandler.unregisterTask(packet);
    }
}
