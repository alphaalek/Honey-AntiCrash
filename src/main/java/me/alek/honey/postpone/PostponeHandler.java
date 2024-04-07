package me.alek.honey.postpone;

import me.alek.honey.postpone.checkimpl.PostponeIgnoreTerminator;
import me.alek.honey.postpone.checkimpl.PostponeRateCheck;
import me.alek.honey.tracker.packet.WrappedPacketTaskFuture;

import java.util.LinkedList;
import java.util.Queue;

public class PostponeHandler {

    private static final int MAX_POSTPONED_PACKETS = 20;
    private static final Queue<PostponeHandler> postponedHandlers = new LinkedList<>();

    public static void retryPostponedPackets(Queue<Object> queue) {
        synchronized (postponedHandlers) {
            PostponeHandler handler;
            while ((handler = postponedHandlers.poll()) != null) {
                WrappedPacketTaskFuture<?> packetTask = handler.postponedPackets.poll();
                if (packetTask == null) {
                    continue;
                }

                queue.add(packetTask);
                packetTask.getPlayer().postponeHandler.postponing = false;
            }
        }
    }

    private final Queue<WrappedPacketTaskFuture<?>> postponedPackets = new LinkedList<>();
    private boolean postponing;
    private final Queue<PostponeCheck> postponeChecks = new LinkedList<>();

    public PostponeHandler() {
        addPostponeCheck(new PostponeIgnoreTerminator());
        addPostponeCheck(new PostponeRateCheck());
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

            if (postponedPackets.size() > MAX_POSTPONED_PACKETS) {
                postponedPackets.remove();
            }
        }
    }
}
