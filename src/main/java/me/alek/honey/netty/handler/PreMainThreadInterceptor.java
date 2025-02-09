package me.alek.honey.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;

@ChannelHandler.Sharable
public class PreMainThreadInterceptor extends ChannelInboundHandlerAdapter {

    public static final String NAME = "honey-pre-main-thread-interceptor";

    public static final ThreadLocal<PacketContainer<?>> POST_TO_MAIN_THREAD = new ThreadLocal<>();
    private final HoneyPacketHandler packetHandler;

    public PreMainThreadInterceptor(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (this.packetHandler.isResolving(PacketBound.SERVER) || !this.packetHandler.isPacket(msg)) {
            ctx.fireChannelRead(msg);
            return;
        }

        PacketContainer<?> packet = this.packetHandler.finalMark;
        this.packetHandler.finalMark = null;
        if (packet != null && packet.getObjectWrapped().getHandle().equals(msg)) {
            POST_TO_MAIN_THREAD.set(packet);
        }

        ctx.fireChannelRead(msg);
    }
}
