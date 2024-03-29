package me.alek.honey.netty.processor;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.utility.CallbackAction;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.utility.Array;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InternalPacketProcessor implements PacketProcessor {

    private static final Class<?> PACKET_CLASS = NMS.getPacketClass();

    private final ListModifier<?> DECODE_LIST_MODIFIER = new ListModifier<>();
    private final ConcurrentHashMap<Object, PacketState> lookupPacketStates = new ConcurrentHashMap<>();
    private final Array<ListModifier<? extends WrappedPacket<?>>> packetAdapters = new Array<>();
    private final Array<List<PacketExecutor<? extends WrappedPacket<?>>>> packetExecutors = new Array<>();
    private final Array<CallbackAction<PacketContainer<? extends WrappedPacket<?>>>> postActions = new Array<>();

    private static class ListModifier<WP extends WrappedPacket<WP>> {

        private final EnumMap<ListenerPriority, List<PacketAdapter<WP>>> priorityMap = new EnumMap<>(ListenerPriority.class);
        private final LinkedList<ListenerPriority> pipeline = new LinkedList<>();

        public void callPacket(HoneyPlayer player, PacketContainer<?> packetContainer, boolean isRead) {
            callPipeline((packetAdapter) -> {
                try {
                    if (isRead) {
                        packetAdapter.onPacketReceive(player, (PacketContainer<WP>) packetContainer);
                    }
                    else {
                        packetAdapter.onPacketSend(player, (PacketContainer<WP>) packetContainer);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        public void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
            callPipeline((packetAdapter) -> {
                packetAdapter.onByteBufDecode(player, byteBuf);
            });
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
                        return; // already exist in pipeline
                    }
                    if (listenerPriority.ordinal() > priority.ordinal()) {
                        indexAtInsert = index;
                        break;
                    }
                    index++;
                }
                if (indexAtInsert == -1) {
                    pipeline.addLast(priority);
                }
                else {
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
        packetExecutors.get(packetType.getPacketId(), ArrayList::new)
                .add(packetExecutor);
    }

    public <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packetContainer) {
        int id = packetContainer.getType().getPacketId();
        if (id == -1 || !packetExecutors.has(id)) return null;

        List<PacketExecutor<? extends WrappedPacket<?>>> executors = packetExecutors.get(id);
        if (executors.isEmpty()) return null;

        for (PacketExecutor<? extends WrappedPacket<?>> executor : executors) {
            if (((PacketExecutor<WP>)executor).accepts(packetContainer)) {
                double start = System.nanoTime();
                Runnable runnable = ((PacketExecutor<WP>)executor).getTask(packetContainer);
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
        DECODE_LIST_MODIFIER.addListener(packetAdapter, priority);
    }

    public synchronized void addListener(PacketAdapter<? extends WrappedPacket<?>> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            if (packetType instanceof RangedPacketTypeEnum) {
                if (!NetProtocol.matchProtocol((RangedPacketTypeEnum) packetType)) {
                    continue;
                }
            }
            packetAdapters.get(packetType.getPacketId(), ListModifier::new)
                    .addListener(packetAdapter, priority);
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
            if (!packetAdapters.has(packetType.getPacketId())) continue;

            packetAdapters.get(packetType.getPacketId()).removeListener(packetAdapter);

            if (packetAdapters.get(packetType.getPacketId()).isEmpty()) {
                packetAdapters.remove(packetType.getPacketId());
            }
        }
    }

    @Override
    public <WP extends WrappedPacket<WP>> boolean isRegistered(PacketAdapter<WP> check) {
        for (ListModifier<?> listModifier : packetAdapters) {
            if (listModifier != null && existsInListModifier(listModifier, check)) {
                return true;
            }
        }
        return false;
    }

    private <WP extends WrappedPacket<WP>, WP2 extends WrappedPacket<WP2>> boolean existsInListModifier(ListModifier<WP2> listModifier, PacketAdapter<WP> check) {
        for (List<PacketAdapter<WP2>> packetAdapters : listModifier.priorityMap.values()) {
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

            if (!packetAdapters.has(packetType.getPacketId())) {
                return false;
            }
        }
        return true;
    }

    public void callListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead) {
        int id = packetContainer.getType().getPacketId();
        if (id == -1 || !packetAdapters.has(id)) return;

        packetAdapters.get(packetContainer.getType().getPacketId()).callPacket(player, packetContainer, isRead);
    }

    public void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
        DECODE_LIST_MODIFIER.callDecode(player, byteBuf);
    }

    @Override
    public void setPostAction(PacketTypeEnum packetType, Runnable postAction) {
        if (packetType.getPacketId() == -1) return;

        postActions.get(packetType.getPacketId(), () -> new CallbackAction<>());
    }

    private void registerToBundle(HoneyPlayer player, PacketTypeEnum packetType, PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetType.getBound() == PacketBound.CLIENT && player != null && packetContainer.getPlayer() != null) {
            player.packetBundleSaver.add(packetContainer);
        }
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initializePacketContainer(PacketContainer<WP> packetContainer,
                                                                                  Object packet, HoneyPlayer player, Channel channel,
                                                                                  CallbackAction<PacketContainer<WP>> postAction, PacketTypeEnum type, long readStart) {
        /* removed from packet processor - now handled in me/alek/honey/netty/injector/handler/InboundPacketDecoder

        if (player != null) {
            if (player.decoder == null) {
                player.decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                if (player.decoder == null)
                    return new InternalPacketContainer<>(packet, player, channel, postAction, type);
            }
            if (player.decoder.earlyInitializedPacketQueue.isEmpty())
                return new InternalPacketContainer<>(packet, player, channel, postAction, type);

            PacketContainer<? extends WrappedPacket<?>> earlyInitializedPacket;
            while ((earlyInitializedPacket = player.decoder.earlyInitializedPacketQueue.poll()) != null && !packet.equals(earlyInitializedPacket.getHandle())) {

            }

            if (earlyInitializedPacket != null && earlyInitializedPacket.getHandle().equals(packet)) {
                earlyInitializedPacket.init(packet, postAction, type);

                HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(player, (double) earlyInitializedPacket.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_DECODE));
                return earlyInitializedPacket;
            }
        }
        */
        if (packetContainer != null) {
            packetContainer.init(packet, postAction, type);

            return packetContainer;
        }

        return new InternalPacketContainer<>(packet, player, channel, postAction, type);
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> handleCommon(HoneyPlayer player, Channel channel,
                                                                     Object packet, PacketBound packetBound,
                                                                     PacketContainer<WP> earlyInitializedPacket, long readStart) {
        // initialize the packet container
        final PacketTypeEnum packetType = PacketType.getPacketType(packet.getClass());
        final CallbackAction<PacketContainer<WP>> postAction = packetType.getPacketId() != -1 ? (CallbackAction<PacketContainer<WP>>) (CallbackAction<?>) postActions.get(packetType.getPacketId()) : null;
        final PacketContainer<WP> packetContainer = initializePacketContainer(
                earlyInitializedPacket, packet, player, channel, postAction, packetType, readStart);

        // check if this packet type has any listeners which can handle this packet.
        // if there are not any, we will just create a simple container and avoid using performance on unnecessary packets
        if (packetType == PacketType.UNKNOWN || !hasListener(packetType)) {
            return packetContainer;
        }

        if (packetContainer.getState() == PacketState.PLAY && packetContainer.getPlayer() == null) {
            return packetContainer;
        }

        if (packetContainer.getPacket() == null) {
            registerToBundle(player, packetType, packetContainer);
            return packetContainer;
        }

        if (packetType != PacketType.Play.Client.KEEP_ALIVE) {
            registerToBundle(player, packetType, packetContainer);
        }

        packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_HANDLE);
        callListeners(player, packet.getClass(), packetContainer, packetBound == PacketBound.CLIENT);
        packetContainer.getEventloopStateTimings().stopTimings();

        return packetContainer;
    }

    @Override
    public PacketContainer<? extends WrappedPacket<?>> read(Channel channel, HoneyPlayer player, Object packet, long readStart, PacketContainer<? extends WrappedPacket<?>> earlyInitializedPacket) {
        return handleCommon(player, channel, packet, PacketBound.CLIENT, earlyInitializedPacket, readStart);
    }

    @Override
    public PacketContainer<? extends WrappedPacket<?>> read(Channel channel, HoneyPlayer player, Object packet, PacketContainer<? extends WrappedPacket<?>> earlyInitializedPacket) {
        return read(channel, player, packet, 0L, earlyInitializedPacket);
    }

    @Override
    public PacketContainer<? extends WrappedPacket<?>> write(Channel channel, HoneyPlayer player, Object packet) {
        return handleCommon(player, channel, packet, PacketBound.SERVER, null, 0L);
    }


    public PacketState getPacketState(HoneyPlayer player, Class<?> packetClass) {
        if (packetClass == null) return null;
        if (player != null) return PacketState.PLAY;

        PacketState packetState;
        if (!lookupPacketStates.containsKey(packetClass)) {
            String packetName = packetClass.getSimpleName();

            if (packetName.startsWith("PacketHandshaking")) {
                packetState = PacketState.HANDSHAKE;
            }
            else if (packetName.startsWith("PacketLogin")) {
                packetState = PacketState.LOGIN;
            }
            else if (packetName.startsWith("PacketStatus")) {
                packetState = PacketState.STATUS;
            }
            else if (packetName.startsWith("PacketPlay") || packetName.startsWith("Clientbound") || packetName.startsWith("Serverbound")) {
                packetState = PacketState.PLAY;
            }
            else {
                if (PACKET_CLASS.isAssignableFrom(packetClass.getSuperclass())) {
                    return getPacketState(null, packetClass.getSuperclass());
                }
                packetState = PacketState.UNKNOWN;
            }

            lookupPacketStates.put(packetClass, packetState);
        }
        else {
            packetState = lookupPacketStates.get(packetClass);
        }

        return packetState;
    }
}
