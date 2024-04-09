package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.async.AsyncDecoder;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.CallbackAction;
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
    private HoneyPlayer player;

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

        if (player == null && asyncDecoder.getPlayer() != null) {
            player = asyncDecoder.getPlayer();
        }

        if (player == null) {
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
        if (msg instanceof ByteBuf) {
            if (promise != null) {
                ctx.write(msg, promise);
            } else {
                ctx.write(msg);
            }
            return;
        }

        @SuppressWarnings("unchecked")
        PacketContainer<WP> packetContainer = (PacketContainer<WP>) packetProcessor.write(ctx.channel(), asyncDecoder.getPlayer(), msg);
        if (!packetHandler.write(packetContainer)) {
            return;
        }

        if (retain) {
            ReferenceCountUtil.retain(msg);
        }

        if (promise != null) {
            ctx.write(msg, promise);
        } else {
            ctx.write(msg);
        }

        CallbackAction<PacketContainer<WP>> postAction = packetContainer.getPost();
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

            write0(ctx, value, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
