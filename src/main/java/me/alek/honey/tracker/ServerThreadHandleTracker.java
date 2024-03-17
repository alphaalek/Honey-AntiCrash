package me.alek.honey.tracker;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.DuplexChannelHandler;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.*;
import me.alek.honey.tracker.packet.PacketTaskFuture;
import me.alek.honey.tracker.packet.PacketTaskHandler;
import me.alek.honey.tracker.temp.TempTimedQueuedTracker;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.tracker.timed.Timespan;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ServerThreadHandleTracker extends WideTempQueuedHandleTracker {

    private final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = DuplexChannelHandler.getPacketSendMarker();
    private final Map<UUID, Integer> packetsInQueueExecution = new HashMap<>();
    private final Map<UUID, Integer> ignoredPacketsDecrementInQueueExecution = new HashMap<>();
    public Set<PacketTypeEnum> ignoredPacketsInQueueExecutionSaver;

    public ServerThreadHandleTracker() {
        reload();
        tryInject();
    }

    public void reload() {
        Set<PacketTypeEnum> packets = new HashSet<>();
        for (String ignoredPacket : ConfigurationOptions.Netty.NettyA.IGNORED_PACKETS) {
            packets.add(PacketWrapperInvokerFactory.getPacketTypeForName(ignoredPacket));
        }
        ignoredPacketsInQueueExecutionSaver = packets;
    }

    private void tryInject() {
        Class<?> queueHolderClass = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14)
                ? NMS.getMinecraftServerClass() : NMS.getIAsyncTaskHandlerClass();
        Object minecraftServer = NMS.getMinecraftServer();

        FieldAccessor<Queue<?>> queueAccessor = Reflection.getField(queueHolderClass, 0, Queue.class);

        PacketQueueWrapper queueWrapper = new PacketQueueWrapper((Queue<Object>) queueAccessor.get(minecraftServer));
        try {
            Reflection.setFinalField(minecraftServer, queueAccessor, queueWrapper);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public TempTimedQueuedTracker getPropertyTracker(HoneyPlayer player) {
        return player.serverThreadTimedQueuedTracker;
    }

    public void setPropertyTracker(HoneyPlayer player, TempTimedQueuedTracker tracker) {
        player.serverThreadTimedQueuedTracker = tracker;
    }

    private final class PacketQueueWrapper extends ConcurrentLinkedQueue<Object> {

        private final AtomicReference<HoneyPlayer> recentPacketAuthor = new AtomicReference<>();
        private final NettyThreadLocal<ConcurrentLinkedQueue<PacketTaskFuture<Object>>> nettySubmitQueue = new NettyThreadLocal<>(ConcurrentLinkedQueue::new);
        private ConstructorInvoker<?> tickTaskConstructor;
        private final boolean pollsOneExtra;
        private long recentPollStart = -1;
        private double recentDeltaTime = -1;
        private boolean nullPoll;
        private final Object EMPTY_VALUE = new Object();
        private final Object EMPTY_TICK_TASK;

        public PacketQueueWrapper(final Queue<Object> delegate) {
            // convert all elements from the previous queue to the current one, if there are any
            while (!delegate.isEmpty()) {
                add(delegate.poll());
            }
            EMPTY_TICK_TASK = (pollsOneExtra = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14)) ? null
                    : (tickTaskConstructor = Reflection.getConstructor(NMS.getTickTaskClass(), int.class, Runnable.class))
                    .invoke(Integer.MAX_VALUE - 3, (Runnable) () -> {});

            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_18)) {
                tickTaskConstructor = null;
            }
        }

        private double getEventloopTimingsIfAny(HoneyPlayer player) {
            PacketContainer<? extends WrappedPacket<?>> packet = PacketTaskHandler.getLastHandledPacket();
            if (packet == null || !player.equals(packet.getPlayer())) return 0;

            return packet.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_HANDLE);
        }

        private double getDeltaTime() {
            return System.nanoTime() - recentPollStart;
        }

        private double getDeltaTimeExact(HoneyPlayer player) {
            double eventloopTimings = getEventloopTimingsIfAny(player);

            return eventloopTimings != 0 ? eventloopTimings : getDeltaTime();
        }

        private Object newTickTask(Runnable runnable) {
            if (tickTaskConstructor == null) return runnable;

            return tickTaskConstructor.invoke(Integer.MAX_VALUE - 3, runnable);
        }

        @Override
        public Object peek() {
            // if we have any packets ready to be run, we will return an empty value to mark we're ready
            // if not, we will just return null and tell the server that the delegated queue is empty
            if (super.peek() == null) return null;

            // this runnable is not run by the server, just used to check if the task can be executed
            // so we just return a runnable with an empty body and instead do the tracking work in methods used afterward
            return EMPTY_TICK_TASK;
        }

        @Override
        public Object remove() {
            Object poll;
            if ((poll = poll()) != null) return poll;

            return EMPTY_TICK_TASK;
        }

        @Override
        public synchronized Object poll() {
            Object otherTask;
            if ((otherTask = super.poll()) != null && otherTask != EMPTY_VALUE) {
                if (recentPollStart != -1)
                    recentDeltaTime = getDeltaTime();
                recentPollStart = -1;
                return otherTask;
            }
            // check if this packet is not the first one handled in this tick
            if (recentPollStart != -1 && recentPacketAuthor.get() != null) {

                // get the player who sent the previously polled packet from the queue
                HoneyPlayer player = recentPacketAuthor.get();
                recentPacketAuthor.set(null);

                if (player != null && player.player.isOnline()) {
                    // evaluate the difference in time
                    double deltaTime = recentDeltaTime != -1 ? recentDeltaTime : getDeltaTimeExact(player);

                    registerTime(player, deltaTime);
                }
                recentDeltaTime = -1;
            }
            recentPollStart = System.nanoTime();

            PacketTaskFuture<Object> task = null;
            int thread = nettySubmitQueue.getAnyThread((queue) -> !queue.isEmpty());

            if (thread != -1) {
                task = nettySubmitQueue.get(thread).poll();
                recentPacketAuthor.set(Optional.ofNullable(task).map(PacketTaskFuture::getPlayer).orElse(null));
            }

            if (task != null)
                HoneyPlugin.get().getLoggerService().registerQueueExecutionPacket();

            Object tickTask = task == null ? null : newTickTask(task);

            // if the queue has exactly the same amount of elements as the delegated queue, we have to
            // make sure that the tickloop is being completed by polling one extra time
            if (!nullPoll && !pollsOneExtra && super.peek() == null) {
                nullPoll = true;
                poll();
            }

            if (task == null) {
                packetsInQueueExecution.clear();
                nettySubmitQueue.forEach(ConcurrentLinkedQueue::clear);

                // all packets in the queue has been handled, and no packets for this tick remains to be checked
                recentPollStart = -1;
                nullPoll = false;

                HoneyPlugin.get().getLoggerService().registerQueueExecutionStop();

                if (Thread.interrupted()) {
                    HoneyPlugin.get().getLoggerService().getLogger().info("Server thread was interrupted by packet. Clearing interrupted flag.");
                }
            }
            return tickTask;
        }

        @Override
        public synchronized boolean add(Object value) {
            // why add something to an execution queue, without it being a runnable?
            if (!(value instanceof Runnable)) return false;

            // add it two times if it's empty, so we always have one extra element
            // this is because in versions below 1.14, it polls until it reaches null, which means one extra time
            if (super.isEmpty() && pollsOneExtra) super.add(EMPTY_VALUE);

            // only register tracking for tasks added by netty threads (packet handling)
            if (!Thread.currentThread().getName().startsWith("Netty")) return super.add(value);

            // we will make sure that this thread is currently trying to add a packet to the server sent by a player.
            // it is not enough to just check the packet send queue, because in newer versions there will sometimes be added
            // elements to the execution queue from netty threads which are not packets
            PacketContainer<? extends WrappedPacket<?>> packet = packetSendMarker.get();
            if (packet == null) return super.add(value);

            packet.setHandledByServer();
            UUID uuid = packet.getPlayer().uuid;
            synchronized (PacketTaskHandler.lock) {
                if (!ignoredPacketsInQueueExecutionSaver.contains(packet.getType())) {
                    packetsInQueueExecution.put(uuid, packetsInQueueExecution.getOrDefault(uuid, 0) + 1);
                } else ignoredPacketsDecrementInQueueExecution.put(uuid, ignoredPacketsDecrementInQueueExecution.getOrDefault(uuid, 0) + 1);
            }
            super.add(EMPTY_VALUE);

            if (packet.getPlayer() == null || packet.getPlayer().nettyThreadID == -1)
                return super.add(value);

            return nettySubmitQueue.get(packet.getPlayer().nettyThreadID)
                    .add(new PacketTaskFuture<>((Runnable) value, packet.getPlayer(), packet));
        }
    }

    public void decrementPacketsInQueueExecution(UUID uuid) {
        synchronized (PacketTaskHandler.lock) {
            int ignoredDecrement = ignoredPacketsDecrementInQueueExecution.getOrDefault(uuid, 0);
            if (ignoredDecrement > 0)
                ignoredPacketsDecrementInQueueExecution.put(uuid, ignoredDecrement - 1);

            packetsInQueueExecution.put(uuid, packetsInQueueExecution.getOrDefault(uuid, 0) - (ignoredDecrement == 0 ? 1 : 0));
        }
    }

    @Override
    public void remove(HoneyPlayer player) {
        UUID uuid = player.uuid;
        packetsInQueueExecution.remove(uuid);
        ignoredPacketsDecrementInQueueExecution.remove(uuid);
        super.remove(player);
    }

    public int getPacketsInQueueExecution(UUID uuid) {
        return packetsInQueueExecution.getOrDefault(uuid, 0);
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        List<HoneyPlayer> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<HoneyPlayer>() {

            @Override
            public int compare(HoneyPlayer o1, HoneyPlayer o2) {
                return Double.compare(getTimeToAverageWithoutQuotient(o1, Timespan.TEN_SECOND, true), getTimeToAverageWithoutQuotient(o2, Timespan.TEN_SECOND, true));
            }
        });
        return trackersList;
    }
}
