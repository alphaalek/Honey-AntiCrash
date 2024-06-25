package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.List;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    public static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> MARK = new ThreadLocal<>();

    public static PacketContainer<? extends WrappedPacket<?>> getEarlyInitializedPacket() {
        PacketContainer<? extends WrappedPacket<?>> packet = MARK.get();
        MARK.remove();
        return packet;
    }

    private boolean hasCalledChannelInactive;
    private HoneyPlayer player;

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

        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(ctx.channel()).setProtocolState(PacketBound.SERVER, PacketState.HANDSHAKE);
        } catch (MissingHandlerException ex) {
            ctx.close();
        }
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

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        PacketContainer<? extends WrappedPacket<?>> packetContainer = new InternalPacketContainer<>(player, ctx.channel());
        MARK.set(packetContainer);
        packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_HANDLE);

        CancellableByteBuf cancellableByteBuf = new CancellableByteBuf(player, byteBuf);
        packetProcessor.callDecode(player, cancellableByteBuf);
        packetContainer.getEventloopStateTimings().stopTimings();

        if (!cancellableByteBuf.isCancelled()) {
            list.add(byteBuf.readBytes(byteBuf.readableBytes()));
        } else {
            list.add(Unpooled.buffer());
        }

        packetContainer.getEventloopStateTimings().startTimings(EventloopState.IN_BETWEEN);
    }
}
