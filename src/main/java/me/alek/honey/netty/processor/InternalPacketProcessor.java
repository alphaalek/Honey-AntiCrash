package me.alek.honey.netty.processor;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.*;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Consumer;

public class InternalPacketProcessor implements PacketProcessor {

    private final EventBus<?> decodeEventBus = new EventBus<>();
    private final Array<EventBus<? extends WrappedPacket<?>>> packetAdapters = new Array<>();
    private final Array<List<PacketExecutor<? extends WrappedPacket<?>>>> packetExecutors = new Array<>();
    private final Array<CallbackAction<PacketContainer<? extends WrappedPacket<?>>>> postActions = new Array<>();

    private static class EventBus<WP extends WrappedPacket<WP>> {

        private final EnumMap<ListenerPriority, List<PacketAdapter<WP>>> priorityMap = new EnumMap<>(ListenerPriority.class);
        private final LinkedList<ListenerPriority> pipeline = new LinkedList<>();

        public void callPacket(HoneyPlayer player, PacketContainer<?> packetContainer) {
            callPipeline(packetAdapter -> {
                try {
                    if (packetContainer.getType().getBound() == PacketBound.SERVER) {
                        packetAdapter.onPacketReceive(player, (PacketContainer<WP>) packetContainer);
                    } else {
                        packetAdapter.onPacketSend(player, (PacketContainer<WP>) packetContainer);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        public void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
            callPipeline(packetAdapter -> packetAdapter.onByteBufDecode(player, byteBuf));
        }

        public void callPipeline(Consumer<PacketAdapter<WP>> consumer) {
            synchronized (pipeline) {
                synchronized (priorityMap) {
                    for (ListenerPriority priority : pipeline) {
                        for (PacketAdapter<WP> listener : priorityMap.get(priority)) {
                            consumer.accept(listener);
                        }
                    }
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
                        final List<PacketAdapter<WP>> listeners = priorityMap.get(priority);

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

        public void clear() {
            priorityMap.clear();
        }
    }

    public void addPacketExecutor(PacketExecutor<? extends WrappedPacket<?>> packetExecutor, PacketTypeEnum packetType) {
        packetExecutors.get(packetType.getHoneyID(), ArrayList::new)
                .add(packetExecutor);
    }

    public <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packetContainer) {
        if (PacketType.isUnknown(packetContainer.getType()) || !packetExecutors.has(packetContainer.getType().getHoneyID())) {
            return null;
        }

        List<PacketExecutor<? extends WrappedPacket<?>>> executors = packetExecutors.get(packetContainer.getType().getHoneyID());
        if (executors.isEmpty()) {
            return null;
        }

        for (PacketExecutor<? extends WrappedPacket<?>> executor : executors) {
            @SuppressWarnings("unchecked")
            PacketExecutor<WP> castedExecutor = (PacketExecutor<WP>) executor;

            if (castedExecutor.accepts(packetContainer)) {
                double start = System.nanoTime();
                Runnable runnable = castedExecutor.getTask(packetContainer);
                double end = System.nanoTime();

                if (runnable != null) {
                    HoneyPlugin.get().getServerThreadTracker().registerTime(packetContainer.getPlayer(), end - start);
                    return runnable;
                }
            }
        }
        return null;
    }

    public void addDecodeListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, ListenerPriority priority) {
        decodeEventBus.addListener(packetAdapter, priority);
    }

    public synchronized void addListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            if (packetType instanceof RangedPacketTypeEnum
                    && !NetProtocol.matchProtocol((RangedPacketTypeEnum) packetType)) {
                continue;
            }
            packetAdapters.get(packetType.getHoneyID(), EventBus::new).addListener(packetAdapter, priority);
        }
    }

    public void addListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, ListenerPriority priority, PacketTypeEnum... packetTypes) {
        addListener(packetAdapter, priority, Arrays.asList(packetTypes));
    }

    public void removeListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, PacketTypeEnum... packetTypes) {
        removeListener(packetAdapter, Arrays.asList(packetTypes));
    }

    public void removeListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, List<PacketTypeEnum> packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            if (!packetAdapters.has(packetType.getHoneyID())) continue;

            packetAdapters.get(packetType.getHoneyID()).removeListener(packetAdapter);

            if (packetAdapters.get(packetType.getHoneyID()).isEmpty()) {
                packetAdapters.remove(packetType.getHoneyID());
            }
        }
    }

    @Override
    public <WP extends WrappedPacket<WP>> boolean isRegistered(PacketAdapter<WP> check) {
        for (EventBus<?> bus : packetAdapters) {
            if (bus != null && existsInBus(bus, check)) {
                return true;
            }
        }
        return false;
    }

    private <WP extends WrappedPacket<WP>, WP2 extends WrappedPacket<WP2>> boolean existsInBus(EventBus<WP2> bus, PacketAdapter<WP> check) {
        for (List<PacketAdapter<WP2>> packetAdapters : bus.priorityMap.values()) {
            for (PacketAdapter<WP2> packetAdapter : packetAdapters) {
                if (packetAdapter == check) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasListener(PacketTypeEnum... packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            if (!packetAdapters.has(packetType.getHoneyID())) {
                return false;
            }
        }
        return true;
    }

    public void callListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer) {
        int id;
        if (PacketType.isUnknown(packetContainer.getType())
                || !packetAdapters.has(id = packetContainer.getType().getHoneyID())) {
            return;
        }

        packetAdapters.get(id).callPacket(player, packetContainer);
    }

    public void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
        decodeEventBus.callDecode(player, byteBuf);
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initializePacketContainer(PacketContainer<WP> packetContainer,
                                                                                         Object packet, HoneyPlayer player, Channel channel,
                                                                                         CallbackAction<PacketContainer<WP>> postAction, PacketTypeEnum type) {
        if (packetContainer != null) {
            packetContainer.init(packet, postAction, type);

            return packetContainer;
        }

        return new InternalPacketContainer<>(packet, player, channel, postAction, type);
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> handleCommon(HoneyPlayer player, Channel channel,
                                                                     Object packet, PacketContainer<WP> earlyInitializedPacket) {
        // initialize the packet container
        PacketTypeEnum packetType = PacketType.getPacketType(packet.getClass());
        CallbackAction<PacketContainer<WP>> postAction = !PacketType.isUnknown(packetType)
                ? (CallbackAction<PacketContainer<WP>>) (CallbackAction<?>) postActions.get(packetType.getHoneyID()) : null;

        PacketContainer<WP> packetContainer = initializePacketContainer(
                earlyInitializedPacket, packet, player, channel, postAction, packetType);


        // check if this packet type has any listeners which can handle this packet.
        // if there are not any, we will just create a simple container and avoid using performance on unnecessary packets
        if (PacketType.isUnknown(packetType)) {
            return packetContainer;
        }

        if (packetContainer.getCommonStates().contains(PacketState.PLAY) && packetContainer.getPlayer() == null) {
            return packetContainer;
        }

        packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_HANDLE);
        try {
            callListeners(player, packet.getClass(), packetContainer);
        } catch (Exception ex) {
            packetContainer.cancel();
        }
        packetContainer.getEventloopStateTimings().stopTimings();

        return packetContainer;
    }

    @Override
    public <WP extends WrappedPacket<WP>> PacketContainer<WP> read(Channel channel, HoneyPlayer player, Object packet, PacketContainer<WP> earlyInitializedPacket) {
        return handleCommon(player, channel, packet, earlyInitializedPacket);
    }

    @Override
    public <WP extends WrappedPacket<WP>> PacketContainer<WP> write(Channel channel, HoneyPlayer player, Object packet) {
        return handleCommon(player, channel, packet, null);
    }

    @Override
    public synchronized void addPostAction(PacketTypeEnum packetType, Consumer<PacketContainer<? extends WrappedPacket<?>>> callback) {
        if (PacketType.isUnknown(packetType)) {
            return;
        }

        if (!postActions.has(packetType.getHoneyID())) {
            postActions.set(packetType.getHoneyID(), new CallbackAction<>());
        }

        CallbackAction<PacketContainer<? extends WrappedPacket<?>>> action = postActions.get(packetType.getHoneyID());
        action.addAction(callback);
    }
}
