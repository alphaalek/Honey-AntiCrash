package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.ChannelCloser;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.cancellables.CancellablePacketImpl;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@ChannelHandler.Sharable
public class InboundPacketDecoder extends ChannelInboundHandlerAdapter {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Queue<Object> serverThreadExecutionQueue = ((ServerThreadHandleTracker)HoneyPlugin.get().getServerThreadTracker()).getExecutionQueue();

    private final AsyncDecoder asyncDecoder;
    private final HoneyPacketHandler packetHandler;
    private ViolationService.PacketIntervalThrottleHandler exemptedIntervalThrottle;
    private HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public InboundPacketDecoder(Channel channel, AsyncDecoder asyncDecoder, HoneyPacketHandler packetHandler) {
        this.asyncDecoder = asyncDecoder;
        this.packetHandler = packetHandler;
        setupMethodInvoke(channel.pipeline());
    }

    public AsyncDecoder getAsyncDecoder() {
        return asyncDecoder;
    }

    private void setupMethodInvoke(ChannelPipeline pipeline) {
        try {
            decoder = pipeline.get(PacketInterceptorResolver.getDecoderPosition(pipeline));

            Method method = decoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            method.setAccessible(true);

            decodeInvoker = MethodHandles.lookup().unreflect(method);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet decoder environment!");
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        try {
            super.handlerAdded(ctx);
        } catch (Exception ignored) {

        }

        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        try {
            super.handlerRemoved(ctx);
        } catch (Exception ignored) {

        }

        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
    }

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> packetContainer, ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)  {
        try {
            if (packetContainer != null) {
                packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
            }

            try {
                // explicit set variable, so Proguard recognizes the method
                Object empty = decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, byteBuf, list});
            } catch (Exception ex) {
                byteBuf.release();

                if (ctx.channel().isOpen()) {
                    ChannelCloser.closeForcibly(ctx.channel());
                }

                if (player != null) {
                    player.punished = true;
                }
                return;
            }
            byteBuf.release();

            if (list.isEmpty()) {
                return;
            }

            if (packetContainer == null
                    || packetContainer.getType() == PacketType.Play.Client.TRANSACTION
                    || ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && player.player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {

                Object packet = list.get(0);
                if (packetContainer != null && exemptedIntervalThrottle != null) {
                    exemptedIntervalThrottle.flag(packet, new CancellablePacketImpl(packetContainer), null, 1, null, null, null, null, null);

                    if (packetContainer.isCancelled()) {
                        if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
                            HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().name);
                        }
                        return;
                    }
                }

                ctx.fireChannelRead(packet);
                return;
            }

            packetContainer.getEventloopStateTimings().stopTimings();
            if (player != null) {
                HoneyPlugin.get().getNettyPacketTracker().registerTime(player, (double) packetContainer.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_DECODE));
            }

            PacketContainer<? extends WrappedPacket<?>> finalPacketContainer = packetProcessor.read(ctx.channel(), player, list.get(0), packetContainer.getTime(), packetContainer);

            Runnable packetExecutorTask = null;
            if (ConfigurationOptions.AsyncDecoder.ENABLED && !finalPacketContainer.isCancelled()) {
                packetExecutorTask = packetProcessor.getPacketExecutorTask(finalPacketContainer);
            }
            Runnable finalPacketExecutorTask = packetExecutorTask;

            ctx.channel().eventLoop().execute(() -> {
                if (!packetHandler.read0(finalPacketContainer)) {
                    return;
                }

                if (finalPacketExecutorTask != null) {
                    serverThreadExecutionQueue.add(finalPacketExecutorTask);
                    return;
                }

                ctx.fireChannelRead(finalPacketContainer.getHandle());
                packetHandler.postRead();
            });
        } catch (Throwable ex) {
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        if (!(o instanceof ByteBuf)) return;

        ByteBuf byteBuf = (ByteBuf) o;
        if (byteBuf.readableBytes() == 0) {
            return;
        }

        if (player == null && asyncDecoder.getPlayer() != null) {
            player = asyncDecoder.getPlayer();
            exemptedIntervalThrottle = new ViolationService.PacketIntervalThrottleHandler(
                    player, ConfigurationOptions.getFloodSettings().get(PacketType.Play.Client.TRANSACTION));
        }

        PacketContainer<? extends WrappedPacket<?>> packetContainer = InboundPacketFilter.getEarlyInitializedPacket();

        List<Object> nonRecycledList = new ArrayList<>();
        if (player == null || !ConfigurationOptions.AsyncDecoder.ENABLED) {
            invokeRead(packetContainer, ctx, byteBuf, nonRecycledList);
            return;
        }

        ByteBuf wrappedBuffer = Unpooled.buffer().writeBytes(byteBuf).retain();
        byteBuf.release();

        asyncDecoder.decode((s) -> {
            if (player.isOnline() && !player.punished) {
                invokeRead(packetContainer, ctx, wrappedBuffer, nonRecycledList);
            }
        });
    }
}