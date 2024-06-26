package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.ChainSupplier;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Processor<WP extends WrappedPacket<WP>, C extends ProcessorContext> extends PacketAdapter<WP> {

    private final Array<List<BiConsumer<HoneyPlayer, PacketContainer<? extends WrappedPacket<?>>>>> listeners = new Array<>();
    private final Array<C> contexts = new Array<>();
    private final Supplier<C> contextFactory;

    public Processor(Supplier<C> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @SuppressWarnings("unchecked")
    public <T extends WrappedPacket<T>> void add(PacketType type, BiConsumer<HoneyPlayer, PacketContainer<T>> listener) {
        List<BiConsumer<HoneyPlayer, PacketContainer<T>>> listeners = (List<BiConsumer<HoneyPlayer, PacketContainer<T>>>) (List<?>) this.listeners.get(type.getHoneyID(), LinkedList::new);
        listeners.add(listener);

        HoneyPlugin.get().getPacketLibrary().addListener(this, ListenerPriority.LOWEST, type);
    }

    public synchronized C getContext(HoneyPlayer player) {
        return contexts.get(player.registryID,
                ChainSupplier.wrap(contextFactory)
                        .thenDo(() -> player.leaveCallback.addAction(p -> contexts.remove(player.registryID))));
    }

    public void handle(HoneyPlayer player, PacketContainer<? extends WrappedPacket<?>> packet) throws Exception {
        for (BiConsumer<HoneyPlayer, PacketContainer<? extends WrappedPacket<?>>> listener : listeners.get(packet.getType().getHoneyID())) {
            listener.accept(player, packet);
        }
    }

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {
        handle(player, packetContainer);
    }

    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {
        handle(player, packetContainer);
    }
}
