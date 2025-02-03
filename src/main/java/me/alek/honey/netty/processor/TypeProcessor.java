package me.alek.honey.netty.processor;

import io.netty.handler.codec.CodecException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.Array;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

public class TypeProcessor<WP extends SourcedWrappedPacket<WP>> extends PacketListenerAdapter.Common<WP> {

    public static class ReaderInfo<T> {

        public static final BiConsumer<HoneyPlayer, Throwable> DEFAULT_THROWABLE_DEBUGGER = (player, thrown) -> {
            throw new CodecException("Error occurred in processor for packet sent by " + player, thrown);
        };

        private final PacketContainer.PipelineState state;
        private final BiConsumer<HoneyPlayer, T> listener;
        private final BiConsumer<HoneyPlayer, Throwable> onException;

        public ReaderInfo(PacketContainer.PipelineState state, BiConsumer<HoneyPlayer, T> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
            this.state = state;
            this.listener = listener;
            this.onException = onException;
        }

        public ReaderInfo(PacketContainer.PipelineState state, BiConsumer<HoneyPlayer, T> listener) {
            this(state, listener, DEFAULT_THROWABLE_DEBUGGER);
        }
    }

    private final Array<List<ReaderInfo<PacketContainer<WP>>>> packetListeners = new Array<>();

    @SuppressWarnings("unchecked")
    public <T extends SourcedWrappedPacket<T>> void addListener(PacketType type, PacketContainer.PipelineState state, BiConsumer<HoneyPlayer, PacketContainer<T>> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
        List<ReaderInfo<PacketContainer<WP>>> readerInfos = this.packetListeners.get(type, LinkedList::new);

        ReaderInfo<PacketContainer<WP>> info;
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

        HoneyPlugin.get().getPacketLibrary().addListener(this, state, ListenerPriority.HIGHEST, type);
    }

    public <T extends SourcedWrappedPacket<T>> void addListener(PacketType type, PacketContainer.PipelineState state, BiConsumer<HoneyPlayer, PacketContainer<T>> listener) {
        addListener(type, state, listener, null);
    }

    @Override
    public void onPacketStream(HoneyPlayer player, PacketBound bound, PacketContainer<WP> packet) {
        if (this.packetListeners.has(packet.getType())) {
            for (ReaderInfo<PacketContainer<WP>> info : this.packetListeners.get(packet.getType())) {
                if (info.state.equals(packet.getPipelineState())) {
                    try {
                        info.listener.accept(player, packet);
                    } catch (Exception ex) {
                        info.onException.accept(player, ex);
                    }
                }
            }
        }
    }
}
