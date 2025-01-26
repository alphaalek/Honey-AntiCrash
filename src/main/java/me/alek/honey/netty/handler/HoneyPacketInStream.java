package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.HoneyPacketDecoder;
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
            decoder = pipeline.get(PacketInterceptorResolver.getDecoderPosition(pipeline));

            Method method = decoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            method.setAccessible(true);

            decodeInvoker = MethodHandles.lookup().unreflect(method);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to setup packet decoder environment!", ex);
        }
    }

    private void stopTimings(PacketContainer<?> packet) {
        if (packet.getEventloopStateTimings().getCurrentState() == LifecycleState.NO_STATE) {
            return;
        }

        packet.getEventloopStateTimings().stopTimings();

        if (this.player.isBukkitReady()) {
            HoneyPlugin.get().getNettyPacketTracker().registerTime(player,
                    packet.getEventloopStateTimings().getTimeFor(LifecycleState.HONEY_HANDLE)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.IN_BETWEEN)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.DECODE)
            );
        }
    }

    private void invokeRead(PacketContainer<?> earlyPacketContainer, ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        if (!buf.isReadable()) {
            return;
        }

        try {
            earlyPacketContainer.getEventloopStateTimings().startTimings(LifecycleState.DECODE);

            Object packet;
            try {
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    packet = HoneyPacketDecoder.decodePacket(this.player, this.packetHandler.getProtocolState(PacketBound.SERVER), packetHandler.getProtocolCodec(PacketBound.SERVER), buf);
                } else {
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = this.decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, buf, list});

                    if (list.isEmpty()) {
                        stopTimings(earlyPacketContainer);
                        return;
                    }
                    packet = list.get(0);
                }
            } catch (Throwable ex) {
                if (HoneyPlugin.get().getLogger().isDebug()) {
                    Bukkit.getLogger().log(Level.WARNING,
                            "Error occurred while decoding packet " + earlyPacketContainer.getType() + " (id " + earlyPacketContainer.getType().getMinecraftID().get()
                                    + ") sent by " + this.player.name + ", " + Optional.ofNullable(earlyPacketContainer.getInitialByteWrapped()).map(ByteInWrappedPacket::getByteSize).orElse(-1) + " bytes", ex);
                }
                buf.release();

                this.player.failPacket(ex, earlyPacketContainer);
                return;
            }

            buf.release();

            if (packet == null) {
                stopTimings(earlyPacketContainer);
                return;
            }

            earlyPacketContainer.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);

            // ignore all transaction packets, as well as players with permission to bypass all validations
            if (earlyPacketContainer.getType() == PacketTypes.Play.Client.TRANSACTION
                    || this.player.bypassEverything()) {
                ctx.fireChannelRead(packet);
                stopTimings(earlyPacketContainer);
                return;
            }

            PacketContainer<?> finalPacketContainer = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().readObjectPacket(player, packet, earlyPacketContainer);

            Runnable packetExecutorTask;
            if (!finalPacketContainer.isCancelled()) {
                packetExecutorTask = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPacketExecutorTask(finalPacketContainer);
            } else {
                packetExecutorTask = null;
            }

            stopTimings(earlyPacketContainer);

            if (ctx.channel().eventLoop().inEventLoop()) {
                firePacketRead(ctx, finalPacketContainer, packetExecutorTask);
            } else {
                ctx.channel().eventLoop().execute(() -> firePacketRead(ctx, finalPacketContainer, packetExecutorTask));
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {
            }

            stopTimings(earlyPacketContainer);
        }
    }

    private <WP extends SourcedWrappedPacket<WP>> void firePacketRead(ChannelHandlerContext ctx, PacketContainer<WP> packetContainer, Runnable packetExecutorTask) {
        if (!this.packetHandler.read(packetContainer)) {
            return;
        }

        if (packetExecutorTask != null) {
            HoneyPlugin.get().getExecutionQueueProxy().getExecutionQueue().add(packetExecutorTask);
            // drop the packet for the pipeline
            return;
        }

        if (packetContainer.getObjectWrapped() != null) {
            ctx.fireChannelRead(packetContainer.getObjectWrapped().getHandle());
        }

        Callback postAction = packetContainer.getPostAction();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        if (!read(ctx, o)) {
            // not async
            PacketContainer<?> packet = InboundPacketFilter.MARK.get();
            if (packet != null) {
                packet.releaseIfAutoReleasable();
            }
        }

        InboundPacketFilter.MARK.remove();
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

            List<Object> nonRecycledList = new ArrayList<>();
            // TODO:
            // force sync packet stream until fixed
            if (true || !ConfigurationOptions.AsyncDecoder.ENABLED
                    || !this.player.isBukkitReady()
                    || !PacketInterceptorResolver.hasEncoderHandler(ctx.pipeline())) {
                invokeRead(packet, ctx, buf, nonRecycledList);
                return false;
            } else {
                ByteBuf wrapped = buf.readBytes(buf.readableBytes()).retain();
                buf.release();

                PacketContainer<?> copy = packet;
                this.stream.decode((s) -> {
                    InboundPacketFilter.MARK.set(copy);

                    if (this.player.isStreamActive()) {
                        invokeRead(copy, ctx, wrapped, nonRecycledList);
                    }

                    copy.releaseIfAutoReleasable();
                    InboundPacketFilter.MARK.remove();
                });

                return true;
            }
        } catch (Exception ex) {
            if (packet != null && HoneyPlugin.get().getLogger().isDebug()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + packet.getType() + " (id " + packet.getType().getMinecraftID().get()
                                + ") sent by " + this.player.name + ", " + buf.readableBytes() + " bytes", ex);
            }
            serializer.release();

            this.player.failPacket(ex, packet);

            return false;
        }
    }
}