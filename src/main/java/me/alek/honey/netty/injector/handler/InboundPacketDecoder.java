package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

public class InboundPacketDecoder extends ChannelInboundHandlerAdapter {

    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private static final Queue<Object> serverThreadExecutionQueue = ((ServerThreadHandleTracker)HoneyPlugin.get().getServerThreadHandleTracker()).getExecutionQueue();
    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = HoneyPacketHandler.getPacketSendMarker();

    private final AsyncDecoder asyncDecoder;
    private final HoneyPacketHandler packetHandler;
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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!HoneyPacketHandler.onExceptionCaught(cause)) {
            super.exceptionCaught(ctx, cause);
        }
    }

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> packetContainer, ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)  {
        try {
            if (packetContainer != null) packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);

            // explicit set variable, so Proguard recognizes the method
            Object empty = decodeInvoker.invokeWithArguments(new Object[]{decoder, ctx, byteBuf, list});
            byteBuf.release();

            if (list.isEmpty()) return;
            if (packetContainer == null) {
                ctx.fireChannelRead(list.get(0));
                return;
            }

            packetContainer.getEventloopStateTimings().stopTimings();
            HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(asyncDecoder.getPlayer(), (double) packetContainer.getEventloopStateTimings().getTimeFor(EventloopState.SERVER_DECODE));

            PacketContainer<? extends WrappedPacket<?>> finalPacketContainer = packetProcessor.read(ctx.channel(), asyncDecoder.getPlayer(), list.get(0), packetContainer.getTime(), packetContainer);

            Runnable packetExecutorTask = null;
            if (ConfigurationOptions.AsyncDecoder.ENABLED) {
                packetExecutorTask = packetProcessor.getPacketExecutorTask(finalPacketContainer);
            }
            Runnable finalPacketExecutorTask = packetExecutorTask;

            ctx.channel().eventLoop().execute(() -> {
                if (packetHandler.read0(finalPacketContainer)) {

                    if (finalPacketExecutorTask != null) {
                        serverThreadExecutionQueue.add(finalPacketExecutorTask);
                        return;
                    }

                    ctx.fireChannelRead(finalPacketContainer.getHandle());
                }

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

        PacketContainer<? extends WrappedPacket<?>> packetContainer = InboundPacketFilter.getEarlyInitializedPacket();

        List<Object> nonRecycledList = new ArrayList<>();
        if (!ConfigurationOptions.AsyncDecoder.ENABLED) {
            invokeRead(packetContainer, ctx, byteBuf, nonRecycledList);
            return;
        }

        ByteBuf wrappedBuffer = Unpooled.buffer().writeBytes(byteBuf).retain();
        byteBuf.release();

        asyncDecoder.decode((s) -> {
            if (asyncDecoder.getPlayer() == null || asyncDecoder.getPlayer().isOnline()) {
                invokeRead(packetContainer, ctx, wrappedBuffer, nonRecycledList);
            }
        });
    }
}
