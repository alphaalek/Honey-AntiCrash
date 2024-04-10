package me.alek.honey.tickloop;

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
import me.alek.honey.netty.util.*;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeHandler;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tickloop.packet.WrappedPacketTaskFuture;
import me.alek.honey.tickloop.packet.PacketTaskHandler;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.impl.TempTimedQueuedTracker;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.*;

public class ExecutionQueueWrapper extends WideTempQueuedHandleTracker {

    private final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = HoneyPacketHandler.getPacketSendMarker();
    private final Map<UUID, Integer> packetsInQueueExecution = new HashMap<>();
    private final Map<UUID, Integer> ignoredPacketsDecrementInQueueExecution = new HashMap<>();
    private DelegatedWrapper executionQueue;
    public IDRegistry ignoredPacketsInQueueExecutionSaver;
    private int tickLoop;

    public ExecutionQueueWrapper() {
        reload();
        injectIfNot();
    }

    public void reload() {
        Set<PacketTypeEnum> packets = new HashSet<>();
        for (String ignoredPacket : ConfigurationOptions.Postpone.MAX_QUEUE_SIZE_IGNORED) {

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

        ignoredPacketsInQueueExecutionSaver = new IDRegistry();
        for (PacketTypeEnum packetType : packets) {
            ignoredPacketsInQueueExecutionSaver.addID(packetType.getPacketId());
        }
    }

    public void injectIfNot() {
        Class<?> queueHolderClass = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14)
                ? NMS.getMinecraftServerClass() : NMS.getIAsyncTaskHandlerClass();
        Object minecraftServer = NMS.getMinecraftServer();

        FieldAccessor<Queue<?>> queueAccessor = Reflection.getField(queueHolderClass, 0, Queue.class);
        if (queueAccessor.get(minecraftServer) instanceof DelegatedWrapper) {
            return;
        }

        DelegatedWrapper queueWrapper = executionQueue = new DelegatedWrapper((Queue<Object>) queueAccessor.get(minecraftServer));
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

    private final class DelegatedWrapper extends ConcurrentLinkedQueue<Object> {

        private final double MAX_POSITION_TACKING = 10 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private final double MAX_POSITION_TRACKING_AVERAGE_ENTRY = 50 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private final ConcurrentLinkedQueue<WrappedPacketTaskFuture<Object>> submitQueue = new ConcurrentLinkedQueue<>();
        private PacketContainer<? extends WrappedPacket<?>> recentPacket;
        private Class<?> tickTaskClass;
        private ConstructorInvoker<?> tickTaskConstructor;
        private final boolean pollsOneExtra;
        private long recentPollStart = -1;
        private double recentDeltaTime = -1;
        private boolean nullPoll;
        private final Object EMPTY_VALUE = new Object();
        private final Object EMPTY_TICK_TASK;
        private int positionPacketsAmount;
        private long positionPacketsTime;

        public DelegatedWrapper(final Queue<Object> delegate) {
            EMPTY_TICK_TASK = (pollsOneExtra = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14)) ? null :
                    (tickTaskConstructor = Reflection.getConstructor((tickTaskClass = NMS.getTickTaskClass()), int.class, Runnable.class))
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
            if (tickTaskConstructor == null) {
                return runnable;
            }

            return tickTaskConstructor.invoke(Integer.MAX_VALUE - 3, runnable);
        }

        private Object getEmptyTask() {
            return newTickTask(new FutureTask<>(() -> null));
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

                return getOtherTask(otherTask);
            }

            // check if this packet task should be tracked
            if (recentPollStart != -1 && recentPacket != null) {
                handleTimings();
            }

            WrappedPacketTaskFuture<Object> task = submitQueue.poll();
            recentPacket = Optional.ofNullable(task).map(WrappedPacketTaskFuture::getPacket).orElse(null);

            if (task != null) {
                HoneyPlugin.get().getLoggerService().registerQueueExecutionPacket();
            }

            // if the queue has exactly the same amount of elements as the delegated queue, we have to
            // make sure that the tick loop tracking is being completed by polling one extra time
            if (!nullPoll && !pollsOneExtra && super.peek() == null) {
                nullPoll = true;
                recentPollStart = System.nanoTime();
                poll();
            }

            if (task == null) {
                tickLoopExecutionDone();
            } else if (shouldPostponeTask(task)) {
                if (!task.isPostponed()) {
                    task.setPostponed();
                    HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(task.getPlayer(), true, false);
                }

                recentPollStart = -1;
                return getEmptyTask();
            }

            if (recentPacket != null) {
                recentPacket.setHandledByServer();
            }

            recentPollStart = System.nanoTime();

            return task == null ? null : newTickTask(task);
        }

        @Override
        public synchronized boolean add(Object value) {
            // why add something to an execution queue, without it being a runnable?
            if (!(value instanceof Runnable)) {
                return false;
            }

            if (value instanceof WrappedPacketTaskFuture) {
                if (super.isEmpty() && pollsOneExtra) {
                    super.add(EMPTY_VALUE);
                }

                super.add(EMPTY_VALUE);
                return submitQueue.add((WrappedPacketTaskFuture<Object>) value);
            }

            // only register tracking for tasks added by netty threads (packet handling)
            if (!Thread.currentThread().getName().startsWith("Netty")) {
                return super.add(value);
            }

            PacketContainer<? extends WrappedPacket<?>> packet = packetSendMarker.get();
            if (packet == null || packet.isHandledByServer() || packet.getPlayer() == null
                    || ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && packet.getPlayer().player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
                return super.add(value);
            }

            // add it two times if it's empty, so we always have one extra element
            // this is because in versions below 1.14, it polls until it reaches null, which means one extra time
            if (super.isEmpty() && pollsOneExtra) {
                super.add(EMPTY_VALUE);
            }

            super.add(EMPTY_VALUE);

            UUID uuid = packet.getPlayer().uuid;
            synchronized (PacketTaskHandler.ignoreDecrementLock) {
                if (!ignoredPacketsInQueueExecutionSaver.hasID(packet.getType().getPacketId())) {
                    packetsInQueueExecution.put(uuid, packetsInQueueExecution.getOrDefault(uuid, 0) + 1);
                } else {
                    ignoredPacketsDecrementInQueueExecution.put(uuid, ignoredPacketsDecrementInQueueExecution.getOrDefault(uuid, 0) + 1);
                }
            }

            return submitQueue.add(new WrappedPacketTaskFuture<>((Runnable) value, packet.getPlayer(), packet, tickLoop));
        }

        private boolean shouldPostponeTask(WrappedPacketTaskFuture<?> task) {
            if (!ConfigurationOptions.Postpone.ENABLED) {
                return false;
            }

            PostponeHandler postponeHandler = task.getPlayer().tickloopHandler.getPostponeHandler();
            if (postponeHandler.isPostponing()) {
                postponeHandler.addPostponedPacket(task);
                return true;
            }

            Array<Object> pipelineData = new Array<>();
            CallbackAction<Boolean> postponed = new CallbackAction<>();
            for (PostponeCheck check : postponeHandler.getPostponeChecks()) {
                try {
                    if (check.test(task.getPacket(), task.getPlayer(), pipelineData, postponed)) {
                        Bukkit.getLogger().info("Postponing " + check);
                        postponeHandler.setPostponing();
                        postponeHandler.addPostponedPacket(task);
                        postponed.call(true);
                        return true;
                    }
                } catch (PostponeTerminationException exception) {
                    break;
                }
            }

            postponed.call(false);
            return false;
        }

        private Runnable getOtherTask(Object otherTask) {
            if (!(otherTask instanceof Runnable)) {
                return null;
            }

            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_14_4) && !(otherTask instanceof FutureTask)) {
                if (otherTask instanceof Callable) {
                    otherTask = new FutureTask<>((Callable<?>) otherTask);
                } else {
                    Runnable otherTaskRunnable = (Runnable) otherTask;
                    otherTask = new FutureTask<>((Callable<? extends Object>) () -> {
                        otherTaskRunnable.run();
                        return null;
                    });
                }
            }
            else if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_14_4) && !tickTaskClass.isInstance(otherTask)) {
                otherTask = newTickTask((Runnable) otherTask);
            }

            return (Runnable) otherTask;
        }

        private void handleTimings() {
            if (recentPacket.getPlayer() != null && recentPacket.getPlayer().isOnlineWeak()
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
                    positionPacketsTime += (long) Math.min(MAX_POSITION_TRACKING_AVERAGE_ENTRY, deltaTime);

                    deltaTime = Math.min(deltaTime, Math.min(MAX_POSITION_TACKING, 5d * positionPacketsTime / positionPacketsAmount));
                }

                registerTime(recentPacket.getPlayer(), deltaTime);
            }
            recentDeltaTime = -1;
            recentPacket = null;
        }

        private void tickLoopExecutionDone() {
            packetsInQueueExecution.clear();
            submitQueue.clear();

            recentPollStart = -1;
            nullPoll = false;

            HoneyPlugin.get().getLoggerService().registerQueueExecutionStop();

            if (Thread.interrupted()) {
                HoneyPlugin.get().getLoggerService().getLogger().info("Server thread was interrupted by packet. Clearing interrupted flag.");
            }

            PostponeHandler.retryPostponedPackets(this);
            tickLoop++;
        }
    }

    public void decrementPacketsInQueueExecution(UUID uuid) {
        synchronized (PacketTaskHandler.ignoreDecrementLock) {
            int ignoredDecrement = ignoredPacketsDecrementInQueueExecution.getOrDefault(uuid, 0);
            if (ignoredDecrement > 0) {
                ignoredPacketsDecrementInQueueExecution.put(uuid, ignoredDecrement - 1);
            }

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

    public int getCurrentTick() {
        return tickLoop;
    }

    public int getPacketsInQueueExecution(UUID uuid) {
        return packetsInQueueExecution.getOrDefault(uuid, 0);
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        List<HoneyPlayer> trackersList = new ArrayList<>(getTrackers());
        trackersList.removeIf(player -> getPlayerRate(player, Timespan.FIVE_SECOND, false) == 0);
        trackersList.sort(new Comparator<HoneyPlayer>() {

            @Override
            public int compare(HoneyPlayer o1, HoneyPlayer o2) {
                return Double.compare(getTimeToAverageWithoutQuotient(o1, Timespan.FIVE_SECOND, true), getTimeToAverageWithoutQuotient(o2, Timespan.FIVE_SECOND, true));
            }
        });
        return trackersList;
    }
}
