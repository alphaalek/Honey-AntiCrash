package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DuplexChannelHandler extends ChannelDuplexHandler {

    private volatile Player player;
    private volatile boolean hasStartedInjectingTask;

    private final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();

    public DuplexChannelHandler(Player player) {
        this.player = player;
    }

    public DuplexChannelHandler() {
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void startDecodeInjectorTask(Channel channel) {
        hasStartedInjectingTask = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player != null && !player.isOnline()) {
                    cancel();
                    return;
                }
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get("decompress") == null) return;

                CommonChannelInjector.injectDecoder(channel, player);
                cancel();
            }
        }.runTaskTimerAsynchronously(HoneyPlugin.get(), 0L, 5L);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        // decoder inject for late injection
        if (player != null && !hasStartedInjectingTask) {
            startDecodeInjectorTask(ctx.channel());
        }
        PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), null);
            return;
        }
        if (packetContainer.isCancelled()) {
            if (HoneyPlugin.get().getLoggerService().isDebug()) {
                HoneyPlugin.get().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().getName());
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
        // decoder inject for early injection
        if (packet.getClass().getSimpleName().equals("PacketLoginOutSetCompression")) {
            startDecodeInjectorTask(ctx.channel());
        }

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
