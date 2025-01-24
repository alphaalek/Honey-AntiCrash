package me.alek.honey.tickloop.postpone.validationimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeValidation;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.AsyncTempIntervalQueue;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;

public class PostponeMaxPerXTickCheck implements PostponeValidation {

    private final AsyncTempIntervalQueue tickTracker = new AsyncTempIntervalQueue(diff -> diff >= ConfigurationOptions.Postpone.MAX_PER_X_AMOUNT, false);

    @Override
    public boolean test(PacketContainer<?> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_PER_X_TICK == -1) {
            return false;
        }

        long recentNode = getTracker().getRecentNode(player, Timespan.ONE_SECOND, true);
        int tick = getCurrentExecutionTick();
        tickTracker.add(new TimedNode<>(recentNode, -1, tick));
        tickTracker.clearExceedingNodes(tick);

        return tickTracker.getTotal() > millisToNanos(ConfigurationOptions.Postpone.MAX_PER_X_TICK);
    }
}
