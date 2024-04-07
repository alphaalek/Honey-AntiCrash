package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.Array;

public interface PostponeCheck {

    default WideTempQueuedHandleTracker getTracker() {
        return HoneyPlugin.get().getServerThreadTracker();
    }

    boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException;
}
