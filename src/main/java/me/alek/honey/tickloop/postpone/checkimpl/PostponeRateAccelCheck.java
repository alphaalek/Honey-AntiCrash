package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;

public class PostponeRateAccelCheck implements PostponeCheck {

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, Callback postponed) throws PostponeTerminationException {
        return ConfigurationOptions.Postpone.MAX_RATE_ACCEL != -1
                && getTracker().getPlayerRateAcceleration(player, false, true) > millisToNanos(ConfigurationOptions.Postpone.MAX_RATE_ACCEL);
    }
}
