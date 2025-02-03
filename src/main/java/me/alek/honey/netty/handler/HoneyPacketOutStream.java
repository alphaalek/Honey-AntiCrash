package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
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
import me.alek.honey.util.Callback;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyPacketOutStream extends ChannelOutboundHandlerAdapter {

    private final AsyncPacketStream async;
    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;
    private ChannelHandler encoder;
    private MethodHandle encodeInvoker;

    public HoneyPacketOutStream(Channel channel, AsyncPacketStream async, HoneyPacketHandler packetHandler) {
        this.async = async;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
        setupMethodInvoke(channel.pipeline());
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AsyncPacketStream getAsyncPacketStream() {
        return async;
    }

    private void setupMethodInvoke(ChannelPipeline pipeline) {
        try {
            this.encoder = pipeline.get(PacketInterceptorResolver.getEncoderPosition(pipeline));
            Method method = Reflection.getMethod(this.encoder.getClass(), 0, ChannelHandlerContext.class, MinecraftReflection.getPacketClass(), ByteBuf.class).getHandle();
            this.encodeInvoker = MethodHandles.lookup().unreflect(method);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to setup packet encoder environment!", ex);
        }
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
                || !this.player.linkedBukkit()
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

        PacketContainer<WP> packet = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().writeObjectPacket(this.player, msg);
        if (packet == null) {
            promise.setSuccess();
            return;
        }
        if (packet.isHandled()) {
            ctx.write(packet.getObjectWrapped().getHandle(), promise);
            return;
        }
        packet.handle();

        if (!this.packetHandler.write(packet)) {
            promise.setSuccess();
            return;
        }
        PacketDataSerializer buf = null;
        try {
            try {
                // serialize before swap protocol
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    buf = HoneyPacketCodec.encodePacket(this.player, msg);
                } else {
                    List<Object> list = new ArrayList<>();
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = this.encodeInvoker.invokeWithArguments(new Object[]{this.encoder, ctx, buf, list});
                    if (list.isEmpty()) {
                        promise.setSuccess();
                        return;
                    }
                    buf = new PacketDataSerializer((ByteBuf) list.get(0));
                }
            } catch (Throwable ex) {
                if (HoneyPlugin.get().getLogger().isDebug()) {
                    Bukkit.getLogger().log(Level.WARNING,
                            "Error occurred while encoding packet " + packet.getType() + " (id " + packet.getType().getMinecraftID().get()
                                    + ") sent by " + this.player.name, ex);
                }
                promise.setSuccess();
                return;
            }

            this.packetHandler.getProtocolSwapHandler().swapProtocolOutbound(packet);
            ctx.write(buf, promise);
            this.packetHandler.getProtocolSwapHandler().swapProtocolOutboundPostPipeline(packet);

            Callback postAction = packet.getPostAction();
            if (postAction != null) {
                postAction.call(packet);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {
                // to do nothing
            }
        }
    }

    public void writeForcedPacket(Object value) {
        if (!this.player.channel.eventLoop().inEventLoop()) {
            this.player.channel.eventLoop().submit(() -> writeForcedPacket(value));
            return;
        }

        try {
            ChannelHandlerContext ctx = this.player.channel.pipeline().context(this);
            if (ctx == null) {
                return;
            }

            write0(ctx, value, new DefaultChannelPromise(this.player.channel));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
