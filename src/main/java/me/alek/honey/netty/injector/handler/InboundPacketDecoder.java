package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.ThreadLocalAccessor;

import java.util.List;

public class InboundPacketDecoder extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final ThreadLocalAccessor<PacketContainer<? extends WrappedPacket<?>>> earlyPacketInitializer = new ThreadLocalAccessor<>();

    public static ThreadLocalAccessor<PacketContainer<? extends WrappedPacket<?>>> getEarlyPacketInitializer() {
        return earlyPacketInitializer;
    }

    private double recentDecodeStart;
    private double recentDeltaReceived = 0;
    private boolean isFirstRead = true;
    private boolean hasBeenRelocated;
    private HoneyPlayer player;
    private final Channel channel;

    public InboundPacketDecoder(InboundPacketDecoder decoder) {
        this.channel = decoder.channel;
        this.player = decoder.player;
        hasBeenRelocated = true;
        recentDeltaReceived = decoder.recentDeltaReceived;
        recentDecodeStart = decoder.recentDecodeStart;
        isFirstRead = false;
    }

    public InboundPacketDecoder(Channel channel) {
        this.channel = channel;
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public boolean hasReadYet() {
        return !isFirstRead;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        if (isFirstRead) {
            recentDecodeStart = System.currentTimeMillis();
            isFirstRead = false;
        }
        PacketContainer<? extends WrappedPacket<?>> packetContainer = null;
        if (player != null) {
            checkRelocation(channel);

            earlyPacketInitializer.set(packetContainer = new InternalPacketContainer<>(player, ctx.channel()));
            packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_DECODE);
        } else earlyPacketInitializer.remove();

        packetProcessor.callDecode(player, byteBuf);

        long currentTime = System.currentTimeMillis();
        recentDeltaReceived = currentTime - recentDecodeStart;
        recentDecodeStart = currentTime;

        if (packetContainer != null) packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        checkRelocation(ctx.channel()); // triggered by ViaVersion sometimes
        super.userEventTriggered(ctx, event);
    }

    public void checkRelocation(Channel channel) {
        if (!hasBeenRelocated) {

            hasBeenRelocated = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocateHandlers(channel);
        }
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
