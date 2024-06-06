package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.HoneyPacketDecoder;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.util.UnsafeChannelCloser;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.async.AsyncDecoder;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.cancellables.CancellablePacketImpl;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@ChannelHandler.Sharable
public class HoneyPacketDecodePreparation extends ChannelInboundHandlerAdapter {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Queue<Object> serverThreadExecutionQueue = HoneyPlugin.get().getServerThreadTracker().getExecutionQueue();

    private final AsyncDecoder asyncDecoder;
    private final HoneyPacketHandler packetHandler;
    private ViolationService.PacketIntervalThrottleHandler exemptedIntervalThrottle;
    private HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public HoneyPacketDecodePreparation(Channel channel, AsyncDecoder asyncDecoder, HoneyPacketHandler packetHandler) {
        this.asyncDecoder = asyncDecoder;
        this.packetHandler = packetHandler;
        setupMethodInvoke(channel.pipeline());
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
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

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> packetContainer, ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)  {
        try {
            if (packetContainer != null) {
                packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
            }

            Object packet;
            try {
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    packet = HoneyPacketDecoder.decodePacket(player, packetHandler.getProtocolState(PacketBound.SERVER), packetHandler.getProtocol(PacketBound.SERVER), byteBuf);
                } else {
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, byteBuf, list});

                    if (list.isEmpty()) {
                        return;
                    }
                    packet = list.get(0);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                byteBuf.release();

                UnsafeChannelCloser.closeForcibly(ctx.channel());

                if (player != null) {
                    player.punished = true;
                }
                return;
            }

            Bukkit.getLogger().info("Decoded " + packet + " " + packetHandler.getProtocolState(PacketBound.SERVER) + " " + packetHandler.getProtocol(PacketBound.SERVER));

            byteBuf.release();

            if (packet == null) {
                return;
            }

            if (packetContainer != null) {
                packetContainer.getEventloopStateTimings().stopTimings();

                if (player != null) {
                    HoneyPlugin.get().getNettyPacketTracker().registerTime(player, (double) packetContainer.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_DECODE));
                }

                if (packetContainer.getType() == PacketType.Play.Client.TRANSACTION
                        || ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && player.player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {

                    if (exemptedIntervalThrottle != null) {
                        exemptedIntervalThrottle.flag(packet, new CancellablePacketImpl(packetContainer), null, 1, null, null, null, null, null);

                        if (packetContainer.isCancelled()) {
                            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
                                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayerName());
                            }
                            return;
                        }
                    }

                    ctx.fireChannelRead(packet);
                    return;
                }
            }

            PacketContainer<? extends WrappedPacket<?>> finalPacketContainer = packetProcessor.read(ctx.channel(), player, packet, packetContainer);

            Runnable packetExecutorTask;
            if (!finalPacketContainer.isCancelled()) {
                packetExecutorTask = packetProcessor.getPacketExecutorTask(finalPacketContainer);
            } else {
                packetExecutorTask = null;
            }

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
        }
    }

    private <WP extends WrappedPacket<WP>> void firePacketRead(ChannelHandlerContext ctx, PacketContainer<WP> packetContainer, Runnable packetExecutorTask) {
        if (!packetHandler.read(packetContainer)) {
            return;
        }

        if (packetExecutorTask != null) {
            serverThreadExecutionQueue.add(packetExecutorTask);
            return;
        }

        ctx.fireChannelRead(packetContainer.getHandle());

        CallbackAction<PacketContainer<WP>> postAction = packetContainer.getPost();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        if (!ctx.channel().isOpen() || !(o instanceof ByteBuf)) {
            return;
        }

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
        if (!ConfigurationOptions.AsyncDecoder.ENABLED || player == null) {
            invokeRead(packetContainer, ctx, byteBuf, nonRecycledList);
            return;
        }

        ByteBuf wrappedBuffer = Unpooled.buffer().writeBytes(byteBuf).retain();
        byteBuf.release();

        asyncDecoder.decode((s) -> {
            if (player.isOnlineWeak() && !player.punished) {
                invokeRead(packetContainer, ctx, wrappedBuffer, nonRecycledList);
            }
        });
    }
}