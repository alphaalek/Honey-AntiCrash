package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.ThreadLocalAccessor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InboundPacketDecoder extends ByteToMessageDecoder implements AbstractPipelineProxy {

    private static final Set<UUID> registeredPacketDecoders = new HashSet<>();
    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();
    private static final ThreadLocalAccessor<PacketContainer<? extends WrappedPacket<?>>> earlyPacketInitializer = new ThreadLocalAccessor<>();

    public static void unregisterPlayer(Player player) {
        synchronized (registeredPacketDecoders) {
            registeredPacketDecoders.remove(player.getUniqueId());
        }
    }

    public static boolean isDecoderRegistered(Player player) {
        return registeredPacketDecoders.contains(player.getUniqueId());
    }

    public static ThreadLocalAccessor<PacketContainer<? extends WrappedPacket<?>>> getEarlyPacketInitializer() {
        return earlyPacketInitializer;
    }

    private double recentDecodeStart = System.currentTimeMillis();
    private double recentDeltaReceived = 0;
    private boolean hasRead;
    private Player player;

    public InboundPacketDecoder(Player player) {
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        PacketContainer<? extends WrappedPacket<?>> packetContainer = null;
        if (player != null) {
            if (!hasRead) {
                hasRead = true;
                synchronized (registeredPacketDecoders) {
                    registeredPacketDecoders.add(player.getUniqueId());
                }
            }
            earlyPacketInitializer.set(packetContainer = new InternalPacketContainer<>(player, ctx.channel()));
            packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_DECODE);
        } else earlyPacketInitializer.remove();

        packetProcessor.callDecode(player, byteBuf);

        double currentTime = System.currentTimeMillis();;
        recentDeltaReceived = currentTime - recentDecodeStart;
        recentDecodeStart = currentTime;

        if (packetContainer != null) packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    public double getRecentDeltaReceived() {
        return recentDeltaReceived;
    }

    public double getRecentDeltaDecode() {
        return System.currentTimeMillis() - recentDecodeStart;
    }

    public double getRecentStartDecode() {
        return recentDecodeStart;
    }
}
