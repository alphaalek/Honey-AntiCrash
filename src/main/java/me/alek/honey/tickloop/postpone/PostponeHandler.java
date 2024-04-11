package me.alek.honey.tickloop.postpone;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.tickloop.ExecutionQueueWrapper;
import me.alek.honey.tickloop.packet.WrappedPacketTaskFuture;
import me.alek.honey.tickloop.postpone.checkimpl.*;

import java.util.LinkedList;
import java.util.Queue;

public class PostponeHandler {

    private static final Queue<PostponeHandler> currentlyPostponedHandlers = new LinkedList<>();
    private static final Queue<PostponeHandler> postponedHandlersOrder = new LinkedList<>();
    private static final Queue<PostponeHandler> nextPostponedHandlersOrder = new LinkedList<>();
    private static final ExecutionQueueWrapper tickLoopAccessor = HoneyPlugin.get().getServerThreadTracker();

    public static void retryPostponedPackets(Queue<Object> queue) {
        synchronized (postponedHandlersOrder) {
            PostponeHandler handler;
            while ((handler = postponedHandlersOrder.poll()) != null) {
                if (handler.postponingRegistered) {
                    handler.retry(queue);
                }
            }

            synchronized (currentlyPostponedHandlers) {
                for (PostponeHandler postponedHandler : currentlyPostponedHandlers) {
                    postponedHandler.retryAmount = 0;
                }
            }

            PostponeHandler nextHandler;
            while ((nextHandler = nextPostponedHandlersOrder.poll()) != null) {
                postponedHandlersOrder.add(nextHandler);
            }
        }
    }

    private final Queue<WrappedPacketTaskFuture<?>> postponedPackets = new LinkedList<>();
    private final Queue<PostponeCheck> postponeChecks = new LinkedList<>();
    private boolean postponing;
    private int retryAmount;
    private boolean postponingRegistered;

    public PostponeHandler() {
        addPostponeCheck(new PostponeRateIgnoreTerminator()); /* terminator */
        addPostponeCheck(new PostponeRateCheck());
        addPostponeCheck(new PostponeRateAccelCheck());
        addPostponeCheck(new PostponeRateAccelQuotientCheck());
        addPostponeCheck(new PostponeMaxPerXTickCheck());
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

    public void retry(Queue<Object> queue) {
        if (++retryAmount > ConfigurationOptions.Postpone.MAX_RETRY_AMOUNT) {
            nextPostponedHandlersOrder.add(this);
            return;
        }

        WrappedPacketTaskFuture<?> packetTask = postponedPackets.poll();
        if (packetTask == null) {
            currentlyPostponedHandlers.remove(this);
            return;
        }

        if (packetTask.getPlayer().punished || !packetTask.getPlayer().isOnlineWeak()) {
            currentlyPostponedHandlers.remove(this);
            return;
        }

        queue.add(packetTask);
        postponing = false;

        if (postponedPackets.isEmpty()) {
            postponingRegistered = false;
            synchronized (currentlyPostponedHandlers) {
                currentlyPostponedHandlers.remove(this);
            }
        }

        HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(packetTask.getPlayer(), false, false);
    }

    public void addPostponedPacket(WrappedPacketTaskFuture<?> packetTask) {
        if (!postponingRegistered) {
            postponingRegistered = true;
            synchronized (currentlyPostponedHandlers) {
                currentlyPostponedHandlers.add(this);
            }
        }

        synchronized (postponedHandlersOrder) {
            postponedHandlersOrder.add(this);
            postponedPackets.add(packetTask);

            if (postponedPackets.size() > ConfigurationOptions.Postpone.MAX_POSTPONED_PACKETS) {
                postponedPackets.remove();
            }

            if (postponedPackets.size() != 1) {
                WrappedPacketTaskFuture<?> oldestPostponedPacket = postponedPackets.peek();
                if (oldestPostponedPacket == null) {
                    return;
                }

                int currentTick = tickLoopAccessor.getCurrentExecutionTick();
                if (currentTick - oldestPostponedPacket.getTickAdded() <= ConfigurationOptions.Postpone.MAX_POSTPONED_TICKS) {
                    return;
                }

                HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(packetTask.getPlayer(), true, true);
                postponedPackets.remove();
                debugPacketCancelled(oldestPostponedPacket);

                if (postponedPackets.isEmpty()) {
                    return;
                }

                WrappedPacketTaskFuture<?> postponedPacket;
                while ((postponedPacket = postponedPackets.peek()) != null) {
                    if (currentTick - postponedPacket.getTickAdded() <= ConfigurationOptions.Postpone.MAX_POSTPONED_TICKS) {
                        break;
                    }
                    HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(packetTask.getPlayer(), true, true);
                    debugPacketCancelled(postponedPacket);

                    postponedPackets.remove();
                }
            }
        }
    }

    private void debugPacketCancelled(WrappedPacketTaskFuture<?> postponedPacket) {
        if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
            HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled due to postpone overload: " + postponedPacket.getPacket().getType() + ", " + postponedPacket.getPlayer().packetHandler.getProtocolState() + "," + postponedPacket.getPacket().getType().getBound() + ": " + postponedPacket.getPlayer().name);
        }
    }

    public void unregister() {
        postponingRegistered = false;
        synchronized (currentlyPostponedHandlers) {
            currentlyPostponedHandlers.remove(this);
        }
    }

    public int getPostponedPacketsSize() {
        return postponedPackets.size();
    }
}
