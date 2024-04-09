package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.util.Array;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class PostponeMaxQueueSizeCheck implements PostponeCheck {

    private final Queue<PacketContainer<? extends WrappedPacket<?>>> packetsInExecutionQueue = new LinkedList<>();

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData) throws PostponeTerminationException {
        if (!getTracker().ignoredPacketsInQueueExecutionSaver.hasID(packet.getType().getPacketId())) {
            packetsInExecutionQueue.add(packet);
        }

        if (packetsInExecutionQueue.isEmpty()) {
            return false;
        }

        synchronized (this) {
            PacketContainer<? extends WrappedPacket<?>> oldestQueueExecutionPacket = packetsInExecutionQueue.peek();
            if (oldestQueueExecutionPacket == null || oldestQueueExecutionPacket == packet) {
                return false;
            }

            if (oldestQueueExecutionPacket.isHandledByServer()) {
                packetsInExecutionQueue.remove();
            }

            packetsInExecutionQueue.removeIf(PacketContainer::isHandledByServer);

            return packetsInExecutionQueue.size() > ConfigurationOptions.Postpone.MAX_QUEUE_SIZE;
        }
    }
}
