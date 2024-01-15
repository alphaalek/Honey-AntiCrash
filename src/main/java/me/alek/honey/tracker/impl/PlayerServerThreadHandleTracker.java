package me.alek.honey.tracker.impl;

import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.tracker.AbstractOldAndCurrentHandleTracker;
import me.alek.honey.tracker.task.PacketTaskHandler;
import me.alek.honey.utility.ThreadSeperatedQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerServerThreadHandleTracker extends AbstractOldAndCurrentHandleTracker {

    private final ThreadSeperatedQueue<UUID> packetSendQueue = DuplexChannelHandler.getPacketSendQueue();
    private boolean workingProperly;
    private boolean initiallyInjected;

    public PlayerServerThreadHandleTracker() {
        super(100, 5, 10, 30, 60);
        update();

        try {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            executorService.schedule(() -> {
                if (!initiallyInjected || (!workingProperly && !Bukkit.getOnlinePlayers().isEmpty())) tryInject();

                initiallyInjected = true;
                workingProperly = false;
            }, 5L, TimeUnit.SECONDS);
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    private void tryInject() {
        Class<?> minecraftServerClass = NMSUtils.getMinecraftServerClass();
        Object minecraftServer = NMSUtils.getMinecraftServer();

        FieldAccessor<Queue<?>> queueAccessor = Reflection.getField(minecraftServerClass, 0, Queue.class);

        PacketQueueWrapper queueWrapper = new PacketQueueWrapper((Queue<Object>) queueAccessor.get(minecraftServer));

        try {
            Reflection.setFinalField(minecraftServer, queueAccessor, queueWrapper);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private final class PacketQueueWrapper extends ConcurrentLinkedQueue<Object> {

        private final AtomicReference<UUID> recentPacketAuthor = new AtomicReference<>();
        private final ThreadSeperatedQueue<Object> seperatedQueue = new ThreadSeperatedQueue<>();
        private long recentPollStart = -1;
        private boolean startup;
        private boolean initiallyTickRound;

        private final Object EMPTY_VALUE = new Object();

        public PacketQueueWrapper(final Queue<Object> delegate) {

            // convert all elements from the previous queue to the current one, if there are any
            while (!delegate.isEmpty()) {
                add(delegate.poll());
            }
        }

        @Override
        public Object poll() {
            Object otherTask;
            if ((otherTask = super.poll()) != null && otherTask != EMPTY_VALUE)
                return otherTask;

            synchronized (packetSendQueue) {
                if (!startup) {
                    initiallyTickRound = true;
                    startup = true;
                }
                workingProperly = true;

                // ignore packets in the first tick round, because the synchronization with the packet send queue might be wrong
                if (!initiallyTickRound) {

                    // check if this packet is not the first one handled in this tick
                    if (recentPollStart != -1 && recentPacketAuthor.get() != null) {

                        // evaluate the difference in time
                        double deltaTime = ((double) (System.nanoTime() - recentPollStart)) / 1_000_000;

                        Player player = Bukkit.getPlayer(recentPacketAuthor.get());
                        recentPacketAuthor.set(null);

                        if (player != null && player.isOnline()) {
                            Bukkit.getLogger().info(player.getName() + ": " + deltaTime);
                            registerTime(player, deltaTime);
                        }
                    }
                    recentPollStart = System.nanoTime();
                }
                Thread thread = seperatedQueue.getAnyQueuedThread();

                recentPacketAuthor.set(packetSendQueue.poll(thread));
                Object task = seperatedQueue.poll(thread);

                if (task == null) {
                    // all packets in the queue has been handled, and no packets for this tick remains to be checked
                    recentPollStart = -1;

                    if (initiallyTickRound) {
                        packetSendQueue.clear();
                        initiallyTickRound = false;
                    }
                }
                //Bukkit.broadcastMessage("Sum: " + seperatedQueue.getSize());
                return task == null ? null : recentPacketAuthor.get() == null ? task : PacketTaskHandler.wrapTask(task, recentPacketAuthor.get());
            }
        }

        @Override
        public boolean add(Object value) {
            if (!Thread.currentThread().getName().startsWith("Netty"))
                return super.add(value);

            // add it two times if it's empty, so we always have one extra element
            if (super.isEmpty()) super.add(EMPTY_VALUE);

            super.add(EMPTY_VALUE);
            return seperatedQueue.add(value);
        }
    }

    @Override
    public List<Player> getTrackersOrdered() {
        List<Player> trackersList = new ArrayList<>(getTrackers());
        trackersList.sort(new Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return Double.compare(getStatistic(o1, 0), getStatistic(o2, 0));
            }
        });
        return trackersList;
    }
}
