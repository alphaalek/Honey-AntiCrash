package me.alek.honey.netty.processor;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InternalPacketProcessor implements PacketProcessor {

    private static final Class<?> PACKET_CLASS = NMS.getPacketClass();

    private final ConcurrentHashMap<Object, PacketState> LOOKUP_PACKET_STATES = new ConcurrentHashMap<>();
    private final Map<Class<?>, ListModifier<?>> PACKET_ADAPTERS = new HashMap<>();
    private final ListModifier<?> DECODE_LIST_MODIFIER = new ListModifier<>();
    private final Map<PacketTypeEnum, Runnable> POST_ACTION_MAP = new HashMap<>();

    private static class ListModifier<WP extends WrappedPacket<WP>> {

        private final EnumMap<ListenerPriority, List<SyncPacketAdapter<WP>>> priorityMap = new EnumMap<>(ListenerPriority.class);
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
                    error(player, packetContainer.getType(), packetContainer);
                }
            });
        }

        public void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
            callPipeline((packetAdapter) -> {
                packetAdapter.onByteBufDecode(player, byteBuf);
            });
        }

        public void callPipeline(Consumer<SyncPacketAdapter<WP>> consumer) {
            synchronized (pipeline) {
                synchronized (priorityMap) {
                    for (ListenerPriority priority : pipeline) {
                        for (SyncPacketAdapter<WP> listener : priorityMap.get(priority)) {
                            consumer.accept(listener);
                        }
                    }
                }
            }
        }

        public void cancel(HoneyPlayer player, PacketContainer<?> packetContainer, boolean isRead) {
            final PacketBound bound = (isRead) ? PacketBound.CLIENT : PacketBound.SERVER;

            callPipeline((packetAdapter) -> {
                packetAdapter.onPacketCancel(player, (PacketContainer<WP>) packetContainer, bound);
            });
        }

        public void error(HoneyPlayer player, PacketTypeEnum packetType, PacketContainer<?> packetContainer) {
            callPipeline((packetAdapter) -> {
                packetAdapter.onPacketError(player, packetType, (PacketContainer<WP>) packetContainer);
            });
        }

        public void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority) {
            synchronized (priorityMap) {
                if (!priorityMap.containsKey(priority)) {
                    priorityMap.put(priority, new ArrayList<>());
                }
                priorityMap.get(priority).add((SyncPacketAdapter<WP>) packetAdapter);
                addToPipeline(priority);
            }
        }

        public boolean removeListener(SyncPacketAdapter<?> packetAdapter) {
            synchronized (priorityMap) {
                synchronized (pipeline) {
                    for (ListenerPriority priority : new ArrayList<>(pipeline)) {
                        final List<SyncPacketAdapter<WP>> listeners = priorityMap.get(priority);

                        Iterator<SyncPacketAdapter<WP>> listenerIterator = listeners.iterator();
                        while (listenerIterator.hasNext()) {
                            SyncPacketAdapter<WP> listener = listenerIterator.next();
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

    public void addDecodeListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority) {
        DECODE_LIST_MODIFIER.addListener(packetAdapter, priority);
    }

    public synchronized void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetTypes) {
        for (PacketTypeEnum packetType : packetTypes) {
            if (packetType instanceof RangedPacketTypeEnum) {
                if (!NetProtocol.matchProtocol((RangedPacketTypeEnum) packetType)) {
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
            if (compareListModifier(listModifier, check)) {
                return true;
            }
        }
        return false;
    }

    private <WP extends WrappedPacket<WP>, WP2 extends WrappedPacket<WP2>> boolean compareListModifier(ListModifier<WP2> listModifier, SyncPacketAdapter<WP> check) {
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
        if (!PACKET_ADAPTERS.containsKey(clazz)) return false;

        return !PACKET_ADAPTERS.get(clazz).isEmpty();
    }

    public void callListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead) {
         if (!hasListeners(clazz)) return;

        PACKET_ADAPTERS.get(clazz).callPacket(player, packetContainer, isRead);
    }

    public void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
        DECODE_LIST_MODIFIER.callDecode(player, byteBuf);
    }

    public void errorListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer) {
        if (!hasListeners(clazz)) return;

        PACKET_ADAPTERS.get(clazz).error(player, PacketType.getPacketType(clazz), packetContainer);
    }

    public void cancelListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead) {
        if (!hasListeners(clazz)) return;

        PACKET_ADAPTERS.get(clazz).cancel(player, packetContainer, isRead);
    }

    @Override
    public void setPostAction(PacketTypeEnum packetType, Runnable postAction) {
        POST_ACTION_MAP.put(packetType, postAction);
    }

    private void registerToBundle(HoneyPlayer player, PacketTypeEnum packetType, PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetType.getBound() == PacketBound.CLIENT && player != null && packetContainer.getPlayer() != null) {
            player.packetBundleSaver.add(packetContainer);
        }
    }

    private PacketContainer<? extends WrappedPacket<?>> initializePacketContainer(PacketContainer<? extends WrappedPacket<?>> packetContainer,
                                                                                  Object packet, HoneyPlayer player, Channel channel, Runnable postAction, PacketTypeEnum type, long readStart) {
        /* removed from packet processor - now handled in InboundPacketDecoder

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

    private PacketContainer<? extends WrappedPacket<?>> handleCommon(HoneyPlayer player, Channel channel,
                                                                     Object packet, PacketBound packetBound,
                                                                     PacketContainer<? extends WrappedPacket<?>> earlyInitializedPacket, long readStart) {
        // initialize the packet container
        final PacketTypeEnum packetType = PacketType.getPacketType(packet.getClass());
        final PacketContainer<? extends WrappedPacket<?>> packetContainer = initializePacketContainer(
                earlyInitializedPacket, packet, player, channel, POST_ACTION_MAP.get(packetType), packetType, readStart);


        // check if this packet type has any listeners which can handle this packet.
        // if there are not any, we will just create a simple container and avoid using performance on unnecessary packets
        if (!hasListener(packetType)) {
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
        Bukkit.getLogger().info("Call " + packetContainer.getPacket() + " " + packetContainer.getType() + " " + packet);
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
        if (!LOOKUP_PACKET_STATES.containsKey(packetClass)) {
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

            LOOKUP_PACKET_STATES.put(packetClass, packetState);
        }
        else {
            packetState = LOOKUP_PACKET_STATES.get(packetClass);
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
