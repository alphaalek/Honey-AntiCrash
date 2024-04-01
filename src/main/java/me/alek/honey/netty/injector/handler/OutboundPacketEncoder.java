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
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

@ChannelHandler.Sharable
public class OutboundPacketEncoder extends MessageToByteEncoder<Object> {

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
    private final MessageToByteEncoder<Object> encoder;

    public OutboundPacketEncoder(Channel channel, AsyncDecoder asyncDecoder, HoneyPacketHandler packetHandler) {
        this.channel = channel;
        this.asyncDecoder = asyncDecoder;
        this.packetHandler = packetHandler;
        encoder = (MessageToByteEncoder<Object>) channel.pipeline().get("encoder");
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
            if (asyncDecoder.getPlayer() == null || asyncDecoder.getPlayer().isOnline()) {
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

    private void write0(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, boolean retain) throws Exception {
        if (ctx == null) return;

        if (asyncDecoder.getPlayer() != null) {
            PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.write(ctx.channel(), asyncDecoder.getPlayer(), msg);

            if (!packetHandler.write0(packetContainer, promise)) {
                return;
            }
        }

        if (retain) {
            ReferenceCountUtil.retain(msg);
        }

        ctx.write(msg, promise);
    }

    public void writeForcedPacket(Object value) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> writeForcedPacket(value));
            return;
        }

        try {
            write0(channel.pipeline().context(this), value, new DefaultChannelPromise(channel), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object value, ByteBuf byteBuf) throws Exception {
        try {
            packetEncode.invoke(encoder, ctx, value, byteBuf);
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
    }
}
