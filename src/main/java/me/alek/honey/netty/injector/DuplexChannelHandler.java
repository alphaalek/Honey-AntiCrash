package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public class DuplexChannelHandler extends ChannelDuplexHandler implements AbstractPipelineProxy {

    private volatile Player player;
    private final Channel channel;
    private final AtomicBoolean hasStartedInjectingTask = new AtomicBoolean(false);
    private InboundPacketDecoder inboundPacketDecoder;

    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();

    public DuplexChannelHandler(Channel channel, Player player) {
        this.channel = channel;
        this.player = player;
    }

    public DuplexChannelHandler(Channel channel) {
        this.channel = channel;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void startDecodeInjectorTask(Channel channel) {
        hasStartedInjectingTask.set(true);
        new Thread(() -> {

            while (true) {
                if (player != null && !player.isOnline()) {
                    break;
                }
                ChannelPipeline pipeline = channel.pipeline();

                // waiting for minecraft to add decompress or decode channel handler
                if (pipeline.get("decompress") == null && pipeline.get("decoder") == null) break;
                if (player == null) break;

                CommonChannelInjector.injectDecoder(channel, player);
                return;
            }
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        // decoder inject for late injection
        if (player != null && !hasStartedInjectingTask.get()) {
            startDecodeInjectorTask(this.channel);
        }

        if (inboundPacketDecoder == null && player != null) {

            AbstractPipelineProxy proxy = HoneyPlugin.get().getPacketLibrary().getInjector().getDecoder(NMSUtils.getChannel(player));
            if (proxy instanceof InboundPacketDecoder) {
                inboundPacketDecoder = (InboundPacketDecoder) proxy;
            }
        }
        // register the handle time between the last handled packet and this one
        if (inboundPacketDecoder != null) {
            HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(player, inboundPacketDecoder.getRecentDeltaDecode());
        }

        PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), null);
            return;
        }
        // check if the packet should be cancelled
        if (packetContainer.isCancelled()) {
            if (HoneyPlugin.get().getLoggerService().isDebug()) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().getName());
            }
            return;
        }
        super.channelRead(ctx, packetContainer.getHandle());
        packetProcessor.postRead();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (packet instanceof ByteBuf) {
            super.write(ctx, packet, promise);
            return;
        }
        PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.write(ctx.channel(), player, packet);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), packetContainer);
            return;
        }
        if (packetContainer.isCancelled()) {
            return;
        }
        if (packetContainer.getPost() != null) {
            promise.addListener((f) -> packetContainer.getPost().run());
        }
        super.write(ctx, packetContainer.getHandle(), promise);
        packetProcessor.postWrite();
    }
}
