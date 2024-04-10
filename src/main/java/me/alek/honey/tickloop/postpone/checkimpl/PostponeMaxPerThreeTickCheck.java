package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketEventloopStateTimings;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

public class PostponeMaxPerThreeTickCheck implements PostponeCheck {

    private double tickTotal;
    private int previousTickUpdate;
    private double previousRate;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction<Boolean> postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_PER_TICK == -1) {
            return false;
        }

        double playerRate1S = getTracker().getPlayerTotal(player, Timespan.ONE_SECOND, false);
        double diff = playerRate1S - previousRate;
        previousRate = playerRate1S;

        if (getTracker().getCurrentTick() >= previousTickUpdate + 3) {
            previousTickUpdate = getTracker().getCurrentTick();
            tickTotal = 0;
        }
        tickTotal += Math.max(0, diff);

        Bukkit.getLogger().info("Tick total " + PacketEventloopStateTimings.NANOS_TO_MILLIS_MULTIPLIER * tickTotal);
        return tickTotal > millisToNanos(ConfigurationOptions.Postpone.MAX_PER_TICK);
    }
}
