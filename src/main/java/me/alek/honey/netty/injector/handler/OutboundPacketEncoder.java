package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.Reflection;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

public class OutboundPacketEncoder extends MessageToByteEncoder<Object> {

    private static final Method packetEncode;
    private static final Class<?> packetClass;
    private static boolean encoding;
    private static boolean checkEncoding;

    static {
        try {
            packetEncode = MessageToByteEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, ByteBuf.class);
            packetEncode.setAccessible(true);

            packetClass = NMSUtils.getPacketClass();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet encoder environment!");
        }
    }

    private final AsyncDecoder asyncDecoder;
    private final Channel channel;
    private final MessageToByteEncoder<Object> encoder;
    private volatile boolean forced;

    public OutboundPacketEncoder(Channel channel, AsyncDecoder asyncDecoder) {
        this.channel = channel;
        this.asyncDecoder = asyncDecoder;
        encoder = (MessageToByteEncoder<Object>) channel.pipeline().get("encoder");
    }

    @Override
    public synchronized void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (forced || !ConfigurationOptions.AsyncDecoder.ENABLED) {
            write0(ctx, msg, promise, false);
            return;
        }

        asyncDecoder.encode((s) -> {
            if (asyncDecoder.getPlayer().isOnline()) {
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
        if (!checkEncoding) {
            checkEncoding = true;
            encoding = packetClass.isInstance(msg);
        }
        if (retain) ReferenceCountUtil.retain(msg);

        if (encoding)
            super.write(ctx, msg, promise);
        else
            ctx.write(msg, promise);
    }

    public synchronized void writeForcedPacket(Object value) {
        forced = true;
        channel.write(value);
        forced = false;
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
