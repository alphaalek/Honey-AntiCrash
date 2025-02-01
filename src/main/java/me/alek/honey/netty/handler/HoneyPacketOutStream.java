package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.processor.PacketProcessor;
import me.alek.honey.util.Callback;

import java.lang.reflect.Method;

@ChannelHandler.Sharable
public class HoneyPacketOutStream extends ChannelOutboundHandlerAdapter {

    private static final PacketProcessor PACKET_PROCESSOR = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Method PACKET_ENCODING;

    static {
        try {
            PACKET_ENCODING = MessageToByteEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, ByteBuf.class);
            PACKET_ENCODING.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to setup packet encoder environment!", ex);
        }
    }

    private final AsyncPacketStream async;
    private final HoneyPacketHandler packetHandler;
    private final Channel channel;
    private final HoneyPlayer player;

    public HoneyPacketOutStream(Channel channel, AsyncPacketStream async, HoneyPacketHandler packetHandler) {
        this.channel = channel;
        this.async = async;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AsyncPacketStream getAsyncPacketStream() {
        return async;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!this.player.isStreamActive()) {
            return;
        }

        if (!this.packetHandler.isPacket(msg)) {
            ctx.write(msg, promise);
            return;
        }

        // TODO fix
        if (true || !ConfigurationOptions.AsyncDecoder.ENABLED
                || !this.player.isBukkitReady()
                || !PacketInterceptorResolver.hasDecoderHandler(ctx.pipeline())) {
            write0(ctx, msg, promise);
        } else {
            this.async.prependDecode((s) -> {
                if (this.player.isStreamActive()) {
                    try {
                        ChannelPromise asyncPromise;
                        if (!promise.isDone()) {
                            asyncPromise = promise;
                        } else {
                            asyncPromise = new DefaultChannelPromise(ctx.channel());
                        }

                        write0(ctx, ReferenceCountUtil.retain(msg), asyncPromise);
                    } catch (Exception ex) {
                        try {
                            super.exceptionCaught(ctx, ex);
                        } catch (Exception ignored) {
                            // to do nothing
                        }
                    }
                }
            });
        }
    }

    private <WP extends SourcedWrappedPacket<WP>> void write0(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        // fix login fallback state if error happens during player login (can be both during handshake, configuration or login)
        if (!this.packetHandler.getProtocolState(PacketBound.CLIENT).equals(PacketState.PLAY)
                && msg.getClass().equals(PacketTypes.Login.Server.DISCONNECT.getNmsClass())) {
            this.packetHandler.setProtocolState(PacketBound.CLIENT, PacketState.LOGIN);
        }

        PacketContainer<WP> packet = PACKET_PROCESSOR.writeObjectPacket(this.player, msg);
        if (packet == null) {
            return;
        }
        if (packet.isHandled()) {
            ctx.write(packet.getObjectWrapped().getHandle());
            return;
        }
        packet.handle();

        if (!this.packetHandler.write(packet)) {
            promise.setSuccess();
            return;
        }

        PacketDataSerializer buf = null;
        try {
            // serialize before swap protocol
            buf = HoneyPacketCodec.encodePacket(this.player, msg);

            this.packetHandler.getProtocolSwapHandler().swapProtocolOutbound(packet);
            ctx.write(buf, promise);
            this.packetHandler.getProtocolSwapHandler().swapProtocolOutboundPostPipeline(packet);

            Callback postAction = packet.getPostAction();
            if (postAction != null) {
                postAction.call(packet);
            }
        } catch (Exception ex) {
            ex.printStackTrace();

            if (buf != null) {
                buf.release();
            }
        }
    }

    public void writeForcedPacket(Object value) {
        if (!this.channel.eventLoop().inEventLoop()) {
            this.channel.eventLoop().submit(() -> writeForcedPacket(value));
            return;
        }

        try {
            ChannelHandlerContext ctx = this.channel.pipeline().context(this);
            if (ctx == null) {
                return;
            }

            write0(ctx, value, new DefaultChannelPromise(this.channel));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
