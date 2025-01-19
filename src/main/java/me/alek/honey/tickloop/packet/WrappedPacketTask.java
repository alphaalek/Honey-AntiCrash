package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPacketTask {

    private final int tickAdded;
    private final PacketContainer<? extends WrappedPacket<?>> packet;
    private Runnable packetTask;
    private boolean postponed;
    private long started;
    private boolean done;

    public WrappedPacketTask(PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        this.packet = packet;
        this.tickAdded = tickAdded;
    }

    public boolean runCheck() {
        return packet.getPlayer().isStreamActive();
    }

    public void runPre() {
        this.started = System.nanoTime();
        TaskLifecycleHandler.registerTask(this);
    }

    public void runPost() {
        this.done = true;
        packet.invokeServerCallback();

        TaskLifecycleHandler.unregisterTask(packet);
    }

    public void initPacketTask(Runnable packetTask) {
        this.packetTask = packetTask;
    }

    public HoneyPlayer getPlayer() {
        return packet.getPlayer();
    }

    public PacketContainer<? extends WrappedPacket<?>> getPacket() {
        return packet;
    }

    public Runnable getPacketTask() {
        return packetTask;
    }

    public long getStarted() {
        return started;
    }

    public boolean isPostponed() {
        return postponed;
    }

    public void postpone() {
        postponed = true;
    }

    public int getTickAdded() {
        return tickAdded;
    }

    public boolean isDone() {
        return done;
    }
}
