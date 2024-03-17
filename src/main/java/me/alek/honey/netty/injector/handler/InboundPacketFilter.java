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
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> implements UpdatableChannel {

    private static final List<Thread> nettyThreads = new ArrayList<>();
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

    public InboundPacketFilter copy() {
        InboundPacketFilter copy = new InboundPacketFilter(channel);
        copy.hasBeenRelocated = true;
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

            if (player.nettyThreadID == -1) {
                Thread currentThread = Thread.currentThread();
                for (int i = 0; i < nettyThreads.size(); i++) {

                    if (nettyThreads.get(i) == currentThread)
                        if (player.nettyThreadID == -1)
                            player.nettyThreadID = i;
                        else
                            nettyThreads.remove(i);
                }
                if (player.nettyThreadID == -1) {
                    nettyThreads.add(currentThread);
                    player.nettyThreadID = nettyThreads.size() - 1;
                }
            }

            earlyPacketInitializer.set(packetContainer = new InternalPacketContainer<>(player, ctx.channel()));
            packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_DECODE);
        } else earlyPacketInitializer.set(null);

        packetProcessor.callDecode(player, byteBuf);

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
                Bukkit.getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> player.player.kickPlayer(ex.getMessage()));
            }
        }
    }
}
