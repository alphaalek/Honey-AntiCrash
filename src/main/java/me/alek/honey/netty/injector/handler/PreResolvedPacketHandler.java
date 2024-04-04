package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.utility.CallbackAction;

import java.util.function.Function;

public class PreResolvedPacketHandler extends ChannelDuplexHandler {

    private final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private final HoneyPacketHandler packetHandler;

    public PreResolvedPacketHandler(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initPacketContainer(Channel channel, Object msg) {
        PacketContainer<WP> packetContainer = new InternalPacketContainer<>(null, channel);

        return (PacketContainer<WP>) packetProcessor.read(channel, null, msg, packetContainer);
    }

    private <WP extends WrappedPacket<WP>> void sendThroughPipeline(Channel channel, Object msg, Function<PacketContainer<WP>, Boolean> handling, Runnable sending) {
        if (msg instanceof ByteBuf) {
            sending.run();
            return;
        }

        PacketContainer<WP> packetContainer = initPacketContainer(channel, msg);
        if (!handling.apply(packetContainer)) {
            return;
        }

        sending.run();

        CallbackAction<PacketContainer<WP>> postAction = packetContainer.getPost();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        sendThroughPipeline(ctx.channel(), msg, packetHandler::read, () -> ctx.fireChannelRead(msg));
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        sendThroughPipeline(ctx.channel(), msg, (packet) -> packetHandler.write((PacketContainer<? extends WrappedPacket<?>>) packet), () -> ctx.write(msg, promise));
    }
}
