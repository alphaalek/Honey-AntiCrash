package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

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

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initPacketContainer(Channel channel, Object msg, PacketBound bound) {
        switch (bound) {
            case SERVER:
                PacketContainer<WP> packetContainer = new InternalPacketContainer<>(null, channel);
                return packetProcessor.read(channel, null, msg, packetContainer);
            case CLIENT:
                return packetProcessor.write(channel, null, msg);
        }

        throw new IllegalStateException();
    }

    private <WP extends WrappedPacket<WP>> void sendThroughPipeline(Object msg, PacketContainer<WP> packetContainer,
                                                                    Function<PacketContainer<WP>, Boolean> handling,
                                                                    Runnable sending) {
        if (msg instanceof ByteBuf) {
            sending.run();
            return;
        }

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
        Bukkit.getLogger().info("Read " + msg);
        PacketContainer<? extends WrappedPacket<?>> packetContainer = initPacketContainer(ctx.channel(), msg, PacketBound.SERVER);
        sendThroughPipeline(msg, packetContainer, packetHandler::read, () -> ctx.fireChannelRead(msg));
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Bukkit.getLogger().info("Write " + msg);
        PacketContainer<? extends WrappedPacket<?>> packetContainer = initPacketContainer(ctx.channel(), msg, PacketBound.CLIENT);
        sendThroughPipeline(msg, packetContainer, packetHandler::write, () -> ctx.write(msg, promise));
    }
}
