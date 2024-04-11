package me.alek.honey.netty.injector.handler;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.List;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> earlyPacketInitializer = new ThreadLocal<>();

    public static PacketContainer<? extends WrappedPacket<?>> getEarlyInitializedPacket() {
        PacketContainer<? extends WrappedPacket<?>> packet = earlyPacketInitializer.get();
        earlyPacketInitializer.remove();
        return packet;
    }

    private boolean hasBeenRelocated;
    private boolean hasCalledChannelInactive;
    private HoneyPlayer player;
    private final Channel channel;

    public InboundPacketFilter(Channel channel) {
        this.channel = channel;
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();

        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannelProxy(ctx.channel());
        } catch (MissingHandlerException ex) {
            ctx.close();
        }
        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();

        if (!hasCalledChannelInactive) {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().decrementTaskSupplierCount(ctx.channel());
        }
        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(ctx.channel());
        hasCalledChannelInactive = true;
    }

    public void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        if (player != null) {
            checkRelocation();
        }

        PacketContainer<? extends WrappedPacket<?>> packetContainer = new InternalPacketContainer<>(player, ctx.channel());
        earlyPacketInitializer.set(packetContainer);
        packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_DECODE);

        CancellableByteBufImpl cancellableByteBuf = new CancellableByteBufImpl(player, byteBuf);
        packetProcessor.callDecode(player, cancellableByteBuf);

        packetContainer.getEventloopStateTimings().stopTimings();

        if (!cancellableByteBuf.isCancelled()) {
            list.add(byteBuf.readBytes(byteBuf.readableBytes()));
        } else {
            list.add(Unpooled.buffer());
        }
    }

    public void checkRelocation() {
        if (!hasBeenRelocated) {
            hasBeenRelocated = true;
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocateHandlers(player, channel);
            } catch (MissingHandlerException ex) {
                hasBeenRelocated = false;
            }
        }
    }
}
