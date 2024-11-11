package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.lang.reflect.Method;

@ChannelHandler.Sharable
public class HoneyPacketEncodePreparation extends ChannelOutboundHandlerAdapter {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Method packetEncode;

    static {
        try {
            packetEncode = MessageToByteEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, ByteBuf.class);
            packetEncode.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet encoder environment!");
        }
    }

    private final AsyncPacketStream async;
    private final HoneyPacketHandler packetHandler;
    private final Channel channel;
    private final HoneyPlayer player;

    public HoneyPacketEncodePreparation(Channel channel, AsyncPacketStream async, HoneyPacketHandler packetHandler) {
        this.channel = channel;
        this.async = async;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AsyncPacketStream getAsync() {
        return async;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!player.isStreamActive()) {
            return;
        }

        packetHandler.runDelayedTasks(PacketBound.CLIENT);

        if (!packetHandler.isPacket(msg)) {
            if (!packetHandler.handleNonPacket(msg)) {
                promise.setSuccess();
                return;
            }
            ctx.write(msg, promise);
            return;
        }

        if (!ConfigurationOptions.AsyncDecoder.ENABLED
                || !player.isBukkitReady()
                || !PacketInterceptorResolver.hasDecoderHandler(ctx.pipeline())) {
            write0(ctx, msg, promise, false);
            return;
        }

        async.prependDecode((s) -> {
            if (player.isStreamActive()) {
                try {
                    ChannelPromise asyncPromise;
                    if (!promise.isDone()) {
                        asyncPromise = promise;
                    } else {
                        asyncPromise = new DefaultChannelPromise(ctx.channel());
                    }

                    write0(ctx, msg, asyncPromise, true);
                } catch (Exception ex) {
                    try {
                        super.exceptionCaught(ctx, ex);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private <WP extends WrappedPacket<WP>> void write0(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, boolean retain) {
        if (msg instanceof ReferenceCounted && retain) {
            ReferenceCountUtil.retain(msg);
        }

        PacketContainer<WP> packetContainer = packetProcessor.writeObjectPacket(player, msg);
        if (!packetHandler.write(packetContainer)) {
            promise.setSuccess();
            return;
        }

        ctx.write(msg, promise);

        Callback postAction = packetContainer.getPostAction();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    public void writeForcedPacket(Object value) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> writeForcedPacket(value));
            return;
        }

        try {
            ChannelHandlerContext ctx = channel.pipeline().context(this);
            if (ctx == null) {
                return;
            }

            write0(ctx, value, new DefaultChannelPromise(channel), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
