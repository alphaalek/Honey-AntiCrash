package me.alek.honey.tickloop.postpone.validationimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeValidation;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;

public class PostponeRateAccelQuotientCheck implements PostponeValidation {

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_RATE_ACCEL_QUOTIENT == -1) {
            return false;
        }

        double playerRate5S = (double) pipelineData.get(0);
        return playerRate5S > millisToNanos(ConfigurationOptions.Postpone.MIN_RATE_5S_FOR_QUOTIENT)
                && getTracker().hasCollected(player, Timespan.FIVE_SECOND)
                && getTracker().getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false) > millisToNanos(ConfigurationOptions.Postpone.MAX_RATE_ACCEL_QUOTIENT);
    }
}
