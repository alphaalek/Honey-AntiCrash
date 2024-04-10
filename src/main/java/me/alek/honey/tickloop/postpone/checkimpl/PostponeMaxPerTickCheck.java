package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

public class PostponeMaxPerTickCheck implements PostponeCheck {

    private double tickTotal;
    private int previousTickLoop;
    private double previousRate;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction<Boolean> postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_PER_TICK == -1) {
            return false;
        }

        double playerRate5S = (double) pipelineData.get(0);
        double diff = playerRate5S - previousRate;
        previousRate = playerRate5S;

        if (previousTickLoop != (previousTickLoop = getTracker().getCurrentTick())) {
            tickTotal = 0;
        }
        tickTotal += diff;

        return tickTotal > millisToNanos(ConfigurationOptions.Postpone.MAX_PER_TICK);
    }
}
