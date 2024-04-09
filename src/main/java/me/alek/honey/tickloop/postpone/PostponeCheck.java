package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketEventloopStateTimings;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.Array;

public interface PostponeCheck {

    default ServerThreadHandleTracker getTracker() {
        return (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadTracker();
    }

    default double millisToNanos(double millis) {
        return PacketEventloopStateTimings.NANOS_TO_MILLIS_MULTIPLIER * millis;
    }

    boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException;
}
