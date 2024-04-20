package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketEventLoopStateTimings;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

public class PostponeRateIgnoreTerminator implements PostponeCheck {

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction<Boolean> postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.RATE_IGNORE_TERMINATOR == -1) {
            return false;
        }

        double playerRate5S = getTracker().getPlayerRate(player, Timespan.FIVE_SECOND, false);
        Bukkit.getLogger().info("Rate: " + getTracker().getPlayerRateAcceleration(player, false, true) * PacketEventLoopStateTimings.NANOS_TO_MILLIS_MULTIPLIER);
        if (playerRate5S < millisToNanos(ConfigurationOptions.Postpone.RATE_IGNORE_TERMINATOR)) {
            throw PostponeTerminationException.TERMINATOR;
        }

        return false;
    }
}
