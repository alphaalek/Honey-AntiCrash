package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.utility.CallbackAction;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

@ChannelHandler.Sharable
public class OutboundPacketEncoder extends ChannelOutboundHandlerAdapter {

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

    public OutboundPacketEncoder(Channel channel, AsyncDecoder asyncDecoder, HoneyPacketHandler packetHandler) {
        this.channel = channel;
        this.asyncDecoder = asyncDecoder;
        this.packetHandler = packetHandler;
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AsyncDecoder getAsyncDecoder() {
        return asyncDecoder;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!ConfigurationOptions.AsyncDecoder.ENABLED) {
            write0(ctx, msg, promise, false);
            return;
        }

        asyncDecoder.encode((s) -> {
            if (asyncDecoder.getPlayer() == null || asyncDecoder.getPlayer().isOnlineDirect()) {
                try {
                    write0(ctx, msg, new DefaultChannelPromise(ctx.channel()), true);
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
        if (msg instanceof ByteBuf) {
            ctx.write(msg, promise);
            return;
        }

        PacketContainer<WP> packetContainer = (PacketContainer<WP>) packetProcessor.write(ctx.channel(), asyncDecoder.getPlayer(), msg);
        if (!packetHandler.write(packetContainer)) {
            return;
        }

        if (retain) {
            ReferenceCountUtil.retain(msg);
        }

        CallbackAction<PacketContainer<WP>> post = packetContainer.getPost();
        if (post != null) {
            promise.addListener((d) -> post.call(packetContainer));
        }

        ctx.write(msg, promise);
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
