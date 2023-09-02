package me.alek.honey.tracker.impl;

import me.alek.honey.netty.injector.InboundPacketDecoder;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.tracker.AbstractOldAndCurrentHandleTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerServerThreadHandleTracker extends AbstractOldAndCurrentHandleTracker {

    private final ConcurrentLinkedQueue<UUID> packetSendQueue = InboundPacketDecoder.getPacketSendQueue();
    private boolean workingProperly;
    private boolean initiallyInjected;

    public PlayerServerThreadHandleTracker(JavaPlugin plugin) {
        super(plugin, 201L, 5, 15, 30, 60, 180);
        update();

        try {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            executorService.schedule(() -> {
                if (!initiallyInjected || (!workingProperly && Bukkit.getOnlinePlayers().size() > 0)) tryInject();

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

        private final List<Object> delegatedIgnoreList = new ArrayList<>();
        private final AtomicReference<UUID> recentPacketAuthor = new AtomicReference<>();
        private long recentPollStart = -1;

        public PacketQueueWrapper(final Queue<Object> delegate) {

            // convert all elements from the previous queue to the current one, if there are any
            while (!delegate.isEmpty()) {
                add(delegate.poll());
            }
        }

        @Override
        public Object poll() {
            synchronized (packetSendQueue) {
                workingProperly = true;

                // check if this packet is not the first one handled in this tick
                if (recentPollStart != -1 && recentPacketAuthor.get() != null) {

                    // evaluate the difference in time
                    double deltaTime = ((double) (System.nanoTime() - recentPollStart)) / 1_000_000;

                    Player player = Bukkit.getPlayer(recentPacketAuthor.get());
                    // cancel this packet and return if the player has quit the server while packets were still waiting in the queue
                    if (player == null || !player.isOnline()) return null;

                    registerTime(player, deltaTime);
                    recentPacketAuthor.set(null);
                }
                recentPollStart = System.nanoTime();

                // set the author of the current packet
                UUID author = packetSendQueue.poll();
                recentPacketAuthor.set(author != null ? author : recentPacketAuthor.get());

                Object value = super.poll();
                // all packets in the queue has been handled, and no packets for this tick remains to be checked
                if (value == null || delegatedIgnoreList.contains(value)) {
                    recentPollStart = -1;
                    delegatedIgnoreList.remove(value);

                    if (value == null) packetSendQueue.clear();
                }
                return value;
            }
        }

        @Override
        public boolean add(Object value) {
            if (!Thread.currentThread().getName().startsWith("Netty")) delegatedIgnoreList.add(value);

            return super.add(value);
        }
    }
}
