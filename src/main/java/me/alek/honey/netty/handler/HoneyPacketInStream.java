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
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.WrappedPacketFactoryCollection;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyPacketInStream extends ChannelInboundHandlerAdapter {

    private final AsyncPacketStream stream;
    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public HoneyPacketInStream(Channel channel, AsyncPacketStream async, HoneyPacketHandler packetHandler) {
        this.stream = async;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
        setupMethodInvoke(channel.pipeline());
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    public AsyncPacketStream getAsyncPacketStream() {
        return this.stream;
    }

    private void setupMethodInvoke(ChannelPipeline pipeline) {
        try {
            this.decoder = pipeline.get(PacketInterceptorResolver.getDecoderPosition(pipeline));
            Method method = this.decoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            method.setAccessible(true);
            this.decodeInvoker = MethodHandles.lookup().unreflect(method);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to setup packet decoder environment!", ex);
        }
    }

    private void stopTimings(PacketContainer<?> packet) {
        if (packet.getEventloopStateTimings().getCurrentState() == LifecycleState.NO_STATE) {
            return;
        }

        packet.getEventloopStateTimings().stopTimings();

        if (this.player.linkedBukkit()) {
            HoneyPlugin.get().getNettyPacketTracker().registerTime(player,
                    packet.getEventloopStateTimings().getTimeFor(LifecycleState.HONEY_HANDLE)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.IN_BETWEEN)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.DECODE)
            );
        }
    }

    private void invokeRead(PacketContainer<?> earlyPacket, ChannelHandlerContext ctx, ByteBuf buf) {
        earlyPacket.getEventloopStateTimings().startTimings(LifecycleState.DECODE);
        try {
            Object packetObject;
            try {
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    packetObject = HoneyPacketCodec.decodePacket(this.player, buf);
                } else {
                    List<Object> list = new ArrayList<>();
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = this.decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, buf, list});
                    if (list.isEmpty()) {
                        stopTimings(earlyPacket);
                        return;
                    }
                    packetObject = list.get(0);
                }
            } catch (Throwable ex) {
                if (HoneyPlugin.get().getLogger().isDebug()) {
                    Bukkit.getLogger().log(Level.WARNING,
                            "Error occurred while decoding packet " + earlyPacket.getType() + " (id " + earlyPacket.getType().getMinecraftID().get()
                                    + ") sent by " + this.player.name + ", " + Optional.ofNullable(earlyPacket.getInitialByteWrapped()).map(ByteInWrappedPacket::getByteSize).orElse(-1) + " bytes", ex);
                }
                buf.release();
                this.player.failPacket(ex, earlyPacket);
                return;
            }
            buf.release();
            if (packetObject == null) {
                stopTimings(earlyPacket);
                return;
            }
            earlyPacket.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);

            // ignore all transaction/pong packets, as well as players with permission to bypass all validations
            if (earlyPacket.getType() == PacketTypes.Play.Client.TRANSACTION || earlyPacket.getType() == PacketTypes.Play.Client.PONG
                    || this.player.bypassEverything()) {
                stopTimings(earlyPacket);
                ctx.fireChannelRead(packetObject);
                return;
            }

            PacketContainer<?> finalPacket = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().readObjectPacket(player, packetObject, earlyPacket);

            Runnable packetExecutorTask;
            if (!finalPacket.isCancelled()) {
                packetExecutorTask = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPacketExecutorTask(finalPacket);
            } else {
                packetExecutorTask = null;
            }

            stopTimings(earlyPacket);

            if (ctx.channel().eventLoop().inEventLoop()) {
                firePacketRead(ctx, finalPacket, packetExecutorTask);
            } else {
                ctx.channel().eventLoop().execute(() -> firePacketRead(ctx, finalPacket, packetExecutorTask));
            }
        } catch (Throwable ex) {
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {
            }

            stopTimings(earlyPacket);
        }
    }

    private <WP extends SourcedWrappedPacket<WP>> void firePacketRead(ChannelHandlerContext ctx, PacketContainer<WP> packet, Runnable packetExecutorTask) {
        if (!this.packetHandler.read(packet)) {
            return;
        }
        packet.handle();

        this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(packet);

        if (packetExecutorTask != null) {
            HoneyPlugin.get().getExecutionQueueProxy().getExecutionQueue().add(packetExecutorTask);
            // drop the packet for the pipeline
            return;
        }

        if (packet.getObjectWrapped() != null) {
            ctx.fireChannelRead(packet.getObjectWrapped().getHandle());
        }

        Callback postAction = packet.getPostAction();
        if (postAction != null) {
            postAction.call(packet);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        if (!read(ctx, o)) {
            PacketContainer<?> packet = HoneyPacketHandler.MARK.get();
            if (packet != null) {
                packet.releaseIfAutoReleasable();
            }
        }

        InboundPacketFilter.removeMarker();
    }

    public boolean read(ChannelHandlerContext ctx, Object msg) {
        if (!this.player.isStreamActive()) {
            return false;
        }

        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return false;
        }

        ByteBuf buf = (ByteBuf) msg;
        if (!buf.isReadable()) {
            ctx.fireChannelRead(buf);
            return false;
        }

        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<?> packet = null;
        try {
            packet = InboundPacketFilter.MARK.get();
            PacketType type = this.packetHandler.getPacketType(serializer, false, PacketBound.SERVER, false);

            if (packet == null || !packet.getType().equals(type)) {
                // this packet is not sent by the client (because it was not received by the filter),
                // but instead invoked by some plugin (most likely ViaVersion)
                if (packet == null) {
                    serializer.markReaderIndex();
                    packet = new InternalPacketContainer<>(PacketContainer.PipelineState.FINAL_BYTE, serializer, this.player, type, null);
                    serializer.resetReaderIndex();
                    InboundPacketFilter.MARK.set(packet);
                } else {
                    packet.handleRewritten(type, PacketContainer.PipelineState.FINAL_BYTE);
                }

                // calls listeners that was not called, due to some plugin invoking this packet in the eventloop after the filter
                packet.callLateInitialByteListeners(serializer);
                serializer.readerIndex(0);
            }
            if (PacketTypes.isUnknown(packet.getType())) {
                return false;
            }

            // stops in-between timings
            packet.getEventloopStateTimings().stopTimings();

            // handle final byte state
            if (WrappedPacketFactoryCollection.hasByteWrappedPacket(packet.getType(), PacketContainer.PipelineState.FINAL_BYTE)) {
                packet.callPreMinecraftDecode(serializer);
            }
            serializer.readerIndex(0);

            // TODO:
            // force sync packet stream until fixed
            if (true || !ConfigurationOptions.AsyncDecoder.ENABLED
                    || !this.player.linkedBukkit()
                    || !PacketInterceptorResolver.hasEncoderHandler(ctx.pipeline())) {
                invokeRead(packet, ctx, buf);
                return false;
            } else {
                ByteBuf wrapped = buf.readBytes(buf.readableBytes()).retain();
                buf.release();

                PacketContainer<?> copy = packet;
                this.stream.decode((s) -> {
                    InboundPacketFilter.MARK.set(copy);

                    if (this.player.isStreamActive()) {
                        invokeRead(copy, ctx, wrapped);
                    }

                    copy.releaseIfAutoReleasable();
                    InboundPacketFilter.removeMarker();
                });
                return true;
            }
        } catch (Exception ex) {
            if (packet != null && HoneyPlugin.get().getLogger().isDebug()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + packet.getType() + " (id " + Optional.ofNullable(packet.getType()).map(t -> t.getMinecraftID().get()).orElse(-1)
                                + ") sent by " + this.player.name + ", " + buf.readableBytes() + " bytes", ex);
            }
            serializer.release();
            this.player.failPacket(ex, packet);
            return false;
        }
    }
}