package me.alek.honey.tracker;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.*;
import me.alek.honey.tracker.packet.WrappedPacketTaskFuture;
import me.alek.honey.tracker.packet.PacketTaskHandler;
import me.alek.honey.tracker.temp.impl.TempTimedQueuedTracker;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerThreadHandleTracker extends WideTempQueuedHandleTracker {

    private final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = HoneyPacketHandler.getPacketSendMarker();
    private final Map<UUID, Integer> packetsInQueueExecution = new HashMap<>();
    private final Map<UUID, Integer> ignoredPacketsDecrementInQueueExecution = new HashMap<>();
    private PacketQueueWrapper executionQueue;
    public Set<PacketTypeEnum> ignoredPacketsInQueueExecutionSaver;

    public ServerThreadHandleTracker() {
        reload();
        tryInject();
    }

    public void reload() {
        Set<PacketTypeEnum> packets = new HashSet<>();
        for (String ignoredPacket : ConfigurationOptions.Netty.NettyA.IGNORED_PACKETS) {

            PacketTypeEnum packetType = PacketWrapperInvokerFactory.getPacketTypeForNameIgnoreCase(ignoredPacket);
            if (packetType != null) {
                packets.add(packetType);

            } else {
                for (PacketTypeEnum type : PacketType.getTypesAvailableFor(NetProtocol.getProtocol())) {
                    if (type != null && type.toString().equalsIgnoreCase(ignoredPacket)) {
                        packets.add(type);
                        break;
                    }
                }
            }
        }
        ignoredPacketsInQueueExecutionSaver = packets;
    }

    private void tryInject() {
        Class<?> queueHolderClass = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14)
                ? NMS.getMinecraftServerClass() : NMS.getIAsyncTaskHandlerClass();
        Object minecraftServer = NMS.getMinecraftServer();

        FieldAccessor<Queue<?>> queueAccessor = Reflection.getField(queueHolderClass, 0, Queue.class);

        PacketQueueWrapper queueWrapper = executionQueue = new PacketQueueWrapper((Queue<Object>) queueAccessor.get(minecraftServer));
        try {
            Reflection.setFinalField(minecraftServer, queueAccessor, queueWrapper);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Queue<Object> getExecutionQueue() {
        return executionQueue;
    }

    public TempTimedQueuedTracker getPropertyTracker(HoneyPlayer player) {
        return player.serverThreadTimedQueuedTracker;
    }

    public void setPropertyTracker(HoneyPlayer player, TempTimedQueuedTracker tracker) {
        player.serverThreadTimedQueuedTracker = tracker;
    }

    private final class PacketQueueWrapper extends ConcurrentLinkedQueue<Object> {

        private final ConcurrentLinkedQueue<WrappedPacketTaskFuture<Object>> nettySubmitQueue = new ConcurrentLinkedQueue<>();
        private PacketContainer<? extends WrappedPacket<?>> recentPacket;
        private ConstructorInvoker<?> tickTaskConstructor;
        private final boolean pollsOneExtra;
        private long recentPollStart = -1;
        private double recentDeltaTime = -1;
        private boolean nullPoll;
        private final Object EMPTY_VALUE = new Object();
        private final Object EMPTY_TICK_TASK;
        private int positionPacketsAmount;
        private long positionPacketsTime;

        public PacketQueueWrapper(final Queue<Object> delegate) {
            EMPTY_TICK_TASK = (pollsOneExtra = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14)) ? null
                    : (tickTaskConstructor = Reflection.getConstructor(NMS.getTickTaskClass(), int.class, Runnable.class))
                    .invoke(Integer.MAX_VALUE - 3, (Runnable) () -> {});

            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_18)) {
                tickTaskConstructor = null;
            }

            // convert all elements from the previous queue to the current one, if there are any
            while (!delegate.isEmpty()) {
                add(delegate.poll());
            }

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                positionPacketsAmount = 0;
                positionPacketsTime = 0;
            }, 30L, 30L, TimeUnit.SECONDS);
        }

        private double getDeltaTime() {
            return System.nanoTime() - recentPollStart;
        }

        private double getDeltaTimeExact(PacketContainer<? extends WrappedPacket<?>> packet) {
            double timings = packet.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_HANDLE);

            return timings != 0 ? timings : getDeltaTime();
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
                if (recentPollStart != -1) {
                    recentDeltaTime = getDeltaTime();
                }
                recentPollStart = -1;
                return otherTask;
            }
            // check if this packet is not the first one handled in this tick
            if (recentPollStart != -1 && recentPacket != null) {

                if (recentPacket.getPlayer() != null && recentPacket.getPlayer().isOnlineDirect()
                        && !(ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && recentPacket.getPlayer().player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION))) {
                    // evaluate the difference in time
                    double deltaTime = recentDeltaTime != -1 ? recentDeltaTime : getDeltaTimeExact(recentPacket);

                    // don't track position packets, because you can legitimately cause a lot of server thread performance by chunk loading in legacy versions (when chunk loading wasn't async)
                    if (recentPacket.getType() == PacketType.Play.Client.POSITION
                            || recentPacket.getType() == PacketType.Play.Client.POSITION_LOOK
                            || recentPacket.getType() == PacketType.Play.Client.FLYING
                            || recentPacket.getType() == PacketType.Play.Client.GROUND
                            || recentPacket.getType() == PacketType.Play.Client.VEHICLE_MOVE) {
                        positionPacketsAmount++;
                        positionPacketsTime += (long) deltaTime;

                        deltaTime = Math.min(deltaTime, 2.25d * positionPacketsTime / positionPacketsAmount);
                    }

                    registerTime(recentPacket.getPlayer(), deltaTime);
                }
                recentDeltaTime = -1;
                recentPacket = null;
            }
            recentPollStart = System.nanoTime();

            WrappedPacketTaskFuture<Object> task = nettySubmitQueue.poll();
            recentPacket = Optional.ofNullable(task).map(WrappedPacketTaskFuture::getPacket).orElse(null);

            if (task != null) {
                HoneyPlugin.get().getLoggerService().registerQueueExecutionPacket();
            }

            Object tickTask = task == null ? null : newTickTask(task);

            // if the queue has exactly the same amount of elements as the delegated queue, we have to
            // make sure that the tickloop is being completed by polling one extra time
            if (!nullPoll && !pollsOneExtra && super.peek() == null) {
                nullPoll = true;
                poll();
            }

            if (task == null) {
                packetsInQueueExecution.clear();
                nettySubmitQueue.clear();

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

            // only register tracking for tasks added by netty threads (packet handling)
            if (!Thread.currentThread().getName().startsWith("Netty")) return super.add(value);

            PacketContainer<? extends WrappedPacket<?>> packet = packetSendMarker.get();
            if (packet == null || packet.isHandledByServer() || packet.getPlayer() == null) {
                return super.add(value);
            }
            packet.setHandledByServer();

            // add it two times if it's empty, so we always have one extra element
            // this is because in versions below 1.14, it polls until it reaches null, which means one extra time
            if (super.isEmpty() && pollsOneExtra) super.add(EMPTY_VALUE);

            if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && packet.getPlayer().player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION))
                return super.add(value);

            UUID uuid = packet.getPlayer().uuid;
            synchronized (PacketTaskHandler.ignoreDecrementLock) {
                if (!ignoredPacketsInQueueExecutionSaver.contains(packet.getType())) {
                    packetsInQueueExecution.put(uuid, packetsInQueueExecution.getOrDefault(uuid, 0) + 1);
                } else ignoredPacketsDecrementInQueueExecution.put(uuid, ignoredPacketsDecrementInQueueExecution.getOrDefault(uuid, 0) + 1);
            }
            super.add(EMPTY_VALUE);

            return nettySubmitQueue.add(new WrappedPacketTaskFuture<>((Runnable) value, packet.getPlayer(), packet));
        }
    }

    public void decrementPacketsInQueueExecution(UUID uuid) {
        synchronized (PacketTaskHandler.ignoreDecrementLock) {
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
                return Double.compare(getTimeToAverageWithoutQuotient(o1, Timespan.FIVE_SECOND, true), getTimeToAverageWithoutQuotient(o2, Timespan.FIVE_SECOND, true));
            }
        });
        return trackersList;
    }
}
