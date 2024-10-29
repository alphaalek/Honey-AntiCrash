package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class Processor<WP extends WrappedPacket<WP>> extends PacketListenerAdapter<WP> {

    public static class ReaderInfo<B, T> {

        private static final BiConsumer<HoneyPlayer, Throwable> DEFAULT_THROWABLE_DEBUGGER = (player, thrown) -> {
            HoneyPlugin.get().getPlugin().getLogger().log(Level.SEVERE, "Error occurred in processor for packet sent by " + player, thrown);
        };

        private final PacketContainer.State state;
        private final BiConsumer<HoneyPlayer, T> listener;
        private final BiConsumer<HoneyPlayer, Throwable> onException;

        public ReaderInfo(PacketContainer.State state, BiConsumer<HoneyPlayer, T> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
            this.state = state;
            this.listener = listener;
            this.onException = onException;
        }

        public ReaderInfo(PacketContainer.State state, BiConsumer<HoneyPlayer, T> listener) {
            this(state, listener, DEFAULT_THROWABLE_DEBUGGER);
        }
    }

    private final Array<List<ReaderInfo<PacketContainer<WP>, PacketContainer<WP>>>> packetListeners = new Array<>();

    @SuppressWarnings("unchecked")
    public <T extends WrappedPacket<T>> void addListener(PacketType type, PacketContainer.State state, BiConsumer<HoneyPlayer, PacketContainer<T>> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
        List<ReaderInfo<PacketContainer<WP>, PacketContainer<WP>>> readerInfos = packetListeners.get(type, LinkedList::new);

        ReaderInfo<PacketContainer<WP>, PacketContainer<WP>> info;
        if (onException != null) {
            info = new ReaderInfo<>(
                    state,
                    (BiConsumer<HoneyPlayer, PacketContainer<WP>>) (BiConsumer<?, ?>) listener,
                    onException);
        } else {
            info = new ReaderInfo<>(
                    state,
                    (BiConsumer<HoneyPlayer, PacketContainer<WP>>) (BiConsumer<?, ?>) listener);
        }
        readerInfos.add(info);

        HoneyPlugin.get().getPacketLibrary().addListeners(this, state, ListenerPriority.HIGHEST, type);
    }

    public <T extends WrappedPacket<T>> void addListener(PacketType type, PacketContainer.State state, BiConsumer<HoneyPlayer, PacketContainer<T>> listener) {
        addListener(type, state, listener, null);
    }

    public void handle(HoneyPlayer player, PacketContainer.State state, PacketContainer<WP> packet) throws Exception {
        if (packetListeners.has(packet.getType())) {
            for (ReaderInfo<PacketContainer<WP>, PacketContainer<WP>> info : packetListeners.get(packet.getType().getHoneyID())) {
                if (info.state.equals(state)) {
                    try {
                        info.listener.accept(player, packet);
                    } catch (Exception ex) {
                        info.onException.accept(player, ex);
                    }
                }
            }
        }
    }

    public void onObjectPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
        handle(player, PacketContainer.State.OBJECT, packet);
    }

    @Override
    public void onObjectPacketSend(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
        handle(player, PacketContainer.State.OBJECT, packet);
    }

    @Override
    public void onBytePacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
        handle(player, PacketContainer.State.INITIAL_BYTE, packet);
    }
}
