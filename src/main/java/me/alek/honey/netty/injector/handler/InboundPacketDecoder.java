package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.async.ChainedCallable;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.Reflection;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.List;

public class InboundPacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final Method packetDecode;
    private static final Object commonPacketDecoder;

    static {
        try {
            packetDecode = ByteToMessageDecoder.class.getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            packetDecode.setAccessible(true);

            commonPacketDecoder = NMSUtils.getPacketDecoderClass().getDeclaredConstructor(NMSUtils.getEnumProtocolDirectionClass())
                    .newInstance(Reflection.getEnumAtIndex(NMSUtils.getEnumProtocolDirectionClass(), 0));

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet decoder environment!");
        }
    }

    private final AsyncDecoder asyncDecoder;

    public InboundPacketDecoder(AsyncDecoder asyncDecoder) {
        this.asyncDecoder = asyncDecoder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!ConfigurationOptions.AsyncDecoder.ENABLED) {
            super.channelRead(ctx, msg);
            return;
        }
        ReferenceCountUtil.retain(msg);

        asyncDecoder.decode((s) -> {
            if (asyncDecoder.getPlayer().isOnline()) {
                try {
                    Bukkit.getLogger().info("Calling " + Thread.currentThread().getName());
                    super.channelRead(ctx, msg);
                } catch (Exception ex) {
                    try {
                        super.exceptionCaught(ctx, ex);
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        try {
            packetDecode.invoke(commonPacketDecoder, ctx, byteBuf, list);
        } catch (Throwable throwable) {
            throw (Exception) throwable;
        }
    }
}
