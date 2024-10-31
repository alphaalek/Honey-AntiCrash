package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.codec.HoneyPacketDecoder;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.WrappedPacketFactoryCollection;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyPacketDecodePreparation extends ChannelInboundHandlerAdapter {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Queue<Object> serverThreadExecutionQueue = HoneyPlugin.get().getExecutionQueueProxy().getExecutionQueue();

    private final AsyncPacketStream stream;
    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public HoneyPacketDecodePreparation(Channel channel, AsyncPacketStream async, HoneyPacketHandler packetHandler) {
        this.stream = async;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
        setupMethodInvoke(channel.pipeline());
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public AsyncPacketStream getAsync() {
        return stream;
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

    private void stopTimings(PacketContainer<? extends WrappedPacket<?>> packet) {
        if (packet.getEventloopStateTimings().getCurrentState() == LifecycleState.NO_STATE) {
            return;
        }

        packet.getEventloopStateTimings().stopTimings();

        if (player.isBukkitReady()) {
            HoneyPlugin.get().getNettyPacketTracker().registerTime(player,
                      packet.getEventloopStateTimings().getTimeFor(LifecycleState.HONEY_HANDLE)
                    + packet.getEventloopStateTimings().getTimeFor(LifecycleState.IN_BETWEEN)
                    + packet.getEventloopStateTimings().getTimeFor(LifecycleState.DECODE)
            );
        }
    }

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> earlyPacketContainer, ChannelHandlerContext ctx, ByteBuf buf, List<Object> list)  {
        if (!buf.isReadable()) {
            return;
        }

        try {
            earlyPacketContainer.getEventloopStateTimings().startTimings(LifecycleState.DECODE);

            Object packet;
            try {
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    packet = HoneyPacketDecoder.decodePacket(player, packetHandler.getProtocolState(PacketBound.SERVER), packetHandler.getProtocolCodec(PacketBound.SERVER), buf);
                } else {
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, buf, list});

                    if (list.isEmpty()) {
                        stopTimings(earlyPacketContainer);
                        return;
                    }
                    packet = list.get(0);
                }
            } catch (Throwable ex) {
                if (HoneyPlugin.get().getLoggerService().isDebug()) {
                    Bukkit.getLogger().log(Level.INFO, "Error occurred while decoding packet sent by " + player + ", " + Optional.ofNullable(earlyPacketContainer.getInitialByteWrapped()).map(ByteWrappedPacket::getByteSize).orElse(-1) + " bytes", ex);
                }
                buf.release();

                if (ex.getMessage() != null) {
                    player.bukkit.kickPlayer(ex.getMessage());
                } else {
                    player.bukkit.kickPlayer("Disconnected");
                }
                player.punished = true;

                stopTimings(earlyPacketContainer);
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
                    || player.bypassEverything()) {
                ctx.fireChannelRead(packet);
                stopTimings(earlyPacketContainer);
                return;
            }

            PacketContainer<? extends WrappedPacket<?>> finalPacketContainer = packetProcessor.read(player, packet, earlyPacketContainer);

            Runnable packetExecutorTask;
            if (!finalPacketContainer.isCancelled()) {
                packetExecutorTask = packetProcessor.getPacketExecutorTask(finalPacketContainer);
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

    private <WP extends WrappedPacket<WP>> void firePacketRead(ChannelHandlerContext ctx, PacketContainer<WP> packetContainer, Runnable packetExecutorTask) {
        if (!packetHandler.read(packetContainer)) {
            return;
        }

        if (packetExecutorTask != null) {
            serverThreadExecutionQueue.add(packetExecutorTask);
            // drop the packet for the pipeline
            return;
        }

        if (packetContainer.getObjectWrapped() != null) {
            ctx.fireChannelRead(packetContainer.getObjectWrapped().getHandle());
        }

        Callback postAction = packetContainer.getPost();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        if (!read(ctx, o)) {
            // not async
            InboundPacketFilter.MARK.get().releaseIfAutoReleasable();
        }

        InboundPacketFilter.MARK.remove();
    }

    public boolean read(ChannelHandlerContext ctx, Object msg) {
        if (!player.isStreamActive()) {
            return false;
        }

        if (!(msg instanceof ByteBuf)) {
            if (!packetHandler.handleNonPacket(msg)) {
                return false;
            }
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return false;
        }

        ByteBuf buf = (ByteBuf) msg;
        if (!buf.isReadable()) {
            ctx.fireChannelRead(buf);
            return false;
        }

        /*
        if (WrappedPacketFactoryCollection.hasFactoryFor()) {

        }
        */

        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<? extends WrappedPacket<?>> packet = InboundPacketFilter.MARK.get();
        if (packet == null) {
            // this packet is not sent by the client (because it was not received by the filter),
            // but instead invoked by some plugin (most likely ViaVersion)
            packet = new InternalPacketContainer<>(PacketContainer.PipelineState.FINAL_BYTE, serializer, player, packetHandler.getPacketType(buf, false), null);
            InboundPacketFilter.MARK.set(packet);
        } else {
            serializer.readVarInt(); // skip packet-id
        }

        // stops in-between timings
        packet.getEventloopStateTimings().stopTimings();

        // handle final byte state
        packet.initPreMinecraftDecode(serializer);
        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callFinalBytePacketProcessListeners(player, packet);
        serializer.resetReaderIndex();

        List<Object> nonRecycledList = new ArrayList<>();
        if (!ConfigurationOptions.AsyncDecoder.ENABLED
                || !player.isBukkitReady()
                || !PacketInterceptorResolver.hasEncoderHandler(ctx.pipeline())) {
            invokeRead(packet, ctx, buf, nonRecycledList);
            return false;
        } else {
            ByteBuf wrapped = buf.readBytes(buf.readableBytes()).retain();
            buf.release();

            PacketContainer<? extends WrappedPacket<?>> copy = packet;
            stream.decode((s) -> {
                InboundPacketFilter.MARK.set(copy);

                if (player.isStreamActive()) {
                    invokeRead(copy, ctx, wrapped, nonRecycledList);
                    copy.releaseIfAutoReleasable();
                }

                InboundPacketFilter.MARK.remove();
            });

            return true;
        }
    }
}