package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.TempQueue;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

public class PostponeMaxPerXTickCheck implements PostponeCheck {

    private final TempQueue tickTracker = new TempQueue(diff -> diff >= ConfigurationOptions.Postpone.MAX_PER_X_AMOUNT, false);

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction<Boolean> postponed) throws PostponeTerminationException {
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
