package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;

public class PostponeRateAccelCheck implements PostponeCheck {

    private static final double RATE_ACCEL = /*115 */ 150 * Timespan.MILLIS_TO_NANO_MULTIPLIER;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        return getTracker().getPlayerRateAcceleration(player, false, true) > RATE_ACCEL;
    }
}
