package me.alek.honey.netty.injector.handler;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
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

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> earlyPacketInitializer = new ThreadLocal<>();

    public static PacketContainer<? extends WrappedPacket<?>> getEarlyInitializedPacket() {
        return earlyPacketInitializer.get();
    }

    private boolean hasBeenRelocated;
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
    public void channelInactive(ChannelHandlerContext ctx) {
        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().decrementTaskSupplierCount(ctx.channel());

        try {
            super.channelInactive(ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        PacketContainer<? extends WrappedPacket<?>> packetContainer = null;
        if (player != null) {
            checkRelocation();

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
            hasBeenRelocated = true;
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocateHandlers(player, channel);
        }
    }
}
