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

public class PostponeRateIgnoreTerminator implements PostponeValidation {

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.RATE_IGNORE_TERMINATOR == -1) {
            return false;
        }

        double playerRate5S = getTracker().getPlayerRate(player, Timespan.FIVE_SECOND, false);
        if (playerRate5S < millisToNanos(ConfigurationOptions.Postpone.RATE_IGNORE_TERMINATOR)) {
            throw PostponeTerminationException.TERMINATOR;
        }

        return false;
    }
}
