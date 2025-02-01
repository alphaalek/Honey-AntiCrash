package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPacketTask {

    private final int tickAdded;
    private final PacketContainer<?> packet;
    private Runnable packetTask;
    private boolean postponed;
    private long started;
    private boolean done;

    public WrappedPacketTask(PacketContainer<?> packet, int tickAdded) {
        this.packet = packet;
        this.tickAdded = tickAdded;
    }

    public boolean runCheck() {
        return this.packet.getPlayer().isStreamActive();
    }

    public void runPre() {
        this.started = System.nanoTime();
        TaskLifecycleHandler.registerTask(this);
    }

    public void runPost() {
        this.done = true;
        this.packet.invokeServerCallback();

        TaskLifecycleHandler.unregisterTask(this.packet);
    }

    public void initPacketTask(Runnable packetTask) {
        this.packetTask = packetTask;
    }

    public HoneyPlayer getPlayer() {
        return this.packet.getPlayer();
    }

    public PacketContainer<?> getPacket() {
        return this.packet;
    }

    public Runnable getPacketTask() {
        return this.packetTask;
    }

    public long getStarted() {
        return this.started;
    }

    public boolean isPostponed() {
        return this.postponed;
    }

    public void postpone() {
        this.postponed = true;
    }

    public int getTickAdded() {
        return this.tickAdded;
    }

    public boolean isDone() {
        return this.done;
    }
}
