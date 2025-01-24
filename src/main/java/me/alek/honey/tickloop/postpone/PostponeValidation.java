package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.TimedManualHandleTracker;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;

public interface PostponeValidation {

    default TimedManualHandleTracker<Long, Double, HoneyPlayer> getTracker() {
        return HoneyPlugin.get().getExecutionQueueProxy();
    }

    default int getCurrentExecutionTick() {
        return HoneyPlugin.get().getExecutionQueueProxy().getCurrentExecutionTick();
    }

    default double millisToNanos(double millis) {
        return Timespan.MILLIS_TO_NANO_MULTIPLIER * millis;
    }

    boolean test(PacketContainer<?> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException;
}
