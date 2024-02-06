package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.netty.utility.ConstructorInvoker;
import me.alek.honey.netty.utility.FieldAccessor;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.Reflection;
import me.alek.honey.tracker.task.PacketTaskHandler;
import me.alek.honey.utility.ThreadSeperatedQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerServerThreadHandleTracker extends WideTempQueuedHandleTracker {

    private final ThreadSeperatedQueue<UUID> packetSendQueue = DuplexChannelHandler.getPacketSendQueue();
    private boolean initiallyInjected;
    private final AtomicBoolean workingProperly = new AtomicBoolean();
    private final AtomicBoolean initiallyTickRound = new AtomicBoolean(true);
    private final AtomicBoolean started = new AtomicBoolean();

    public PlayerServerThreadHandleTracker() {
        try {
            Bukkit.getServer().getScheduler().runTaskTimer(HoneyPlugin.get().getPlugin(), () -> {
                if (!initiallyInjected || (!workingProperly.get() && !Bukkit.getOnlinePlayers().isEmpty())) tryInject();

                initiallyInjected = true;
                workingProperly.set(false);
            }, 0L, 100L);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        DuplexChannelHandler.getPacketBundleSavers().clear();
    }

    private final class PacketQueueWrapper extends ConcurrentLinkedQueue<Object> {

        private final AtomicReference<UUID> recentPacketAuthor = new AtomicReference<>();
        private final ThreadSeperatedQueue<Object> seperatedQueue = new ThreadSeperatedQueue<>();
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

        private double getDeltaTime() {
            return ((double) (System.nanoTime() - recentPollStart)) / 1_000_000;
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
            started.set(true);
            workingProperly.set(true);

            Player player = null;
            // ignore packets in the first tick round, because the synchronization with the packet send queue might be wrong
            if (!initiallyTickRound.get()) {

                // check if this packet is not the first one handled in this tick
                if (recentPollStart != -1 && recentPacketAuthor.get() != null) {

                    // evaluate the difference in time
                    double deltaTime = recentDeltaTime != -1 ? recentDeltaTime : getDeltaTime();
                    recentDeltaTime = -1;

                    player = Bukkit.getPlayer(recentPacketAuthor.get());
                    recentPacketAuthor.set(null);

                    if (player != null && player.isOnline()) {
                        registerTime(player, Math.max(0, deltaTime - 0.0025));
                    }
                }
                recentPollStart = System.nanoTime();
            }
            Thread thread = seperatedQueue.getAnyQueuedThread();

            recentPacketAuthor.set(packetSendQueue.poll(thread));
            Object task = seperatedQueue.poll(thread);

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

                if (initiallyTickRound.get()) {
                    packetSendQueue.clear();
                    initiallyTickRound.set(false);
                }
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

            if (!Thread.currentThread().getName().startsWith("Netty"))
                return super.add(value);

            super.add(EMPTY_VALUE);
            return seperatedQueue.add(value);
        }
    }

    public synchronized void waitUntilInitiallyTickRoundCompletes() {
        while (initiallyTickRound.get() && started.get() && workingProperly.get()) {}
    }

    @Override
    public List<Player> getTrackersOrdered() {
        List<Player> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return Double.compare(getTimeToAverageWithoutQuotient(o1, Timespan.FIVE_SECOND), getTimeToAverageWithoutQuotient(o2, Timespan.FIVE_SECOND));
            }
        });
        return trackersList;
    }
}
