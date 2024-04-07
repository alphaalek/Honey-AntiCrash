package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;

public class PostponeRateAccelQuotientCheck implements PostponeCheck {

    private static final double RATE_ACCEL_5S = 32.5 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
    private static final double QUOTIENT_ACCEL_5S = 12.5;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        double playerRate5S = (double) pipelineData.get(0);

        return playerRate5S > RATE_ACCEL_5S
                && getTracker().hasCollected(player, Timespan.FIVE_SECOND)
                && getTracker().getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false, true) > QUOTIENT_ACCEL_5S;
    }
}
