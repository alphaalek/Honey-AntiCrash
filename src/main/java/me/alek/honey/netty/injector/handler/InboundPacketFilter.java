package me.alek.honey.netty.injector.handler;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.utility.Array;
import org.bukkit.Bukkit;

import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> earlyPacketInitializer = new ThreadLocal<>();

    public static PacketContainer<? extends WrappedPacket<?>> getEarlyInitializedPacket() {
        return earlyPacketInitializer.get();
    }

    private boolean hasBeenRelocated;
    private HoneyPlayer player;
    private final Channel channel;
    private final HoneyPacketHandler packetHandler;

    public InboundPacketFilter(Channel channel, HoneyPacketHandler packetHandler) {
        this.channel = channel;
        this.packetHandler = packetHandler;
    }

    public InboundPacketFilter copy() {
        InboundPacketFilter copy = new InboundPacketFilter(channel, packetHandler);
        copy.hasBeenRelocated = true;
        copy.player = player;
        return copy;
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        PacketContainer<? extends WrappedPacket<?>> packetContainer = null;
        if (player != null) {
            checkRelocation();

            if (player.updateHandlersLater &&
                    PacketInterceptorResolver.isDecoderPositionResolved() && PacketInterceptorResolver.isEncoderPositionResolved()) {
                try {
                    HoneyPlugin.get().getPacketLibrary().getPipelineInjector().updatePlayer(player);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                player.updateHandlersLater = false;
            }

            earlyPacketInitializer.set(packetContainer = new InternalPacketContainer<>(player, ctx.channel()));
            packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_DECODE);
        } else earlyPacketInitializer.remove();

        CancellableByteBufImpl cancellableByteBuf = new CancellableByteBufImpl(player, byteBuf);
        packetProcessor.callDecode(player, cancellableByteBuf);

        if (packetContainer != null) {
            packetContainer.getEventloopStateTimings().stopTimings();
        }

        if (!cancellableByteBuf.isCancelled())
            list.add(byteBuf.readBytes(byteBuf.readableBytes()));
        else
            list.add(Unpooled.buffer());

    }

    public void checkRelocation() {
        if (!hasBeenRelocated) {
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocateHandlers(channel, packetHandler);

                hasBeenRelocated = true;
            } catch (MissingHandlerException ex) {
                ex.printStackTrace();
                if (player != null)
                    Bukkit.getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> player.player.kickPlayer(ex.getMessage()));
                else
                    channel.close();
            }
        }
    }
}
