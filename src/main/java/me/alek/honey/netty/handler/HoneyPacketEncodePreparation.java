package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncDecoder;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;

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

    private final AsyncDecoder asyncDecoder;
    private final HoneyPacketHandler packetHandler;
    private final Channel channel;
    private final HoneyPlayer player;

    public HoneyPacketEncodePreparation(Channel channel, AsyncDecoder asyncDecoder, HoneyPacketHandler packetHandler) {
        this.channel = channel;
        this.asyncDecoder = asyncDecoder;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AsyncDecoder getAsyncDecoder() {
        return asyncDecoder;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!ctx.channel().isOpen()) {
            promise.setFailure(new ClosedChannelException());
            return;
        }
        if (!packetHandler.isPacket(msg)) {
            if (!packetHandler.handleNonPacket(msg)) {
                promise.setSuccess();
                return;
            }
            ctx.write(msg, promise);
            return;
        }

        packetHandler.handleEventloopProcesses(PacketBound.CLIENT);

        if (!ConfigurationOptions.AsyncDecoder.ENABLED
                || !player.isBukkitReady()
                || !PacketInterceptorResolver.hasDecoderHandler(ctx.pipeline())) {
            write0(ctx, msg, promise, false);
            return;
        }

        asyncDecoder.prependDecode((s) -> {
            if (player.isOnlineWeak() && !player.punished) {
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

        PacketContainer<WP> packetContainer = packetProcessor.write(ctx.channel(), player, msg);
        if (!packetHandler.write(packetContainer)) {
            promise.setSuccess();
            return;
        }

        if (promise != null) {
            ctx.write(msg, promise);
        } else {
            ctx.write(msg);
        }

        Callback postAction = packetContainer.getPost();
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
