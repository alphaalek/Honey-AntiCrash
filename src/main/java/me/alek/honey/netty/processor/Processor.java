package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.VarInt;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class Processor<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> {

    public static class BoundContext<WP extends WrappedPacket<WP>> {

        private final Array<List<BiConsumer<HoneyPlayer, PacketContainer<WP>>>> packetListeners = new Array<>();

    }

    public static class ReaderInfo {

        private static final BiConsumer<HoneyPlayer, Throwable> DEFAULT_THROWABLE_DEBUGGER = (player, thrown) -> {
            HoneyPlugin.get().getPlugin().getLogger().log(Level.SEVERE, "Error occurred in processor for packet sent by " + player, thrown);
        };

        private final PacketReader.Buf<PacketDataSerializer> reader;
        private final BiConsumer<HoneyPlayer, Object[]> listener;
        private final BiConsumer<HoneyPlayer, Throwable> onException;

        public ReaderInfo(PacketReader.Buf<PacketDataSerializer> reader, BiConsumer<HoneyPlayer, Object[]> listener, BiConsumer<HoneyPlayer, Throwable> onException) {
            this.reader = reader;
            this.listener = listener;
            this.onException = onException;
        }

        public ReaderInfo(PacketReader.Buf<PacketDataSerializer> reader, BiConsumer<HoneyPlayer, Object[]> listener) {
            this(reader, listener, DEFAULT_THROWABLE_DEBUGGER);
        }
    }

    private final Array<List<ReaderInfo>> packetReaders = new Array<>();
    private final BoundContext<? extends WrappedPacket<?>>[] contexts = (BoundContext<? extends WrappedPacket<?>>[]) new BoundContext[]{new BoundContext<>(), new BoundContext<>()};

    @SuppressWarnings("unchecked")
    public BoundContext<WP> getContext(PacketBound bound) {
        return (BoundContext<WP>) contexts[bound.ordinal()];
    }

    @SuppressWarnings("unchecked")
    public <T extends WrappedPacket<T>> void addListener(PacketType type, BiConsumer<HoneyPlayer, PacketContainer<T>> listener) {
        List<BiConsumer<HoneyPlayer, PacketContainer<T>>> listeners = (List<BiConsumer<HoneyPlayer, PacketContainer<T>>>) (List<?>)
                getContext(type.getBound()).packetListeners.get(type.getHoneyID(), LinkedList::new);
        listeners.add(listener);

        HoneyPlugin.get().getPacketLibrary().addListeners(this, ListenerPriority.HIGHEST, type);
    }

    public void addReader(PacketType type, PacketReader.Buf<PacketDataSerializer> reader, BiConsumer<HoneyPlayer, Object[]> listener) {
        List<ReaderInfo> readerInfos = packetReaders.get(type.getHoneyID(), LinkedList::new);
        readerInfos.add(new ReaderInfo(reader, listener));

        HoneyPlugin.get().getPacketLibrary().addListeners(this, ListenerPriority.NORMAL, type);
    }

    public void handle(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {
        if (getContext(packet.getType().getBound()).packetListeners.has(packet.getType().getHoneyID())) {

            for (BiConsumer<HoneyPlayer, PacketContainer<WP>> listener :
                    getContext(packet.getType().getBound()).packetListeners.get(packet.getType().getHoneyID())) {
                listener.accept(player, packet);
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

            for (ReaderInfo info : packetReaders.get(type.getHoneyID())) {
                try {
                    Object[] data = info.reader.read(serializer);
                    info.listener.accept(player, data);
                } catch (Exception ex) {
                    info.onException.accept(player, ex);
                }
            }

            buf.buf().resetReaderIndex();
        }
    }
}
