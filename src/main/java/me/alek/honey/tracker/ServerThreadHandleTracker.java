package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketBundleSaver;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.netty.utility.ConstructorInvoker;
import me.alek.honey.netty.utility.FieldAccessor;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.Reflection;
import me.alek.honey.tracker.task.PacketTaskHandler;
import me.alek.honey.utility.ThreadLocalAccessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ServerThreadHandleTracker extends WideTempQueuedHandleTracker {

    private final ThreadLocalAccessor<ConcurrentLinkedDeque<UUID>> packetSendQueue = new ThreadLocalAccessor<>(ConcurrentLinkedDeque::new);
    private final ThreadLocalAccessor<UUID> packetSendMarker = DuplexChannelHandler.getPacketSendMarker();
    private final Map<UUID, Integer> packetsInQueueExecution = new HashMap<>();
    private final Map<UUID, Integer> ignoredPacketsDecrementInQueueExecution = new HashMap<>();
    private final AtomicBoolean initiallyTickRound = new AtomicBoolean(true);
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
                ? NMSUtils.getMinecraftServerClass() : NMSUtils.getIAsyncTaskHandlerClass();
        Object minecraftServer = NMSUtils.getMinecraftServer();

        FieldAccessor<Queue<?>> queueAccessor = Reflection.getField(queueHolderClass, 0, Queue.class);

        PacketQueueWrapper queueWrapper = new PacketQueueWrapper((Queue<Object>) queueAccessor.get(minecraftServer));
        try {
            Reflection.setFinalField(minecraftServer, queueAccessor, queueWrapper);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private final class PacketQueueWrapper extends ConcurrentLinkedQueue<Object> {

        private final AtomicReference<UUID> recentPacketAuthor = new AtomicReference<>();
        private final ThreadLocalAccessor<ConcurrentLinkedQueue<Object>> seperatedQueue = new ThreadLocalAccessor<>(ConcurrentLinkedQueue::new);
        private final boolean havingOneMoreElement;
        private ConstructorInvoker<?> tickTaskConstructor;
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
            havingOneMoreElement = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14);

            EMPTY_TICK_TASK = havingOneMoreElement ? null
                    : (tickTaskConstructor = Reflection.getConstructor(NMSUtils.getTickTaskClass(), int.class, Runnable.class))
                    .invoke(Integer.MAX_VALUE - 3, (Runnable) () -> {});

            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_18)) {
                tickTaskConstructor = null;
            }
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

        private double getEventloopTimingsIfAny(Player player) {
            PacketContainer<? extends WrappedPacket<?>> packet = PacketTaskHandler.getLastHandledPacket();
            if (packet == null || !player.equals(packet.getPlayer())) return 0;

            return packet.getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_HANDLE);
        }

        private double getDeltaTime() {
            return ((double) (System.nanoTime() - recentPollStart)) / 1_000_000;
        }

        private double getDeltaTimeExact(Player player) {
            double eventloopTimings = getEventloopTimingsIfAny(player);

            return eventloopTimings != 0 ? eventloopTimings : getDeltaTime();
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
            Player player = null;
            // ignore packets in the first tick round, because the synchronization with the packet send queue might be wrong
            if (!initiallyTickRound.get()) {

                // check if this packet is not the first one handled in this tick
                if (recentPollStart != -1 && recentPacketAuthor.get() != null) {

                    // get the player who sent the previously polled packet from the queue
                    player = Bukkit.getPlayer(recentPacketAuthor.get());
                    recentPacketAuthor.set(null);

                    if (player != null && player.isOnline()) {
                        // evaluate the difference in time
                        double deltaTime = recentDeltaTime != -1 ? recentDeltaTime : getDeltaTimeExact(player);

                        registerTime(player, Math.max(0, deltaTime - 0.0025));
                    }
                    recentDeltaTime = -1;
                }
                recentPollStart = System.nanoTime();
            }
            Object task = null;
            Thread thread = seperatedQueue.getAnyThread((queue) -> !queue.isEmpty());

            if (thread != null) {
                recentPacketAuthor.set(packetSendQueue.get(thread).poll());
                task = seperatedQueue.get(thread).poll();
            }

            if (task != null)
                HoneyPlugin.get().getLoggerService().registerQueueExecutionPacket();

            // if the queue has exactly the same amount of elements as the delegated queue, we have to
            // make sure that the tickloop is being completed by polling one extra time
            if (!nullPoll && !havingOneMoreElement && super.peek() == null) {
                nullPoll = true;
                poll();
            }

            if (task == null) {

                // all packets in the queue has been handled, and no packets for this tick remains to be checked
                recentPollStart = -1;
                nullPoll = false;

                HoneyPlugin.get().getLoggerService().registerQueueExecutionStop();

                if (Thread.interrupted()) {
                    HoneyPlugin.get().getLoggerService().getLogger().info("Server thread was interrupted by packet. Clearing interrupted flag. "
                            + (player == null ? "" : " Maybe sent from " + player.getName() + "."));
                }
                packetsInQueueExecution.clear();

                if (initiallyTickRound.get()) initiallyTickRound.set(false);
            }

            return task == null ? null : newTickTask(recentPacketAuthor.get() == null
                    ? (Runnable) task : PacketTaskHandler.wrapTask(task, recentPacketAuthor.get()));
        }

        private Object newTickTask(Runnable runnable) {
            if (tickTaskConstructor == null) return runnable;

            return tickTaskConstructor.invoke(Integer.MAX_VALUE - 3, runnable);
        }

        @Override
        public synchronized boolean add(Object value) {
            // add it two times if it's empty, so we always have one extra element
            // this is because in versions below 1.14, it polls until it reaches null, which means one extra time
            if (super.isEmpty() && havingOneMoreElement) super.add(EMPTY_VALUE);

            // only register tracking for tasks added by netty threads (packet handling)
            if (!Thread.currentThread().getName().startsWith("Netty")) return super.add(value);

            // we will make sure that this thread is currently trying to add a packet to the server sent by a player.
            // it is not enough to just check the packet send queue, because in newer versions there will sometimes be added
            // elements to the execution queue from netty threads which are not packets
            UUID uuid = packetSendMarker.get();
            if (uuid == null) return super.add(value);

            PacketBundleSaver bundleSaver = DuplexChannelHandler.getPacketBundleSavers().get(uuid);
            // if the uuid from the packet send marker does not have a bundle saver, we will not continue doing anything.
            // we do this because this scenario is only possible if the player has leaved the server in the gap between
            // send marker register and add to queue execution
            if (bundleSaver == null) return false;

            PacketContainer<?> packet = bundleSaver.getRecent(false);
            // if it somehow tries to add multiple times, we will cancel it
            if (packet.isHandledByServer()) return true;

            packet.setHandledByServer();
            synchronized (PacketTaskHandler.lock) {
                if (!ignoredPacketsInQueueExecutionSaver.contains(packet.getType())) {
                    packetsInQueueExecution.put(uuid, packetsInQueueExecution.getOrDefault(uuid, 0) + 1);
                } else ignoredPacketsDecrementInQueueExecution.put(uuid, ignoredPacketsDecrementInQueueExecution.getOrDefault(uuid, 0) + 1);
            }
            packetSendQueue.get().add(packetSendMarker.remove());

            super.add(EMPTY_VALUE);
            return seperatedQueue.get().add(value);
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
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        packetsInQueueExecution.remove(uuid);
        ignoredPacketsDecrementInQueueExecution.remove(uuid);
        super.remove(player);
    }

    public int getPacketsInQueueExecution(UUID uuid) {
        return packetsInQueueExecution.getOrDefault(uuid, 0);
    }

    @Override
    public List<Player> getTrackersOrdered() {
        List<Player> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return Double.compare(getTimeToAverageWithoutQuotient(o1, Timespan.TEN_SECOND), getTimeToAverageWithoutQuotient(o2, Timespan.TEN_SECOND));
            }
        });
        return trackersList;
    }
}
