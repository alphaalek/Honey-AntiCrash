package me.alek.honey.tickloop;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.packet.WrappedPacketTaskFuture;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import me.alek.honey.tracker.temp.TempQueuedHandleTracker;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ExecutionQueueProxy extends TempQueuedHandleTracker {

    private final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = HoneyPacketHandler.getPacketSendMarker();
    private DelegatedWrapper executionQueue;
    private boolean executing;
    private int doneTicks;
    private int doneExecutionTicks;

    public ExecutionQueueProxy() {
        inject();
    }

    public void inject() {
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

    public TempQueuedTracker getPropertyTracker(HoneyPlayer player) {
        return player.serverThreadTimedQueuedTracker;
    }

    public void setPropertyTracker(HoneyPlayer player, TempQueuedTracker tracker) {
        player.serverThreadTimedQueuedTracker = tracker;
    }

    public Supplier<Integer> getTickAccessor() {
        IStructureModifier<Integer> tickStructureModifier = ReflectStructureCache.acquireStructure(NMS.getMinecraftServerClass())
                .withInts().withTarget(NMS.getMinecraftServer());
        int finalFields = (int) tickStructureModifier.getAccessors()
                .stream()
                .filter(field -> Modifier.isFinal(field.getHandle().getModifiers()))
                .count();

        return () -> {
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
                return tickStructureModifier.readField(null, "ticks", 0);
            } else {
                return tickStructureModifier.readField(finalFields + 1);
            }
        };
    }

    private final class DelegatedWrapper extends WrappedQueue<Object> {

        private final Supplier<Integer> tickAccessor = getTickAccessor();
        private Class<?> tickTaskClass;
        private ConstructorInvoker<?> tickTaskConstructor;

        private Array<WrappedPacketTaskFuture> submitQueue = new Array<>();
        private int currentPollIndex;
        private PacketContainer<? extends WrappedPacket<?>> recentPacket;

        private long recentPollStart = -1;
        private double recentDeltaTime = -1;

        private final Object EMPTY_VALUE = new Object();
        private final Object EMPTY_TICK_TASK;

        public DelegatedWrapper(final Queue<Object> delegate) {
            super(delegate);
            EMPTY_TICK_TASK = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_14) ? null :
                    (tickTaskConstructor = Reflection.getConstructor((tickTaskClass = NMS.getTickTaskClass()), int.class, Runnable.class))
                            .invoke(Integer.MAX_VALUE - 3, (Runnable) () -> {
                            });

            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_18)) {
                tickTaskConstructor = null;
            }
        }

        private double getDeltaTime() {
            return System.nanoTime() - recentPollStart;
        }

        private double getDeltaTimeExact(PacketContainer<? extends WrappedPacket<?>> packet) {
            double timings = packet.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_HANDLE);

            return timings != 0 ? timings : getDeltaTime();
        }

        private Object newTickTask(Runnable runnable, int currentTick) {
            if (tickTaskConstructor == null) {
                return runnable;
            }

            return tickTaskConstructor.invoke(currentTick, runnable);
        }

        private Object getEmptyTask() {
            return newTickTask(new FutureTask<>(() -> null), tickAccessor.get());
        }

        @Override
        public Object peek() {
            // if we have any packets ready to be run, we will return an empty value to mark we're ready
            // if not, we will just return null and tell the server that the delegated queue is empty
            if (super.peek() == null) {
                return null;
            }

            // this runnable is not run by the server, just used to check if the task can be executed
            // so we just return a runnable with an empty body and instead do the tracking work in methods used afterward
            return EMPTY_TICK_TASK;
        }

        @Override
        public Object remove() {
            Object poll;
            if ((poll = poll()) != null) {
                return poll;
            }

            return EMPTY_TICK_TASK;
        }

        @Override
        public synchronized Object poll() {
            executing = true;
            Object otherTask;
            if ((otherTask = super.poll()) != null && otherTask != EMPTY_VALUE) {
                if (recentPollStart != -1) {
                    recentDeltaTime = getDeltaTime();
                }
                recentPollStart = -1;

                return getOtherTask(otherTask);
            }

            if (recentPollStart != -1 && recentPacket != null) {
                handleTimings();
            }

            WrappedPacketTaskFuture task = submitQueue.poll(currentPollIndex++);
            recentPacket = Optional.ofNullable(task).map(WrappedPacketTaskFuture::getPacket).orElse(null);

            recentPollStart = -1;
            if (task == null) {
                tickLoopExecutionDone();
                return null;
            }

            if (task.isPostponed()) {
                task.getPlayer().postponedPackets--;
            }

            if (task.getPlayer().postponedLastTick != getCurrentExecutionTick()) {
                task.getPlayer().postponedPacketsForTick = 0;
                task.getPlayer().postponedLastTick = getCurrentExecutionTick();
            }

            if (recentPacket != null) {
                recentPacket.setHandledByServer();
            }

            if (shouldDropTask(task)) {
                if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13_1)) {
                    submitQueue.set(currentPollIndex - 1, task);
                    task = submitQueue.poll(findOtherTaskIndex());
                }

                if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
                    HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled due to postpone overload: "
                            + task.getPacket().getType() + ", "
                            + task.getPlayer().packetHandler.getProtocolState(task.getPacket().getType().getBound()) + ", "
                            + task.getPacket().getType().getBound() + ", "
                            + task.getPacket().getPlayerName());
                }

                return getEmptyTask();
            }

            if (shouldPostponeTask(task)) {
                HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(
                        task.getPlayer(), !task.isPostponed(), false);

                task.getPlayer().postponedPackets++;
                task.getPlayer().postponedPacketsForTick++;
                task.setPostponed();
                submitQueue.set(currentPollIndex - 1, task);
                super.add(EMPTY_VALUE);

                return getEmptyTask();
            }

            recentPollStart = System.nanoTime();
            return newTickTask(task, doneTicks);
        }

        @Override
        public synchronized boolean add(Object value) {
            // why add something to an execution queue, if it's not a runnable?
            if (!(value instanceof Runnable)) {
                return false;
            }

            // only register tracking for tasks added by netty threads (packet handling)
            if (!Thread.currentThread().getName().startsWith("Netty")) {
                return super.add(value);
            }

            PacketContainer<? extends WrappedPacket<?>> packet = packetSendMarker.get();
            if (packet == null
                    || packet.isHandledByServer()
                    || !packet.getPlayer().isBukkitReady()
                    || ConfigurationOptions.General.BYPASS_PERMISSION_SERVER_THREAD_ENABLED
                            && packet.getPlayer().bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
                return super.add(value);
            }

            return addPacketTask(
                    new WrappedPacketTaskFuture(
                            (Runnable) value,
                            packet,
                            ExecutionQueueProxy.this.getCurrentExecutionTick())
                    );
        }

        private boolean addPacketTask(WrappedPacketTaskFuture task) {
            if (super.isEmpty()) {
                // add it two times if it's empty, so we always have one extra element to stop tracking of the last packet task
                super.add(EMPTY_VALUE);
            }

            super.add(EMPTY_VALUE);

            submitQueue.add(task);
            return true;
        }

        private boolean isPacketDroppable(PacketContainer<? extends WrappedPacket<?>> packet) {
            return packet.getType() == PacketTypes.Play.Client.PLAYER_MOVEMENT
                    || packet.getType() == PacketTypes.Play.Client.PLAYER_POSITION
                    || packet.getType() == PacketTypes.Play.Client.PLAYER_POS_AND_ROT
                    || packet.getType() == PacketTypes.Play.Client.GROUND
                    || packet.getType() == PacketTypes.Play.Client.BOAT_MOVE
                    || packet.getType() == PacketTypes.Play.Client.VEHICLE_MOVE;
        }

        private int findOtherTaskIndex() {
            WrappedPacketTaskFuture first = submitQueue.get(currentPollIndex - 1);
            if (first.isPostponed() && isPacketDroppable(first.getPacket())) {
                return currentPollIndex - 1;
            }

            for (int i = currentPollIndex; i < submitQueue.size(); i++) {
                WrappedPacketTaskFuture task = submitQueue.get(i);
                if (task != null && first.isPostponed() && isPacketDroppable(task.getPacket())) {
                    return i;
                }
            }

            return currentPollIndex - 1;
        }

        private boolean shouldDropTask(WrappedPacketTaskFuture task) {
            return task.getPlayer().postponedPackets > ConfigurationOptions.Postpone.MAX_POSTPONED_PACKETS
                    || getCurrentExecutionTick() - task.getTickAdded() > ConfigurationOptions.Postpone.MAX_POSTPONED_TICKS;
        }

        private boolean shouldPostponeTask(WrappedPacketTaskFuture task) {
            if (!ConfigurationOptions.Postpone.ENABLED) {
                return false;
            }

            HoneyPlayer player = task.getPlayer();
            if (player.postponedPacketsForTick > 0) {
                return true;
            }

            Array<Object> pipelineData = new Array<>();
            Callback postponed = new Callback();
            for (PostponeCheck check : task.getPlayer().postponedChecks) {
                try {
                    if (check.test(task.getPacket(), task.getPlayer(), pipelineData, postponed)) {
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
            } else if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_14_4) && !tickTaskClass.isInstance(otherTask)) {
                otherTask = newTickTask((Runnable) otherTask, tickAccessor.get());
            }

            return (Runnable) otherTask;
        }

        private void handleTimings() {
            // evaluate the difference in time
            double deltaTime = recentDeltaTime != -1 ? recentDeltaTime : getDeltaTimeExact(recentPacket);
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13_1)
                    && isPacketDroppable(recentPacket)) {
                deltaTime = Math.min(deltaTime, ConfigurationOptions.Postpone.MAX_POSITION_TIME_REGISTER * Timespan.MILLIS_TO_NANO_MULTIPLIER);
            }
            registerTime(recentPacket.getPlayer(), deltaTime);

            recentDeltaTime = -1;
            recentPacket = null;
        }

        private void tickLoopExecutionDone() {
            currentPollIndex = 0;
            recentPollStart = -1;
            submitQueue = submitQueue.compact();

            HoneyPlugin.get().getLoggerService().registerQueueExecutionStop();

            if (Thread.interrupted()) {
                HoneyPlugin.get().getLoggerService().getLogger().info("Server thread was interrupted by packet. Clearing interrupted flag.");
            }

            doneTicks = tickAccessor.get();
            doneExecutionTicks++;
            executing = false;
        }
    }

    public int getCurrentExecutionTick() {
        return doneExecutionTicks + (executing ? 1 : 0);
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
