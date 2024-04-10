package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.ExecutionQueueWrapper;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

public interface PostponeCheck {

    default ExecutionQueueWrapper getTracker() {
        return HoneyPlugin.get().getServerThreadTracker();
    }

    default double millisToNanos(double millis) {
        return Timespan.MILLIS_TO_NANO_MULTIPLIER * millis;
    }

    boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction<Boolean> postponed) throws PostponeTerminationException;
}
