package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InboundPacketDecoder extends ChannelInboundHandlerAdapter {

    public final Queue<PacketContainer<? extends WrappedPacket<?>>> earlyInitializedPacketQueue = new LinkedList<>();
    private final AsyncDecoder asyncDecoder;
    private Method decodeInvoker;
    private ChannelHandler decoder;

    public InboundPacketDecoder(AsyncDecoder asyncDecoder, ChannelPipeline pipeline) {
        this.asyncDecoder = asyncDecoder;
        setupMethodInvoke(pipeline);
    }

    private void setupMethodInvoke(ChannelPipeline pipeline) {
        try {
            decoder = pipeline.get(PacketInterceptorResolver.getDecoderPosition(pipeline));

            decodeInvoker = decoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            decodeInvoker.setAccessible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet decoder environment!");
        }
    }

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> packetContainer, ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)  {
        try {
            earlyInitializedPacketQueue.add(packetContainer);

            if (packetContainer != null) packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
            decodeInvoker.invoke(decoder, ctx, byteBuf, list);
            if (packetContainer != null) packetContainer.getEventloopStateTimings().stopTimings();

            byteBuf.release();
            for (Object element : list) {
                if (packetContainer != null && packetContainer.getHandle() == null) packetContainer.setHandle(element);
                ctx.fireChannelRead(element);
            }
        } catch (Exception ex) {
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) {
        if (!(o instanceof ByteBuf)) return;

        PacketContainer<? extends WrappedPacket<?>> packetContainer = InboundPacketFilter.getEarlyInitializedPacket();
        ByteBuf byteBuf = (ByteBuf) o;

        List<Object> nonRecycledList = new ArrayList<>();

        if (!ConfigurationOptions.AsyncDecoder.ENABLED) {
            invokeRead(packetContainer, ctx, byteBuf, nonRecycledList);
            return;
        }

        ByteBuf wrappedBuffer = Unpooled.buffer().writeBytes(byteBuf).retain();
        byteBuf.release();

        asyncDecoder.decode((s) -> {
            if (asyncDecoder.getPlayer().isOnline()) {
                invokeRead(packetContainer, ctx, wrappedBuffer, nonRecycledList);
                // Bukkit.getLogger().info("Done read " + nonRecycledList);
            }
        });
    }
}
