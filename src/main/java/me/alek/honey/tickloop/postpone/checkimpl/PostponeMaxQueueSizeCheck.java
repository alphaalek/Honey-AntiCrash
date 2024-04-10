package me.alek.honey.tickloop.postpone.checkimpl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;

public class PostponeMaxQueueSizeCheck implements PostponeCheck {

    private final Queue<PacketContainer<? extends WrappedPacket<?>>> packetsInExecutionQueue = new LinkedList<>();

    @Override
    public boolean test(PacketContainer<? extends WrappedPacket<?>> packet, HoneyPlayer player, Array<Object> pipelineData, CallbackAction<Boolean> postponed) throws PostponeTerminationException {
        if (ConfigurationOptions.Postpone.MAX_QUEUE_SIZE == -1) {
            return false;
        }

        if (!PacketType.isUnknown(packet.getType()) && !getTracker().ignoredPacketsInQueueExecutionSaver.hasID(packet.getType().getPacketId())) {
            postponed.addAction(isPostponed -> {
                if (!isPostponed) {
                    packetsInExecutionQueue.add(packet);
                }
            });
        }

        if (packetsInExecutionQueue.isEmpty()) {
            return false;
        }

        PacketContainer<? extends WrappedPacket<?>> oldestQueueExecutionPacket = packetsInExecutionQueue.peek();
        if (oldestQueueExecutionPacket == packet) {
            return false;
        }

        if (oldestQueueExecutionPacket.isHandledByServer()) {
            packetsInExecutionQueue.remove();
        }

        PacketContainer<? extends WrappedPacket<?>> executionPacket;
        while ((executionPacket = packetsInExecutionQueue.peek()) != null) {
            if (!executionPacket.isHandledByServer()) {
                break;
            }
            packetsInExecutionQueue.remove();
        }

        return packetsInExecutionQueue.size() > ConfigurationOptions.Postpone.MAX_QUEUE_SIZE;
    }
}
