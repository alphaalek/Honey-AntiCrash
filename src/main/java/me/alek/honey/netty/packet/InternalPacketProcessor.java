package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.processor.PacketProcessor;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class InternalPacketProcessor implements PacketProcessor {

    private final Array<EventBus<?>> eventBusses = new Array<>();
    private final Array<Array<PacketExecutor<?>>> packetExecutors = new Array<>();
    private final Array<Callback> postActions = new Array<>();

    private static class EventBus<WP extends SourcedWrappedPacket<WP>> {

        private class StateBus {
            private final EnumMap<ListenerPriority, List<PacketListener<WP>>> priorityMap = new EnumMap<>(ListenerPriority.class);
            private final LinkedList<ListenerPriority> pipeline = new LinkedList<>();
        }

        private final EnumMap<PacketContainer.PipelineState, StateBus> stateBusses = new EnumMap<>(PacketContainer.PipelineState.class);

        {
            for (PacketContainer.PipelineState state : PacketContainer.PipelineState.values()) {
                this.stateBusses.put(state, new StateBus());
            }
        }

        private StateBus getStateBus(PacketContainer.PipelineState state) {
            return stateBusses.get(state);
        }

        @SuppressWarnings("unchecked")
        public void callObjectPacketProcess(HoneyPlayer player, PacketContainer<?> packet) {
            callPipeline(PacketContainer.PipelineState.OBJECT, listener -> {
                try {
                    if (packet.getType().getBound().equals(PacketBound.SERVER)) {
                        listener.onObjectPacketReceive(player, (PacketContainer<WP>) packet);
                    } else {
                        listener.onObjectPacketSend(player, (PacketContainer<WP>) packet);
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error occurred in packet task: " + packet.getPlayerName() + ", " + packet.getType() + ", " + packet.getState(), ex);

                    packet.cancel();
                }
            });
        }

        @SuppressWarnings("unchecked")
        public void callBytePacketProcess(HoneyPlayer player, PacketContainer.PipelineState state, PacketContainer<?> packet) {
            if (state == PacketContainer.PipelineState.OBJECT) {
                throw new IllegalArgumentException("Object state");
            }

            callPipeline(state, listener -> {
                try {
                    listener.onBytePacketReceive(player, (PacketContainer<WP>) packet);
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error occurred in packet task: " + packet.getPlayerName() + ", " + packet.getType() + ", " + packet.getState(), ex);

                    packet.cancel();
                }
            });
        }

        public void callPipeline(PacketContainer.PipelineState state, Consumer<PacketListener<WP>> consumer) {
            StateBus bus = getStateBus(state);
            for (ListenerPriority priority : bus.pipeline) {
                for (PacketListener<WP> listener : bus.priorityMap.get(priority)) {
                    consumer.accept(listener);
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void addListener(PacketListener<?> listener, PacketContainer.PipelineState state, ListenerPriority priority) {
            StateBus bus = getStateBus(state);
            synchronized (bus.priorityMap) {
                bus.priorityMap.computeIfAbsent(priority, __ -> new ArrayList<>())
                        .add((PacketListener<WP>) listener);
                addToPipeline(state, priority);
            }
        }

        public boolean removeListener(PacketContainer.PipelineState state, PacketListener<?> listener) {
            StateBus bus = getStateBus(state);
            synchronized (bus.priorityMap) {
                synchronized (bus.pipeline) {
                    for (ListenerPriority priority : new ArrayList<>(bus.pipeline)) {
                        List<PacketListener<WP>> listeners = bus.priorityMap.get(priority);
                        Iterator<PacketListener<WP>> listenerIterator = listeners.iterator();
                        while (listenerIterator.hasNext()) {
                            PacketListener<WP> busListener = listenerIterator.next();
                            if (busListener != listener) {
                                continue;
                            }

                            listenerIterator.remove();

                            if (listeners.isEmpty()) {
                                bus.priorityMap.remove(priority);
                                removeFromPipeline(state, priority);
                            }

                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public boolean removeFromPipeline(PacketContainer.PipelineState state, ListenerPriority priority) {
            StateBus bus = getStateBus(state);
            synchronized (bus.pipeline) {
                return bus.pipeline.removeIf(busPriority -> busPriority == priority);
            }
        }

        public void addToPipeline(PacketContainer.PipelineState state, ListenerPriority priority) {
            StateBus bus = getStateBus(state);
            synchronized (bus.pipeline) {
                int indexAtInsert = -1;
                int index = 0;

                for (ListenerPriority listenerPriority : bus.pipeline) {
                    if (priority == listenerPriority) {
                        return;
                    }
                    if (listenerPriority.ordinal() > priority.ordinal()) {
                        indexAtInsert = index;
                        break;
                    }
                    index++;
                }

                if (indexAtInsert == -1) {
                    bus.pipeline.addLast(priority);
                } else {
                    bus.pipeline.add(indexAtInsert, priority);
                }
            }
        }
    }

    @Override
    public void addPacketExecutor(PacketType type, PacketExecutor<?> packetExecutor) {
        this.packetExecutors.get(type, Array::new)
                .add(packetExecutor);
    }

    @Override
    public <WP extends SourcedWrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packet) {
        if (PacketTypes.isUnknown(packet.getType())
                || !this.packetExecutors.has(packet.getType())) {
            return null;
        }

        Array<PacketExecutor<?>> executors = this.packetExecutors.get(packet.getType());
        if (executors.isEmpty()) {
            return null;
        }

        for (PacketExecutor<?> executorImpl : executors) {
            @SuppressWarnings("unchecked")
            PacketExecutor<WP> executor = (PacketExecutor<WP>) executorImpl;

            if (executor.test(packet)) {
                long start = System.nanoTime();
                Runnable runnable = executor.getTask(packet);
                long end = System.nanoTime();

                if (runnable != null) {
                    HoneyPlugin.get().getExecutionQueueProxy().registerTime(packet.getPlayer(), end - start);
                    return runnable;
                }
            }
        }
        return null;
    }

    private void addListener(Consumer<EventBus<?>> addToEventBus, List<PacketType> types) {
        for (PacketType type : types) {
            if (type instanceof RangedPacketType
                    && !((RangedPacketType) type).exists()) {
                continue;
            }

            addToEventBus.accept(eventBusses.get(type, EventBus::new));
        }
    }

    @Override
    public synchronized void addListener(PacketListener<?> listener, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types) {
        addListener(bus -> bus.addListener(listener, state, priority), types);
    }

    @Override
    public void removeListener(PacketListener<?> listener, PacketContainer.PipelineState state, PacketType... types) {
        removeListener(listener, state, Arrays.asList(types));
    }

    @Override
    public void removeListener(PacketListener<?> listener, PacketContainer.PipelineState state, List<PacketType> types) {
        for (PacketType type : types) {
            if (!this.eventBusses.has(type)) {
                continue;
            }

            this.eventBusses.get(type).removeListener(state, listener);

            // check if there are no listeners for this type left
            if (Arrays.stream(PacketContainer.PipelineState.values())
                    .allMatch(s -> this.eventBusses.get(type).getStateBus(s).priorityMap.isEmpty())) {
                this.eventBusses.remove(type);
            }
        }
    }

    @Override
    public <WP extends SourcedWrappedPacket<WP>> boolean isRegistered(PacketListener<WP> listener, PacketContainer.PipelineState state) {
        for (EventBus<?> bus : this.eventBusses) {
            if (bus != null && existsInBus(bus, state, listener)) {
                return true;
            }
        }

        return false;
    }

    private <WP extends SourcedWrappedPacket<WP>, WP2 extends SourcedWrappedPacket<WP2>> boolean existsInBus(EventBus<WP2> bus, PacketContainer.PipelineState state, PacketListener<WP> listener) {
        for (List<PacketListener<WP2>> packetListeners : bus.getStateBus(state).priorityMap.values()) {
            if (packetListeners.stream().anyMatch(a -> a.equals(listener))) {
                return true;
            }
        }
        return false;
    }

    private void call(PacketType type, Consumer<EventBus<?>> callToEventBus) {
        if (PacketTypes.isUnknown(type)
                || !this.eventBusses.has(type)) {
            return;
        }

        callToEventBus.accept(this.eventBusses.get(type));
    }

    @Override
    public void callObjectPacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet) {
        call(packet.getType(), bus -> bus.callObjectPacketProcess(player, packet));
    }

    @Override
    public void callInitialBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet) {
        call(packet.getType(), bus -> bus.callBytePacketProcess(player, PacketContainer.PipelineState.INITIAL_BYTE, packet));
    }

    @Override
    public void callFinalBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet) {
        call(packet.getType(), bus -> bus.callBytePacketProcess(player, PacketContainer.PipelineState.FINAL_BYTE, packet));
    }

    private <WP extends SourcedWrappedPacket<WP>> PacketContainer<WP> handleObjectCommon(HoneyPlayer player, PacketBound bound, Object objectPacket, PacketContainer<WP> earlyInitializedPacket) {
        Optional<PacketType> typeFromClass = PacketTypes.getPacketType(objectPacket.getClass(), player.packetHandler.getProtocolState(bound));
        if (!typeFromClass.isPresent()) {
            return null;
        }
        PacketType type = typeFromClass.get();
        if (PacketTypes.isUnknown(type)) {
            return null;
        }

        PacketContainer<WP> packet = null;
        try {
            if (earlyInitializedPacket != null) {
                earlyInitializedPacket.callPostMinecraftDecode(objectPacket, type);
                packet = earlyInitializedPacket;
            } else {
                packet = new InternalPacketContainer<>(objectPacket, player, type, null);
            }

            if (packet.getState().equals(PacketState.PLAY) && !packet.getPlayer().isBukkitReady()) {
                // shouldn't happen
                return packet;
            }

            callObjectPacketProcessListeners(player, packet);
        } catch (Exception ex) {
            if (packet != null) {
                packet.cancel();
            }
        }

        return packet;
    }

    @Override
    public <WP extends SourcedWrappedPacket<WP>> PacketContainer<WP> readObjectPacket(HoneyPlayer player, Object packet, PacketContainer<WP> earlyInitializedPacket) {
        return handleObjectCommon(player, PacketBound.SERVER, packet, earlyInitializedPacket);
    }

    @Override
    public <WP extends SourcedWrappedPacket<WP>> PacketContainer<WP> writeObjectPacket(HoneyPlayer player, Object packet) {
        return handleObjectCommon(player, PacketBound.CLIENT, packet, null);
    }

    @Override
    public Callback getPostAction(PacketType type) {
        return PacketTypes.isUnknown(type) ? null : this.postActions.get(type);
    }

    @Override
    public synchronized void addPostAction(PacketType type, Callback.Action action) {
        if (!PacketTypes.isUnknown(type)) {
            Callback callback = this.postActions.get(type, Callback::new);
            callback.addAction(action);
        }
    }
}

