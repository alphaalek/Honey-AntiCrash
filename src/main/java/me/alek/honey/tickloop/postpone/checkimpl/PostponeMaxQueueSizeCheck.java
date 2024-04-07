package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import me.alek.honey.util.Array;

public class PostponeMaxQueueSizeCheck implements PostponeCheck {

    private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadTracker();
    private static final int MAX_PACKETS = 12;

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        return tracker.getPacketsInQueueExecution(player.uuid) > MAX_PACKETS
                && !tracker.ignoredPacketsInQueueExecutionSaver.contains(packet.getType());
    }
}
