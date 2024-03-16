package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.utility.ThreadLocalAccessor;

import java.util.List;

public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final ThreadLocalAccessor<PacketContainer<? extends WrappedPacket<?>>> earlyPacketInitializer = new ThreadLocalAccessor<>();

    public static PacketContainer<? extends WrappedPacket<?>> getEarlyInitializedPacket() {
        return earlyPacketInitializer.get();
    }

    private double recentDecodeStart;
    private double recentDeltaReceived = 0;
    private boolean hasBeenRelocated;
    private HoneyPlayer player;
    private final Channel channel;

    public InboundPacketFilter(Channel channel) {
        this.channel = channel;
    }

    public InboundPacketFilter copy() {
        InboundPacketFilter copy = new InboundPacketFilter(channel);
        copy.hasBeenRelocated = true;
        copy.recentDeltaReceived = recentDeltaReceived;
        copy.recentDecodeStart = recentDecodeStart;
        copy.player = player;
        return copy;
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
            checkRelocation(channel);

            earlyPacketInitializer.set(packetContainer = new InternalPacketContainer<>(player, ctx.channel()));
            packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_DECODE);
        } else earlyPacketInitializer.set(null);

        packetProcessor.callDecode(player, byteBuf);

        long currentTime = System.currentTimeMillis();
        recentDeltaReceived = currentTime - recentDecodeStart;
        recentDecodeStart = currentTime;

        if (packetContainer != null) {
            packetContainer.getEventloopStateTimings().stopTimings();
        }

        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        checkRelocation(ctx.channel()); // triggered by ViaVersion
        super.userEventTriggered(ctx, event);
    }

    public void checkRelocation(Channel channel) {
        if (!hasBeenRelocated && player != null) {
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocateHandlers(channel, player);
                hasBeenRelocated = true;
            } catch (MissingHandlerException ex) {
                player.player.kickPlayer(ex.getMessage());
            }
        }
    }

    public double getRecentDeltaReceived() {
        return recentDeltaReceived;
    }

    public double getRecentDecodeStart() {
        return recentDecodeStart;
    }

}
