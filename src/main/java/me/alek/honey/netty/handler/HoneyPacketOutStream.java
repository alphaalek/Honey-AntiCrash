package me.alek.honey.netty.handler;

import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyPacketOutStream extends ChannelOutboundHandlerAdapter {

    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;

    public HoneyPacketOutStream(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!this.player.isStreamActive()) {
            return;
        }
        if (!this.packetHandler.isPacket(msg)) {
            ctx.write(msg, promise);
            return;
        }

        write0(ctx, msg, promise);
    }

    private <WP extends SourcedWrappedPacket<WP>> void write0(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        // fix login fallback state if error happens during player login (can be both during handshake, configuration or login)
        if (!this.packetHandler.getProtocolState(PacketBound.CLIENT).equals(PacketState.PLAY)
                && msg.getClass().equals(PacketTypes.Login.Server.DISCONNECT.getNmsClass())) {
            this.packetHandler.setProtocolState(PacketBound.CLIENT, PacketState.LOGIN);
        }

        Optional<PacketType> typeFromClass = PacketTypes.getPacketType(msg.getClass(), this.packetHandler.getProtocolState(PacketBound.CLIENT));
        if (!typeFromClass.isPresent()) {
            promise.setSuccess();
            return;
        }

        PacketContainer<WP> packet = new InternalPacketContainer<>(msg, this.player, typeFromClass.get(), null);
        try {
            HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callObjectPacketProcessListeners(this.player, packet);
        } catch (Exception ex) {
            if (HoneyPlugin.get().getLogger().isDebug()) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Error occurred while handling packet " + packet.getType() + " sent by " + this.player.name, ex);
            }
            packet.cancel();
        }

        if (!this.packetHandler.write(packet)) {
            promise.setSuccess();
            return;
        }

        this.packetHandler.getProtocolSwapHandler().swapProtocolOutbound(packet);
        ctx.write(msg, promise);
        this.packetHandler.getProtocolSwapHandler().swapProtocolOutboundPostPipeline(packet);

        Callback postAction = packet.getPostAction();
        if (postAction != null) {
            postAction.call(packet);
        }
    }

    public void writePacket(Object value) {
        if (!this.player.channel.eventLoop().inEventLoop()) {
            this.player.channel.eventLoop().submit(() -> writePacket(value));
            return;
        }

        try {
            ChannelHandlerContext ctx = this.player.channel.pipeline().context(this);
            if (ctx == null) {
                return;
            }

            write0(ctx, value, new DefaultChannelPromise(this.player.channel));
        } catch (Exception ex) {
            HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to write packet " + value, ex);
        }
    }
}
