package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.util.Array;

public class PostponeMaxPerTickCheck implements PostponeCheck {

    private static final int MAX_TICK_TIME = 50;

    private double tickTotal;
    private int previousTickLoop;
    private double previousRate;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        double playerRate5S = (double) pipelineData.get(0);
        double diff = playerRate5S - previousRate;
        previousRate = playerRate5S;

        if (previousTickLoop != (previousTickLoop = getTracker().getCurrentTickLoop())) {
            tickTotal = 0;
        }

        tickTotal += diff;

        return tickTotal > MAX_TICK_TIME;
    }
}
