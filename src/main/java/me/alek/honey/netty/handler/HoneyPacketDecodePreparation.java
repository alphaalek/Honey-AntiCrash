package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncDecoder;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.UnsafeChannelCloser;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.codec.HoneyPacketDecoder;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@ChannelHandler.Sharable
public class HoneyPacketDecodePreparation extends ChannelInboundHandlerAdapter {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Queue<Object> serverThreadExecutionQueue = HoneyPlugin.get().getExecutionQueueProxy().getExecutionQueue();

    private final AsyncDecoder asyncDecoder;
    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public HoneyPacketDecodePreparation(Channel channel, AsyncDecoder asyncDecoder, HoneyPacketHandler packetHandler) {
        this.asyncDecoder = asyncDecoder;
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
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

    private void stopTimings(PacketContainer<? extends WrappedPacket<?>> packet) {
        if (packet.getEventloopStateTimings().getCurrentState() == EventloopState.NO_STATE) {
            return;
        }

        packet.getEventloopStateTimings().stopTimings();

        if (player.isBukkitReady()) {
            HoneyPlugin.get().getNettyPacketTracker().registerTime(player, (double)
                    packet.getEventloopStateTimings().getTimeFor(EventloopState.HONEY_HANDLE)
                    + packet.getEventloopStateTimings().getTimeFor(EventloopState.IN_BETWEEN)
                    + packet.getEventloopStateTimings().getTimeFor(EventloopState.DECODE)
            );
        }
    }

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> earlyPacketContainer, ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)  {
        try {
            if (earlyPacketContainer != null) {
                earlyPacketContainer.getEventloopStateTimings().startTimings(EventloopState.DECODE);
            }

            Object packet;
            try {
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    packet = HoneyPacketDecoder.decodePacket(player, packetHandler.getProtocolState(PacketBound.SERVER), packetHandler.getProtocolCodec(PacketBound.SERVER), byteBuf);
                } else {
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, byteBuf, list});

                    if (list.isEmpty()) {
                        if (earlyPacketContainer != null) {
                            stopTimings(earlyPacketContainer);
                        }
                        return;
                    }
                    packet = list.get(0);
                }
            } catch (Throwable ex) {
                if (HoneyPlugin.get().getLoggerService().isDebug()) {
                    ex.printStackTrace();
                }
                byteBuf.release();

                UnsafeChannelCloser.closeForcibly(ctx.channel());

                player.punished = true;
                if (earlyPacketContainer != null) {
                    stopTimings(earlyPacketContainer);
                }
                return;
            }

            byteBuf.release();

            if (packet == null) {
                if (earlyPacketContainer != null) {
                    stopTimings(earlyPacketContainer);
                }
                return;
            }

            if (earlyPacketContainer != null) {
                earlyPacketContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_HANDLE);

                if (earlyPacketContainer.getType() == PacketTypes.Play.Client.WINDOW_CONFIRMATION
                        || ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED
                                && player.isBukkitReady()
                                && player.bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {

                    ctx.fireChannelRead(packet);
                    stopTimings(earlyPacketContainer);
                    return;
                }
            }

            PacketContainer<? extends WrappedPacket<?>> finalPacketContainer = packetProcessor.read(ctx.channel(), player, packetHandler, packet, earlyPacketContainer);

            Runnable packetExecutorTask;
            if (!finalPacketContainer.isCancelled()) {
                packetExecutorTask = packetProcessor.getPacketExecutorTask(finalPacketContainer);
            } else {
                packetExecutorTask = null;
            }

            if (earlyPacketContainer != null) {
                stopTimings(earlyPacketContainer);
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

            if (earlyPacketContainer != null) {
                stopTimings(earlyPacketContainer);
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

        ctx.fireChannelRead(packetContainer.getRaw());

        Callback postAction = packetContainer.getPost();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        read(ctx, o);

        InboundPacketFilter.MARK.remove();
    }

    public void read(ChannelHandlerContext ctx, Object o) {
        if (!ctx.channel().isOpen()) {
            return;
        }
        if (!(o instanceof ByteBuf)) {
            if (!packetHandler.handleNonPacket(o)) {
                return;
            }
            ctx.fireChannelRead(ReferenceCountUtil.retain(o));
            return;
        }

        ByteBuf byteBuf = (ByteBuf) o;
        if (byteBuf.readableBytes() == 0) {
            ctx.fireChannelRead(byteBuf);
            return;
        }

        PacketContainer<? extends WrappedPacket<?>> packetContainer = InboundPacketFilter.MARK.get();
        if (packetContainer != null) {
            packetContainer.getEventloopStateTimings().stopTimings();
        }

        List<Object> nonRecycledList = new ArrayList<>();
        if (!ConfigurationOptions.AsyncDecoder.ENABLED
                || !player.isBukkitReady()
                || !PacketInterceptorResolver.hasEncoderHandler(ctx.pipeline())) {
            invokeRead(packetContainer, ctx, byteBuf, nonRecycledList);
        } else {
            ByteBuf wrapped = byteBuf.readBytes(byteBuf.readableBytes()).retain();
            byteBuf.release();

            asyncDecoder.decode((s) -> {
                if (player.isOnlineWeak() && !player.punished) {
                    invokeRead(packetContainer, ctx, wrapped, nonRecycledList);
                }
            });
        }
    }
}