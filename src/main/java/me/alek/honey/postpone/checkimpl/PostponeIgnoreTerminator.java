package me.alek.honey.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.postpone.PostponeCheck;
import me.alek.honey.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;

public class PostponeIgnoreTerminator implements PostponeCheck {

    private static final double IGNORE_RATE_5S = 10 * Timespan.MILLIS_TO_NANO_MULTIPLIER;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        double playerRate5S = getTracker().getPlayerRate(player, Timespan.FIVE_SECOND, false);

        if (playerRate5S < IGNORE_RATE_5S) {
            throw new PostponeTerminationException();
        }

        return false;
    }
}
