package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.ExecutionQueueProxy;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

public interface PostponeCheck {

    default ExecutionQueueProxy getTracker() {
        return HoneyPlugin.get().getExecutionQueueProxy();
    }

    default double millisToNanos(double millis) {
        return Timespan.MILLIS_TO_NANO_MULTIPLIER * millis;
    }

    boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction postponed) throws PostponeTerminationException;
}
