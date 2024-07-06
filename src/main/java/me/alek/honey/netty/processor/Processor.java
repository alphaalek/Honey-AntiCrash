package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;

public class Processor<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> {

    public static class ReaderInfo<B, T> {

        private static final BiConsumer<HoneyPlayer, Throwable> DEFAULT_THROWABLE_DEBUGGER = (player, thrown) -> {
            HoneyPlugin.get().getPlugin().getLogger().log(Level.SEVERE, "Error occurred in processor for packet sent by " + player, thrown);
        };

        private final Function<B, T> reader;
        private final BiConsumer<HoneyPlayer, T> listener;
        private final BiConsumer<HoneyPlayer, Throwable> onException;

        public ReaderInfo(Function<B, T> get, BiConsumer<HoneyPlayer, T> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
            this.reader = get;
            this.listener = listener;
            this.onException = onException;
        }

        public ReaderInfo(Function<B, T> get, BiConsumer<HoneyPlayer, T> listener) {
            this(get, listener, DEFAULT_THROWABLE_DEBUGGER);
        }
    }

    private final Array<List<ReaderInfo<PacketContainer<WP>, PacketContainer<WP>>>> packetListeners = new Array<>();
    private final Array<List<ReaderInfo<PacketDataSerializer, Object[]>>> packetReaders = new Array<>();

    @SuppressWarnings("unchecked")
    public <T extends WrappedPacket<T>> void addListener(PacketType type, BiConsumer<HoneyPlayer, PacketContainer<T>> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
        List<ReaderInfo<PacketContainer<WP>, PacketContainer<WP>>> readerInfos = packetListeners.get(type.getHoneyID(), LinkedList::new);

        ReaderInfo<PacketContainer<WP>, PacketContainer<WP>> info;
        if (onException != null) {
            info = new ReaderInfo<>(
                    p -> p,
                    (BiConsumer<HoneyPlayer, PacketContainer<WP>>) (BiConsumer<?, ?>) listener,
                    onException);
        } else {
            info = new ReaderInfo<>(
                    p -> p,
                    (BiConsumer<HoneyPlayer, PacketContainer<WP>>) (BiConsumer<?, ?>) listener);
        }
        readerInfos.add(info);

        HoneyPlugin.get().getPacketLibrary().addListeners(this, ListenerPriority.HIGHEST, type);
    }

    public <T extends WrappedPacket<T>> void addListener(PacketType type, BiConsumer<HoneyPlayer, PacketContainer<T>> listener) {
        addListener(type, listener, null);
    }

    public void addReader(PacketType type, PacketReader.Buf<PacketDataSerializer> reader, BiConsumer<HoneyPlayer, Object[]> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
        List<ReaderInfo<PacketDataSerializer, Object[]>> readerInfos = packetReaders.get(type.getHoneyID(), LinkedList::new);
        ReaderInfo<PacketDataSerializer, Object[]> info;
        if (onException != null) {
            info = new ReaderInfo<>(reader::read, listener, onException);
        } else {
            info = new ReaderInfo<>(reader::read, listener);
        }
        readerInfos.add(info);

        HoneyPlugin.get().getPacketLibrary().addListeners(this, ListenerPriority.NORMAL, type);
    }

    public void addReader(PacketType type, PacketReader.Buf<PacketDataSerializer> reader, BiConsumer<HoneyPlayer, Object[]> listener) {
        addReader(type, reader, listener, null);
    }

    public void handle(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
        if (packetListeners.has(packet.getType().getHoneyID())) {

            for (ReaderInfo<PacketContainer<WP>, PacketContainer<WP>> info : packetListeners.get(packet.getType().getHoneyID())) {
                try {
                    info.listener.accept(player, packet);
                } catch (Exception ex) {
                    info.onException.accept(player, ex);
                }
            }
        }
    }

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {
        handle(player, packetContainer);
    }

    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {
        handle(player, packetContainer);
    }

    public void onPacketDecode(HoneyPlayer player, PacketType type, CancellableByteBuf buf) {
        if (packetReaders.has(type.getHoneyID())) {
            PacketDataSerializer serializer = new PacketDataSerializer(buf.buf());
            serializer.readVarInt();

            for (ReaderInfo<PacketDataSerializer, Object[]> info : packetReaders.get(type.getHoneyID())) {
                try {
                    info.listener.accept(player, info.reader.apply(serializer));
                } catch (Exception ex) {
                    info.onException.accept(player, ex);
                }
            }

            buf.buf().resetReaderIndex();
        }
    }
}
