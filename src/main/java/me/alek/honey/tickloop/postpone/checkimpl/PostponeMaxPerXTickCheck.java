package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.UnsyncTempQueue;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;

public class PostponeMaxPerXTickCheck implements PostponeCheck {

    private final UnsyncTempQueue tickTracker = new UnsyncTempQueue(diff -> diff >= ConfigurationOptions.Postpone.MAX_PER_X_AMOUNT, false);

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_PER_X_TICK == -1) {
            return false;
        }

        double recentNode = getTracker().getRecentNode(player, Timespan.ONE_SECOND, true);
        int tick = getTracker().getCurrentExecutionTick();
        tickTracker.add(new TimedNode<>(recentNode, -1, getTracker().getCurrentExecutionTick()));
        tickTracker.clearExceedingNodes(tick);

        return tickTracker.getTotal() > millisToNanos(ConfigurationOptions.Postpone.MAX_PER_X_TICK);
    }
}
