package me.alek.honey.netty.processor;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.event.EventManager;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.api.event.impl.packet.*;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class InternalPacketProcessor implements PacketProcessor {

    private final ConcurrentHashMap<Object, PacketState> LOOKUP_PACKET_STATES = new ConcurrentHashMap<>();
    private final Map<Class<?>, ListModifier<?>> PACKET_ADAPTERS = new HashMap<>();
    private final Map<PacketTypeEnum, Runnable> POST_ACTION_MAP = new HashMap<>();
    private final EventManager EVENT_MANAGER;

    public InternalPacketProcessor(EventManager eventManager) {
        this.EVENT_MANAGER = eventManager;
    }

    private static class ListModifier<WP extends WrappedPacket<WP>> {

        private final EnumMap<ListenerPriority, List<SyncPacketAdapter<WP>>> priorityMap = new EnumMap<>(ListenerPriority.class);
        private final LinkedList<ListenerPriority> pipeline = new LinkedList<>();

        public void call(Player player, PacketContainer<?> packetContainer, boolean isRead) {
            callPriority((packetAdapter) -> {
                try {
                    if (isRead) {
                        if (packetContainer.getPacket() instanceof WrappedPlayInFlying) {
                            callFlying(player, packetContainer, packetAdapter);
                            return;
                        }
                        packetAdapter.onPacketReceive(player, (PacketContainer<WP>) packetContainer);
                    }
                    else {
                        packetAdapter.onPacketSend(player, (PacketContainer<WP>) packetContainer);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    error(player, packetContainer.getType(), packetContainer);
                }
            });
        }

        private <R extends WrappedPlayInFlying<R>> void callFlying(Player player, PacketContainer<?> packetContainer, SyncPacketAdapter<WP> packetAdapter) throws Exception {
            ((SyncPacketAdapter<R>)packetAdapter).onPacketReceive(player, (PacketContainer<R>) packetContainer);
        }

        public void callPriority(Consumer<SyncPacketAdapter<WP>> consumer) {
            synchronized (priorityMap) {
                synchronized (pipeline) {
                    for (ListenerPriority priority : pipeline) {
                        for (SyncPacketAdapter<WP> listener : priorityMap.get(priority)) {
                            consumer.accept(listener);
                        }
                    }
                }
            }
        }

        public void cancel(Player player, PacketContainer<?> packetContainer, boolean isRead) {
            final PacketBound bound = (isRead) ? PacketBound.CLIENT : PacketBound.SERVER;

            callPriority((packetAdapter) -> {
                packetAdapter.onPacketCancel(player, (PacketContainer<WP>) packetContainer, bound);
            });
        }

        public void error(Player player, PacketTypeEnum packetType, PacketContainer<?> packetContainer) {
            callPriority((packetAdapter) -> {
                packetAdapter.onPacketError(player, packetType, (PacketContainer<WP>) packetContainer);
            });
        }

        public void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority) {
            if (!priorityMap.containsKey(priority)) {
                priorityMap.put(priority, new ArrayList<>());
            }
            priorityMap.get(priority).add((SyncPacketAdapter<WP>) packetAdapter);
            addToPipeline(priority);
        }

        public boolean removeListener(SyncPacketAdapter<?> packetAdapter) {
            synchronized (priorityMap) {
                synchronized (pipeline) {

                    for (ListenerPriority priority : pipeline) {
                        final List<SyncPacketAdapter<WP>> listeners = priorityMap.get(priority);

                        for (SyncPacketAdapter<WP> listener : listeners) {
                            if (listener != packetAdapter) {
                                continue;
                            }
                            listeners.remove(listener);

                            if (listeners.size() == 0) {
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
                        return; // eksisterer allerede i pipeline
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

    public void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            if (packetType instanceof RangedPacketTypeEnum) {
                if (!Protocol.protocolMatch((RangedPacketTypeEnum) packetType)) {
                    continue;
                }
            }
            if (!PACKET_ADAPTERS.containsKey(packetType.getNmsClass())) {
                PACKET_ADAPTERS.put(packetType.getNmsClass(), new ListModifier<>());
            }
            PACKET_ADAPTERS.get(packetType.getNmsClass()).addListener(packetAdapter, priority);
        }
    }

    public void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, PacketTypeEnum... packetTypes) {
        addListener(packetAdapter, priority, Arrays.asList(packetTypes));
    }

    public void removeListener(SyncPacketAdapter<?> packetAdapter, PacketTypeEnum... packetTypes) {
        removeListener(packetAdapter, Arrays.asList(packetTypes));
    }

    public void removeListener(SyncPacketAdapter<?> packetAdapter, List<PacketTypeEnum> packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            PACKET_ADAPTERS.get(packetType.getNmsClass()).removeListener(packetAdapter);

            if (PACKET_ADAPTERS.get(packetType.getNmsClass()).isEmpty()) {
                PACKET_ADAPTERS.remove(packetType.getNmsClass());
            }
        }
    }

    @Override
    public <WP extends WrappedPacket<WP>> boolean isRegistered(SyncPacketAdapter<WP> check) {
        for (ListModifier<?> listModifier : PACKET_ADAPTERS.values()) {
            if (checkListModifier(listModifier, check)) {
                return true;
            }
        }
        return false;
    }

    private <WP extends WrappedPacket<WP>, WP2 extends WrappedPacket<WP2>> boolean checkListModifier(ListModifier<WP2> listModifier, SyncPacketAdapter<WP> check) {
        for (List<SyncPacketAdapter<WP2>> packetAdapters : listModifier.priorityMap.values()) {
            for (SyncPacketAdapter<WP2> packetAdapter : packetAdapters) {
                if (packetAdapter == check) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasListener(PacketTypeEnum... packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {

            if (!hasListeners(packetType.getNmsClass())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasListeners(Class<?> clazz) {
        if (!PACKET_ADAPTERS.containsKey(clazz)) {
            return false;
        }
        return !PACKET_ADAPTERS.get(clazz).isEmpty();
    }

    public void callListeners(Player player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead) {
        if (!hasListeners(clazz)) {
            return;
        }
        PACKET_ADAPTERS.get(clazz).call(player, packetContainer, isRead);
    }

    public void errorListeners(Player player, Class<?> clazz, PacketContainer<?> packetContainer) {
        if (!hasListeners(clazz)) {
            return;
        }
        PACKET_ADAPTERS.get(clazz).error(player, PacketType.getPacketType(clazz), packetContainer);
    }

    public void cancelListeners(Player player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead) {
        if (!hasListeners(clazz)) {
            return;
        }
        PACKET_ADAPTERS.get(clazz).cancel(player, packetContainer, isRead);
    }

    @Override
    public void setPostAction(PacketTypeEnum packetType, Runnable postAction) {
        POST_ACTION_MAP.put(packetType, postAction);
    }

    private PacketContainer<? extends WrappedPacket<?>> handleCommon(
            Player player,
            Channel channel,
            Object packet,
            PacketBound packetBound
    ) {
        final PacketState packetState = getPacketState(player, packet);
        if (packetState == PacketState.UNKNOWN) {
            return InternalPacketContainer.SIMPLE_CONTAINER.apply(packet);
        }
        final boolean hasAdapters = hasListener(PacketType.getPacketType(packet.getClass()));
        final boolean hasListeners = EVENT_MANAGER.hasHandlers(EVENT_MANAGER.getPacketEventFor(packetState, packetBound), true);

        if (!hasAdapters && !hasListeners) {
            return InternalPacketContainer.SIMPLE_CONTAINER.apply(packet);
        }

        final PacketTypeEnum packetType = PacketType.getPacketType(packet.getClass());
        final PacketContainer<? extends WrappedPacket<?>> packetContainer = new InternalPacketContainer<>(
                packet, player, channel, POST_ACTION_MAP.get(packetType), packetType
        );
        if (hasAdapters) {
            callListeners(player, packet.getClass(), packetContainer, packetBound == PacketBound.CLIENT);
        }
        AtomicReference<PacketEvent> packetEvent = new AtomicReference<>();
        if (hasListeners) {
            switch (packetBound) {

                case CLIENT: {
                    switch (packetState) {
                        case HANDSHAKE: {
                            processHandshakeReceiveInternal(packetContainer);
                            packetEvent.set(new PacketHandshakeReceiveEvent(player, packetContainer));
                            break;
                        }
                        case LOGIN: {
                            processLoginReceiveInternal(packetContainer);
                            packetEvent.set(new PacketLoginReceiveEvent(player, packetContainer));
                            break;
                        }
                        case STATUS: {
                            processStatusReceiveInternal(packetContainer);
                            packetEvent.set(new PacketStatusReceiveEvent(player, packetContainer));
                            break;
                        }
                        case PLAY: {
                            processPlayReceiveInternal(packetContainer);
                            packetEvent.set(new PacketPlayReceiveEvent(player, packetContainer));
                            break;
                        }
                    }
                    break;
                }
                case SERVER: {
                    switch (getPacketState(player, packet)) {
                        case LOGIN: {
                            processLoginSendInternal(packetContainer);
                            packetEvent.set(new PacketLoginSendEvent(player, packetContainer));
                            break;
                        }
                        case STATUS: {
                            processStatusSendInternal(packetContainer);
                            packetEvent.set(new PacketStatusSendEvent(player, packetContainer));
                            break;
                        }
                        case PLAY: {
                            processPlaySendInternal(packetContainer);
                            packetEvent.set(new PacketPlaySendEvent(player, packetContainer));
                            break;
                        }
                    }
                    break;
                }
            }
            final PacketEvent event = packetEvent.get();
            if (event != null) {
                PacketLibrary.get().callSyncEvent(event, true);
                if (packetContainer.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
        final PacketEvent event = packetEvent.get();
        boolean calledCancelled = false;
        if (hasAdapters) {
            if (packetContainer.isCancelled()) {
                cancelListeners(player, packet.getClass(), packetContainer, packetBound == PacketBound.CLIENT);
                calledCancelled = true;
            }
        }
        if (event != null) {
            if (event.isCancelled()) {
                packetContainer.cancel();
                if (hasAdapters && !calledCancelled) {
                    cancelListeners(player, packet.getClass(), packetContainer, packetBound == PacketBound.CLIENT);
                }
            }
        }
        return packetContainer;
    }

    @Override
    public PacketContainer<? extends WrappedPacket<?>> read(Channel channel, Player player, Object packet) {
        return handleCommon(player, channel, packet, PacketBound.CLIENT);
    }

    @Override
    public PacketContainer<? extends WrappedPacket<?>> write(Channel channel, Player player, Object packet) {
        return handleCommon(player, channel, packet, PacketBound.SERVER);
    }

    public PacketState getPacketState(Player player, Object packet) {
        if (packet == null) {
            return null;
        }
        if (player != null) {
            return PacketState.PLAY;
        }
        PacketState packetState;
        if (!LOOKUP_PACKET_STATES.containsKey(packet.getClass())) {
            String packetName = packet.getClass().getSimpleName();
            if (packetName.startsWith("PacketH")) {
                packetState = PacketState.HANDSHAKE;
            }
            else if (packetName.startsWith("PacketL")) {
                packetState = PacketState.LOGIN;
            }
            else if (packetName.startsWith("PacketS")) {
                packetState = PacketState.STATUS;
            }
            else {
                packetState = PacketState.UNKNOWN;
            }
            LOOKUP_PACKET_STATES.put(packet.getClass(), packetState);
        }
        else {
            packetState = LOOKUP_PACKET_STATES.get(packet.getClass());
        }
        return packetState;
    }

    public <WP extends WrappedPacket<WP>> void processHandshakeReceiveInternal(PacketContainer<WP> packetContainer) {
    }

    public <WP extends WrappedPacket<WP>> void processStatusReceiveInternal(PacketContainer<WP> packetContainer) {
    }

    public <WP extends WrappedPacket<WP>> void processStatusSendInternal(PacketContainer<WP> packetContainer) {
    }

    public <WP extends WrappedPacket<WP>> void processLoginReceiveInternal(PacketContainer<WP> packetContainer) {
    }

    public <WP extends WrappedPacket<WP>> void processLoginSendInternal(PacketContainer<WP> packetContainer) {
    }

    public <WP extends WrappedPacket<WP>> void processPlayReceiveInternal(PacketContainer<WP> packetContainer) {
    }

    public <WP extends WrappedPacket<WP>> void processPlaySendInternal(PacketContainer<WP> packetContainer) {
    }

    public void postRead() {
    }

    public void postWrite() {
    }
}
