package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WrappedPacketTaskFuture extends FutureTask<Object> {

    private final PacketContainer<? extends WrappedPacket<?>> packet;
    private final int tickAdded;
    private boolean postponed;
    private long started;
    private final long playerPacketIndex;

    public WrappedPacketTaskFuture(Runnable runnable, PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded, long playerPacketIndex) {
        super(runnable, null);
        this.packet = packet;
        this.tickAdded = tickAdded;
        this.playerPacketIndex = playerPacketIndex;
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

    public long getPlayerPacketIndex() {
        return playerPacketIndex;
    }

    @Override
    public void run() {
        if (getPlayer() != null && (!getPlayer().isOnlineWeak() || getPlayer().punished)) {
            super.set(null);
            return;
        }

        started = System.nanoTime();
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

    @Override
    public String toString() {
        return "Task{" + packet.getPlayerName() + "," + playerPacketIndex + "," + tickAdded + "}";
    }
}
