package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerChannelDuplexHandler extends ChannelDuplexHandler {

    private volatile Player player;
    private volatile boolean checkHandshake;

    private final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();

    public PlayerChannelDuplexHandler(Player player) {
        this.player = player;
        checkHandshake = false;
    }

    public PlayerChannelDuplexHandler() {
        checkHandshake = true;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), packetContainer);
            return;
        }
        if (packetContainer.isCancelled()) {
            Bukkit.getLogger().info("cancelled");
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
            promise.addListener((f) -> {
                packetContainer.getPost().run();
            });
        }
        super.write(ctx, packetContainer.getHandle(), promise);
        packetProcessor.postWrite();
    }
}
