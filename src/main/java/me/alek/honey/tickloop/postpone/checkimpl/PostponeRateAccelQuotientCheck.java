package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;

public class PostponeRateAccelQuotientCheck implements PostponeCheck {

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_RATE_ACCEL_QUOTIENT == -1) {
            return false;
        }

        double playerRate5S = (double) pipelineData.get(0);
        return playerRate5S > millisToNanos(ConfigurationOptions.Postpone.MIN_RATE_5S_FOR_QUOTIENT)
                && getTracker().hasCollected(player, Timespan.FIVE_SECOND)
                && getTracker().getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false, false) > millisToNanos(ConfigurationOptions.Postpone.MAX_RATE_ACCEL_QUOTIENT);
    }
}
