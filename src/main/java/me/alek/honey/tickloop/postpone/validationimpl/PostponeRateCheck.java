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

public class PostponeRateCheck implements PostponeValidation {

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_RATE_5S != -1
                && (double) pipelineData.addAndGet(getTracker().getPlayerRate(player, Timespan.FIVE_SECOND, true)) > millisToNanos(ConfigurationOptions.Postpone.MAX_RATE_5S)) {
            return true;
        }

        if (ConfigurationOptions.Postpone.MAX_RATE_10S != -1
                && getTracker().getPlayerRate(player, Timespan.TEN_SECOND, true) > millisToNanos(ConfigurationOptions.Postpone.MAX_RATE_10S)) {
            return true;
        }

        return false;
    }
}
