package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
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
    private static final Object commonPacketEncoder;

    static {
        try {
            packetEncode = MessageToByteEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, ByteBuf.class);
            packetEncode.setAccessible(true);

            commonPacketEncoder = NMSUtils.getPacketEncoderClass().getDeclaredConstructor(NMSUtils.getEnumProtocolDirectionClass())
                    .newInstance(Reflection.getEnumAtIndex(NMSUtils.getEnumProtocolDirectionClass(), 1));

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet encoder environment!");
        }
    }

    private final AsyncDecoder asyncDecoder;
    private final Channel channel;
    private volatile boolean forced;

    public OutboundPacketEncoder(Channel channel, AsyncDecoder asyncDecoder) {
        this.channel = channel;
        this.asyncDecoder = asyncDecoder;
    }

    @Override
    public synchronized void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (forced || !ConfigurationOptions.AsyncDecoder.ENABLED) {
            super.write(ctx, msg, promise);
            return;
        }

        ReferenceCountUtil.retain(msg);

        asyncDecoder.encode((s) -> {
            if (asyncDecoder.getPlayer().isOnline()) {
                try {
                    super.write(ctx, msg, new DefaultChannelPromise(ctx.channel()));
                } catch (Exception ex) {
                    try {
                        super.exceptionCaught(ctx, ex);
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    public synchronized void writeForcedPacket(Object value) {
        forced = true;
        channel.write(value);
        forced = false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object value, ByteBuf byteBuf) throws Exception {
        try {
            packetEncode.invoke(commonPacketEncoder, ctx, value, byteBuf);
        } catch (Throwable throwable) {
            throw (Exception) throwable;
        }
    }
}
