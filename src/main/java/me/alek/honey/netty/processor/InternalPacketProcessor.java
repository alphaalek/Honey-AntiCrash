package me.alek.honey.netty.processor;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.*;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class InternalPacketProcessor implements PacketProcessor {

    private final Array<EventBus<? extends WrappedPacket<?>>> eventBusses = new Array<>();
    private final Array<List<PacketExecutor<? extends WrappedPacket<?>>>> packetExecutors = new Array<>();
    private final Array<Callback> postActions = new Array<>();

    private static class EventBus<WP extends WrappedPacket<WP>> {

        private final EnumMap<ListenerPriority, List<PacketAdapter<WP>>> priorityMap = new EnumMap<>(ListenerPriority.class);
        private final LinkedList<ListenerPriority> pipeline = new LinkedList<>();

        public void callPacket(HoneyPlayer player, PacketContainer<?> packetContainer) {
            callPipeline(listener -> {
                try {
                    if (packetContainer.getType().getBound().equals(PacketBound.SERVER)) {
                        listener.onPacketReceive(player, (PacketContainer<WP>) packetContainer);
                    } else {
                        listener.onPacketSend(player, (PacketContainer<WP>) packetContainer);
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error occurred in packet task: " + packetContainer.getPlayerName() + ", " + packetContainer.getType() + ", " + packetContainer.getState(), ex);

                    packetContainer.cancel();
                }
            });
        }

        public void callDecode(HoneyPlayer player, PacketType type, CancellableByteBuf buf) {
            callPipeline(packetAdapter -> {
                try {
                    packetAdapter.onPacketDecode(player, type, buf);
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error occurred in packet task: " + Optional.ofNullable(player).map(p -> p.name).orElse("Unknown") + ", " + buf.buf().readableBytes() + " bytes", ex);

                    buf.cancel();
                }
            });
        }

        public void callPipeline(Consumer<PacketAdapter<WP>> consumer) {
            for (ListenerPriority priority : pipeline) {
                for (PacketAdapter<WP> listener : priorityMap.get(priority)) {
                    consumer.accept(listener);
                }
            }
        }

        public void addListener(PacketAdapter<?> packetAdapter, ListenerPriority priority) {
            synchronized (priorityMap) {
                if (!priorityMap.containsKey(priority)) {
                    priorityMap.put(priority, new ArrayList<>());
                }
                priorityMap.get(priority).add((PacketAdapter<WP>) packetAdapter);
                addToPipeline(priority);
            }
        }

        public boolean removeListener(PacketAdapter<?> packetAdapter) {
            synchronized (priorityMap) {
                synchronized (pipeline) {
                    for (ListenerPriority priority : new ArrayList<>(pipeline)) {
                        List<PacketAdapter<WP>> listeners = priorityMap.get(priority);
                        Iterator<PacketAdapter<WP>> listenerIterator = listeners.iterator();
                        while (listenerIterator.hasNext()) {
                            PacketAdapter<WP> listener = listenerIterator.next();
                            if (listener != packetAdapter) {
                                continue;
                            }

                            listenerIterator.remove();

                            if (listeners.isEmpty()) {
                                priorityMap.remove(priority);
                                removeFromPipeline(priority);
                            }

                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public boolean removeFromPipeline(ListenerPriority priority) {
            synchronized (pipeline) {
                return pipeline.removeIf((listenerPriority) -> listenerPriority == priority);
            }
        }

        public void addToPipeline(ListenerPriority priority) {
            synchronized (pipeline) {
                int indexAtInsert = -1;
                int index = 0;

                for (ListenerPriority listenerPriority : pipeline) {
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
                    pipeline.addLast(priority);
                } else {
                    pipeline.add(indexAtInsert, priority);
                }
            }
        }

        public boolean isEmpty() {
            return priorityMap.isEmpty();
        }
    }

    @Override
    public void addPacketExecutor(PacketType packetType, PacketExecutor<? extends WrappedPacket<?>> packetExecutor) {
        packetExecutors.get(packetType.getHoneyID(), ArrayList::new)
                .add(packetExecutor);
    }

    @Override
    public <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packetContainer) {
        if (PacketTypes.isUnknown(packetContainer.getType())
                || !packetExecutors.has(packetContainer.getType().getHoneyID())) {
            return null;
        }

        List<PacketExecutor<? extends WrappedPacket<?>>> executors = packetExecutors.get(packetContainer.getType().getHoneyID());
        if (executors.isEmpty()) {
            return null;
        }

        for (PacketExecutor<? extends WrappedPacket<?>> executor : executors) {
            @SuppressWarnings("unchecked")
            PacketExecutor<WP> castedExecutor = (PacketExecutor<WP>) executor;

            if (castedExecutor.test(packetContainer)) {
                double start = System.nanoTime();
                Runnable runnable = castedExecutor.getTask(packetContainer);
                double end = System.nanoTime();

                if (runnable != null) {
                    HoneyPlugin.get().getExecutionQueueProxy().registerTime(packetContainer.getPlayer(), end - start);
                    return runnable;
                }
            }
        }
        return null;
    }

    private void addListener(Consumer<EventBus<? extends WrappedPacket<?>>> addToEventBus, List<PacketType> types) {
        for (PacketType packetType : types) {
            if (packetType instanceof RangedPacketType
                    && !NetProtocol.matchProtocol((RangedPacketType) packetType)) {
                continue;
            }

            addToEventBus.accept(eventBusses.get(packetType.getHoneyID(), EventBus::new));
        }
    }

    @Override
    public synchronized void addListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, ListenerPriority priority, List<PacketType> types) {
        addListener(bus -> bus.addListener(packetAdapter, priority), types);
    }

    @Override
    public void addListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, ListenerPriority priority, PacketType... packetTypes) {
        addListener(packetAdapter, priority, Arrays.asList(packetTypes));
    }

    @Override
    public void removeListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, PacketType... packetTypes) {
        removeListener(packetAdapter, Arrays.asList(packetTypes));
    }

    @Override
    public void removeListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, List<PacketType> packetTypes) {
        for (PacketType packetType : packetTypes) {
            if (!eventBusses.has(packetType.getHoneyID())) continue;

            eventBusses.get(packetType.getHoneyID()).removeListener(packetAdapter);

            if (eventBusses.get(packetType.getHoneyID()).isEmpty()) {
                eventBusses.remove(packetType.getHoneyID());
            }
        }
    }

    @Override
    public <WP extends WrappedPacket<WP>> boolean isRegistered(PacketAdapter<WP> check) {
        for (EventBus<?> bus : eventBusses) {
            if (bus != null && existsInBus(bus, check)) {
                return true;
            }
        }
        return false;
    }

    private <WP extends WrappedPacket<WP>, WP2 extends WrappedPacket<WP2>> boolean existsInBus(EventBus<WP2> bus, PacketAdapter<WP> adapter) {
        for (List<PacketAdapter<WP2>> packetAdapters : bus.priorityMap.values()) {
            if (packetAdapters.stream().anyMatch(a -> a.equals(adapter))) {
                return true;
            }
        }
        return false;
    }

    private void call(PacketType type, Consumer<EventBus<? extends WrappedPacket<?>>> callToEventBus) {
        int id;
        if (PacketTypes.isUnknown(type)
                || !eventBusses.has(id = type.getHoneyID())) {
            return;
        }

        callToEventBus.accept(eventBusses.get(id));
    }

    @Override
    public void callListeners(HoneyPlayer player, PacketContainer<?> packetContainer) {
        call(packetContainer.getType(), bus -> bus.callPacket(player, packetContainer));
    }

    @Override
    public void callDecode(HoneyPlayer player, PacketType type, CancellableByteBuf buf) {
        call(type, bus -> bus.callDecode(player, type, buf));
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initializePacketContainer(PacketContainer<WP> packetContainer,
                                                                                         Object packet, HoneyPlayer player, Channel channel,
                                                                                         Callback postAction, PacketType type) {
        if (packetContainer != null) {
            packetContainer.postDecode(packet, postAction, type);
            return packetContainer;
        }

        return new InternalPacketContainer<>(packet, player, channel, postAction, type);
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> handleCommon(HoneyPlayer player, PacketBound bound, Channel channel,
                                                                            Object packet, PacketContainer<WP> earlyInitializedPacket) {
        PacketType type = null;
        if (bound.equals(PacketBound.SERVER)) {
            type = earlyInitializedPacket.getType();
        }
        if (type == null) {
            Optional<PacketType> optionalType = PacketTypes.getPacketType(packet.getClass(), player.packetHandler.getProtocolState(bound));
            if (!optionalType.isPresent()) {
                return null;
            }
            type = optionalType.get();
        }

        Callback postAction = getPostAction(type);
        PacketContainer<WP> packetContainer = initializePacketContainer(
                earlyInitializedPacket, packet, player, channel, postAction, type);

        if (PacketTypes.isUnknown(type)
                || packetContainer.getState().equals(PacketState.PLAY)
                        && !packetContainer.getPlayer().isBukkitReady()) {
            return packetContainer;
        }

        try {
            callListeners(player, packetContainer);
        } catch (Exception ex) {
            packetContainer.cancel();
        }

        return packetContainer;
    }

    @Override
    public <WP extends WrappedPacket<WP>> PacketContainer<WP> read(Channel channel, HoneyPlayer player, Object packet, PacketContainer<WP> earlyInitializedPacket) {
        return handleCommon(player, PacketBound.SERVER, channel, packet, earlyInitializedPacket);
    }

    @Override
    public <WP extends WrappedPacket<WP>> PacketContainer<WP> write(Channel channel, HoneyPlayer player, Object packet) {
        return handleCommon(player, PacketBound.CLIENT, channel, packet, null);
    }

    @SuppressWarnings("unchecked")
    public Callback getPostAction(PacketType type) {
        return PacketTypes.isUnknown(type) ? null : postActions.get(type.getHoneyID());
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void addPostAction(PacketType packetType, Callback.Action callback) {
        if (PacketTypes.isUnknown(packetType)) {
            return;
        }

        Callback action = postActions.get(packetType.getHoneyID(), Callback::new);
        action.addAction(callback);
    }
}

