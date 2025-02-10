package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
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

    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public HoneyPacketInStream(Channel channel, HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
        setupMethodInvoke(channel.pipeline());
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
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
                    HoneyPlugin.get().getLogger().getLogger().log(Level.WARNING,
                            "Error occurred while decoding packet " + earlyPacket.getType() + " (id " + earlyPacket.getType().getID()
                                    + ") sent by " + this.player.name + ", " + Optional.ofNullable(earlyPacket.getAnyByteWrapped()).map(ByteInWrappedPacket::getByteSize).orElse(-1) + " bytes", ex);
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
            if (finalPacket == null) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to link object-instance to expected packet type, for " + earlyPacket.getType() + " sent by " + this.player.name + "; " + packetObject.getClass());
                return;
            }

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
                // to do nothing
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
            PreMainThreadInterceptor.POST_TO_MAIN_THREAD.set(packet);
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        internalRead(ctx, msg);
        if (this.packetHandler.filterMark != null) {
            this.packetHandler.filterMark.releaseIfAutoReleasable();
            this.packetHandler.filterMark = null;
            InboundPacketFilter.MARK.remove();
        }
    }

    private void internalRead(ChannelHandlerContext ctx, Object msg) {
        if (!this.player.isStreamActive()) {
            return;
        }

        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf buf = (ByteBuf) msg;
        if (!buf.isReadable()) {
            ctx.fireChannelRead(buf);
            return;
        }

        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<?> packet = null;
        PacketType type = null;
        try {
            packet = this.packetHandler.filterMark;
            type = this.packetHandler.getPacketType(serializer, false, PacketBound.SERVER, false);

            if (packet == null || !packet.getType().equals(type)) {
                // this packet is not sent by the client (because it was not received by the filter),
                // but instead invoked by some plugin (most likely ViaVersion)
                if (packet == null) {
                    serializer.markReaderIndex();
                    packet = new InternalPacketContainer<>(PacketContainer.PipelineState.FINAL_BYTE, serializer, this.player, type, null);
                    serializer.resetReaderIndex();
                } else {
                    packet.handleRewritten(type, PacketContainer.PipelineState.FINAL_BYTE);
                }

                // calls listeners that was not called, due to some plugin invoking this packet in the eventloop after the filter
                packet.callLateInitialByteListeners(serializer);
                serializer.readerIndex(0);
            }
            if (PacketTypes.isUnknown(packet.getType())) {
                return;
            }

            // stops in-between timings
            packet.getEventloopStateTimings().stopTimings();

            // handle final byte state
            if (WrappedPacketFactoryCollection.hasByteWrappedPacket(packet.getType(), PacketContainer.PipelineState.FINAL_BYTE)) {
                packet.callPreMinecraftDecode(serializer);
            }
            serializer.readerIndex(0);

            invokeRead(packet, ctx, buf);
        } catch (Exception ex) {
            if (HoneyPlugin.get().getLogger().isDebug()) {
                type = Optional.ofNullable(type).orElse(PacketType.UNKNOWN);
                HoneyPlugin.get().getLogger().getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + type + " (id " + type.getID()
                                + ") sent by " + this.player.name + ", " + (buf.readerIndex() + buf.readableBytes()) + " bytes", ex);
            }
            serializer.release();
            this.player.failPacket(ex, packet);
        }
    }
}