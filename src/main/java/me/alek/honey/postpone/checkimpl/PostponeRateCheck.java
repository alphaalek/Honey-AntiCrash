package me.alek.honey.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketEventloopStateTimings;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.postpone.PostponeCheck;
import me.alek.honey.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import org.bukkit.Bukkit;

public class PostponeRateCheck implements PostponeCheck {

    private static final double RATE_5S = 90 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
    private static final double RATE_10S = 75 * Timespan.MILLIS_TO_NANO_MULTIPLIER;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        double playerRate5S = getTracker().getPlayerRate(player, Timespan.FIVE_SECOND, true);

        Bukkit.getLogger().info("Rate " + playerRate5S * PacketEventloopStateTimings.NANOS_TO_MILLIS_MULTIPLIER);
        if (playerRate5S > RATE_5S
                || getTracker().getPlayerRate(player, Timespan.TEN_SECOND, true) > RATE_10S) {
            return true;
        }

        return false;
    }
}
