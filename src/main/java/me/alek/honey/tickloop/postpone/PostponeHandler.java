package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.tickloop.postpone.checkimpl.*;
import me.alek.honey.tickloop.ExecutionQueueWrapper;
import me.alek.honey.tickloop.packet.WrappedPacketTaskFuture;

import java.util.LinkedList;
import java.util.Queue;

public class PostponeHandler {

    private static final Queue<PostponeHandler> postponedHandlers = new LinkedList<>();
    private static final ExecutionQueueWrapper tickloopAccessor = (ExecutionQueueWrapper) HoneyPlugin.get().getServerThreadTracker();

    public static void retryPostponedPackets(Queue<Object> queue) {
        synchronized (postponedHandlers) {
            PostponeHandler handler;
            while ((handler = postponedHandlers.poll()) != null) {
                WrappedPacketTaskFuture<?> packetTask = handler.postponedPackets.poll();
                if (packetTask == null) {
                    continue;
                }

                queue.add(packetTask);
                packetTask.getPlayer().tickloopHandler.getPostponeHandler().postponing = false;
            }
        }
    }

    private final Queue<WrappedPacketTaskFuture<?>> postponedPackets = new LinkedList<>();
    private boolean postponing;
    private final Queue<PostponeCheck> postponeChecks = new LinkedList<>();

    public PostponeHandler() {
        addPostponeCheck(new PostponeMaxQueueSizeCheck());

        addPostponeCheck(new PostponeRateIgnoreTerminator()); /* terminator */
        addPostponeCheck(new PostponeRateCheck());
        addPostponeCheck(new PostponeMaxPerTickCheck());
        addPostponeCheck(new PostponeRateAccelCheck());
        addPostponeCheck(new PostponeRateAccelQuotientCheck());
    }

    public void addPostponeCheck(PostponeCheck check) {
        postponeChecks.add(check);
    }

    public Queue<PostponeCheck> getPostponeChecks() {
        return postponeChecks;
    }

    public boolean isPostponing() {
        return postponing;
    }

    public void setPostponing() {
        this.postponing = true;
    }

    public void addPostponedPacket(WrappedPacketTaskFuture<?> packetTask) {
        synchronized (postponedHandlers) {
            postponedHandlers.add(this);
            postponedPackets.add(packetTask);

            if (postponedPackets.size() > ConfigurationOptions.Postpone.MAX_POSTPONED_PACKETS) {
                postponedPackets.remove();
            }

            if (postponedPackets.size() != 1) {
                WrappedPacketTaskFuture<?> oldestPacketTask = postponedPackets.peek();
                if (oldestPacketTask == null) {
                    return;
                }

                int currentTick = tickloopAccessor.getCurrentTick();
                if (currentTick - oldestPacketTask.getTickAdded() > ConfigurationOptions.Postpone.MAX_POSTPONED_TICKS) {
                    postponedPackets.remove();
                }

                if (postponedPackets.size() == 1) {
                    return;
                }

                postponedPackets.removeIf(packet -> currentTick - packet.getTickAdded() > ConfigurationOptions.Postpone.MAX_POSTPONED_TICKS);
            }
        }
    }

    public int getPostponedPacketsSize() {
        return postponedPackets.size();
    }
}
