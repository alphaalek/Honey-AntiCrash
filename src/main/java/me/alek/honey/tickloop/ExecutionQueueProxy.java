package me.alek.honey.tickloop;

import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.Runnables;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.tickloop.packet.PacketTaskProvider;
import me.alek.honey.tickloop.packet.WrappedPacketTask;
import me.alek.honey.tickloop.postpone.PostponeValidation;
import me.alek.honey.tickloop.postpone.PostponeTerminationException;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.TempIntervalTrackerManager;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

public class ExecutionQueueProxy extends TempIntervalTrackerManager {

    private DelegatedWrapper executionQueue;
    private boolean executing;
    private int doneTicks;
    private int doneExecutionTicks;

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

    public int getCurrentExecutionTick() {
        return doneExecutionTicks + (executing ? 1 : 0);
    }

    public Queue<Object> getExecutionQueue() {
        return executionQueue;
    }

    public TempIntervalTracker getPropertyTracker(HoneyPlayer player) {
        return player.serverThreadTimedQueuedTracker;
    }

    public void setPropertyTracker(HoneyPlayer player, TempIntervalTracker tracker) {
        player.serverThreadTimedQueuedTracker = tracker;
    }

    public Supplier<Integer> getTickAccessor() {
        IStructureModifier<Integer> tickStructureModifier = ReflectStructure.Provider.acquireStructure(NMS.getMinecraftServerClass())
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

    private final class DelegatedWrapper extends WrappedSynchronizedQueue<Object> {

        private final Supplier<Integer> tickAccessor = getTickAccessor();
        private Class<Object> tickTaskClass;
        private ConstructorInvoker<?> tickTaskConstructor;

        private Array<WrappedPacketTask> submitQueue = new Array<>();
        private final Queue<Integer> collector = new LinkedList<>();
        private int currentPollIndex;
        private PacketContainer<? extends WrappedPacket<?>> recentPacket;

        private long recentPollStart = -1;
        private long recentDeltaTime = -1;

        private final Object EMPTY_VALUE = new Object();
        private final Object EMPTY_TICK_TASK;

        public DelegatedWrapper(final Queue<Object> delegate) {
            super(new LinkedList<>(delegate));

            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
                tickTaskConstructor = Reflection.getConstructor((tickTaskClass = NMS.getTickTaskClass()), int.class, Runnable.class);
                EMPTY_TICK_TASK = tickTaskConstructor.invoke(0, Runnables.doNothing());
            } else {
                EMPTY_TICK_TASK = null;
            }

            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_18)) {
                tickTaskConstructor = null;
            }
        }

        private long getDeltaTime() {
            return System.nanoTime() - recentPollStart;
        }

        private long getDeltaTimeExact(PacketContainer<? extends WrappedPacket<?>> packet) {
            long timings = packet.getEventloopStateTimings().getTimeFor(LifecycleState.SERVER_HANDLE);

            return timings != 0 ? timings : getDeltaTime();
        }

        private Object newTickTask(Runnable runnable, int currentTick) {
            if (tickTaskConstructor == null) {
                return runnable;
            }

            return tickTaskConstructor.invoke(currentTick, runnable);
        }

        private Object getEmptyTask() {
            return newTickTask(new FutureTask<>(Callables.returning(null)), tickAccessor.get());
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
            Object polled = poll();
            if (polled != null) {
                return polled;
            }

            return EMPTY_TICK_TASK;
        }

        @Override
        public synchronized Object poll() {
            executing = true;
            Object poll = super.poll();
            if (poll != null && poll != EMPTY_VALUE) {
                if (recentPollStart != -1) {
                    recentDeltaTime = getDeltaTime();
                }
                recentPollStart = -1;

                return getOtherTask(poll);
            }

            if (recentPollStart != -1 && recentPacket != null) {
                handleTimings();
            }

            WrappedPacketTask task = submitQueue.poll(currentPollIndex++, false);
            recentPacket = Optional.ofNullable(task).map(WrappedPacketTask::getPacket).orElse(null);

            recentPollStart = -1;
            if (task == null) {
                tickLoopExecutionDone();
                return null;
            }

            HoneyPlugin.get().getLogger().registerQueueExecutionPacket();

            if (task.isPostponed()) {
                task.getPlayer().postponedPackets--;
            }

            if (task.getPlayer().postponedLastTick != getCurrentExecutionTick()) {
                task.getPlayer().postponedPacketsForTick = 0;
                task.getPlayer().postponedLastTick = getCurrentExecutionTick();
            }

            if (shouldDropTask(task)) {
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
                task.postpone();
                submitQueue.set(currentPollIndex - 1, task);
                collector.add(currentPollIndex - 1);
                super.add(EMPTY_VALUE);

                return getEmptyTask();
            }

            recentPollStart = System.nanoTime();
            return newTickTask(task.getPacketTask(), doneTicks);
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

            PacketContainer<? extends WrappedPacket<?>> packet = HoneyPacketHandler.MARK.get();
            if (packet == null || !packet.getPlayer().isStreamActive()) {
                return false;
            }

            return addPacketTask(
                    PacketTaskProvider.PROVIDER.create(
                            (Runnable) value,
                            packet,
                            ExecutionQueueProxy.this.getCurrentExecutionTick()
                    )
            );
        }

        private boolean addPacketTask(WrappedPacketTask task) {
            if (super.isEmpty()) {
                // add it two times if it's empty, so we always have one extra element to stop tracking of the last packet task
                super.add(EMPTY_VALUE);
            }

            super.add(EMPTY_VALUE);

            submitQueue.add(task);
            return true;
        }

        private boolean isPositionPacket(PacketContainer<? extends WrappedPacket<?>> packet) {
            return packet.getType() == PacketTypes.Play.Client.PLAYER_MOVEMENT
                    || packet.getType() == PacketTypes.Play.Client.PLAYER_POSITION
                    || packet.getType() == PacketTypes.Play.Client.PLAYER_POS_ROT
                    || packet.getType() == PacketTypes.Play.Client.GROUND
                    || packet.getType() == PacketTypes.Play.Client.BOAT_MOVE
                    || packet.getType() == PacketTypes.Play.Client.VEHICLE_MOVE;
        }

        private boolean shouldDropTask(WrappedPacketTask task) {
            return task.getPlayer().postponedPackets > ConfigurationOptions.Postpone.MAX_POSTPONED_PACKETS
                    || getCurrentExecutionTick() - task.getTickAdded() > ConfigurationOptions.Postpone.MAX_POSTPONED_TICKS;
        }

        private boolean shouldPostponeTask(WrappedPacketTask task) {
            HoneyPlayer player = task.getPlayer();
            if (!ConfigurationOptions.Postpone.ENABLED
                    || player.bypassServerThreadTracking()) {
                return false;
            }

            if (player.postponedPacketsForTick > 0) {
                return true;
            }

            Array<Object> pipelineData = new Array<>();
            Callback postponed = new Callback();
            for (PostponeValidation validation : task.getPlayer().postponedChecks) {
                try {
                    if (validation.test(task.getPacket(), task.getPlayer(), pipelineData, postponed)) {
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

            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_14_4)
                    && !(otherTask instanceof FutureTask)) {
                if (otherTask instanceof Callable) {
                    otherTask = new FutureTask<>((Callable<?>) otherTask);
                } else {
                    Runnable otherTaskRunnable = (Runnable) otherTask;
                    otherTask = new FutureTask<>((Callable<? extends Object>) () -> {
                        otherTaskRunnable.run();
                        return null;
                    });
                }
            } else if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_14_4)
                    && !tickTaskClass.isInstance(otherTask)) {
                otherTask = newTickTask((Runnable) otherTask, tickAccessor.get());
            }

            return (Runnable) otherTask;
        }

        private void handleTimings() {
            // evaluate the difference in time
            long deltaTime = recentDeltaTime != -1 ? recentDeltaTime : getDeltaTimeExact(recentPacket);
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13_1)
                    && isPositionPacket(recentPacket)) {
                deltaTime = Math.min(deltaTime, ConfigurationOptions.Postpone.MAX_POSITION_TIME_REGISTER * Timespan.MILLIS_TO_NANO_MULTIPLIER);
            }
            registerTime(recentPacket.getPlayer(), deltaTime);

            recentDeltaTime = -1;
            recentPacket = null;
        }

        private void tickLoopExecutionDone() {
            currentPollIndex = 0;
            recentPollStart = -1;
            submitQueue = submitQueue.compact(collector);
            collector.clear();

            HoneyPlugin.get().getLogger().registerQueueExecutionStop();

            if (Thread.interrupted()) {
                HoneyPlugin.get().getLogger().getLogger().info("Server thread was interrupted by packet. Clearing interrupted flag.");
            }

            doneTicks = tickAccessor.get();
            doneExecutionTicks++;
            executing = false;
        }
    }

    @Override
    public List<HoneyPlayer> getTrackersOrdered() {
        List<HoneyPlayer> trackersList = new ArrayList<>(getTrackers());
        trackersList.removeIf(player -> getPlayerRate(player, Timespan.FIVE_SECOND, false) == 0);
        trackersList.sort((o1, o2) -> Double.compare(getTimeToAverageWithoutQuotient(o1, Timespan.FIVE_SECOND, true), getTimeToAverageWithoutQuotient(o2, Timespan.FIVE_SECOND, true)));

        return trackersList;
    }
}
