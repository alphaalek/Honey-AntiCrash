package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.wrappers.WrappedChannelInboundHandler;
import me.alek.honey.netty.injector.handler.wrappers.WrappedChannelOutboundHandler;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

public class InboundPacketDecoder extends ChannelInboundHandlerAdapter {

    private static final String INTERCEPTOR = "-interceptor";
    private static volatile String decoderPosition;

    public static String getDecoderPosition(ChannelPipeline pipeline) {
        if (decoderPosition == null)
            decoderPosition = setupInterceptors(pipeline, false);
        return decoderPosition;
    }

    public static String setupInterceptors(ChannelPipeline pipeline, boolean isEncoder) {
        AtomicReference<String> position = new AtomicReference<>();
        List<String> handlers;
        if (isEncoder)
            handlers = setupEncoderPosition(pipeline, position);
        else
            handlers = setupDecoderInterceptors(pipeline, position);

        Bukkit.getLogger().info("handlers " + handlers);
        Bukkit.getLogger().info("position " + position);

        for (String handler : handlers) {
            pipeline.remove(handler);
        }

        return position.get();
    }

    private static List<String> setupDecoderInterceptors(ChannelPipeline pipeline, AtomicReference<String> position) {

        AtomicReference<String> prev = new AtomicReference<>();
        ByteBuf byteBuf = createPositionByteBufPacket(pipeline.channel());
        List<String> handlers = setupInterceptorEnv(pipeline, false, "decompress", handler -> handler instanceof ChannelInboundHandler, handler -> new WrappedChannelInboundHandler((ChannelInboundHandler) handler) {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) {
                if (!(o instanceof ByteBuf)) {
                    position.set(prev.get());
                    return;
                }
                try {
                    String name = ctx.name().substring(0, ctx.name().length() - INTERCEPTOR.length());
                    prev.set(name);

                    ChannelHandler intercepted = ctx.pipeline().get(name);
                    ((ChannelInboundHandler)intercepted).channelRead(ctx, byteBuf.copy());

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        pipeline.context("decompress").fireChannelRead(byteBuf);

        return handlers;
    }

    private static List<String> setupEncoderPosition(ChannelPipeline pipeline, AtomicReference<String> position) { // only called once for each server start

        AtomicReference<String> prev = new AtomicReference<>();
        PacketContainer<WrappedPlayOutPosition> packet = PacketFactory.<WrappedPlayOutPosition>createNewPacket(PacketType.Play.Server.POSITION, null, pipeline.channel(), null).getContainer();
        packet.getDoubles()
                .writeField(0, Double.MAX_VALUE)
                .writeField(1, Double.MAX_VALUE)
                .writeField(2, Double.MAX_VALUE);
        packet.getBooleans()
                .writeField(0, false);
        List<String> handlers = setupInterceptorEnv(pipeline, true, null, handler -> handler instanceof ChannelOutboundHandler, handler -> new WrappedChannelOutboundHandler((ChannelOutboundHandler) handler) {
            @Override
            public void write(ChannelHandlerContext ctx, Object o, ChannelPromise promise) throws Exception {
                String name = ctx.name().substring(0, ctx.name().length() - INTERCEPTOR.length());
                if (o instanceof ByteBuf) {
                    position.set(prev.get() != null ? prev.get() : name);
                    return;
                }
                try {
                    prev.set(name);

                    ChannelHandler intercepted = ctx.pipeline().get(name);
                    ((ChannelOutboundHandler)intercepted).write(ctx, packet.getHandle(), promise);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        pipeline.channel().writeAndFlush(packet.getHandle());
        return handlers;
    }

    private static List<String> setupInterceptorEnv(ChannelPipeline pipeline, boolean desc, String start, Predicate<ChannelHandler> accept, Function<ChannelHandler, ChannelHandler> handlerSupplier) {
        boolean started = false;

        List<String> wrappedHandlers = new ArrayList<>();
        String prevName = null;

        List<String> names = pipeline.names();
        if (desc) Collections.reverse(names);
        for (String name : names) {

            ChannelHandler handler;
            if (pipeline.context(name) == null || !accept.test(handler = pipeline.context(name).handler())) {
                continue;
            }

            String prevInterceptor = pipeline.get(prevName + INTERCEPTOR) != null ? prevName + INTERCEPTOR : prevName;
            if (started) {
                wrappedHandlers.add(name + INTERCEPTOR);

                if (desc)
                    pipeline.addBefore(prevInterceptor, name + INTERCEPTOR, handlerSupplier.apply(handler));
                else
                    pipeline.addAfter(prevInterceptor, name + INTERCEPTOR, handlerSupplier.apply(handler));
            }
            if (start == null || name.equals(start)) started = true;
            prevName = name;
        }
        return wrappedHandlers;
    }

    private static ByteBuf createPositionByteBufPacket(Channel channel) {
        AttributeKey<Object> key;
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2))
            key = ReflectStructureCache.acquireStructure(NMSUtils.getNetworkManagerClass())
                .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(PacketLibrary.get().getServerInjector().getNetworkManagerFor(channel)).readField(0);
        else
            key = ReflectStructureCache.acquireStructure(NMSUtils.getPacketDecoderClass())
                .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get("decoder")).readField(0);
        if (key == null) {
            throw new UnsupportedOperationException("Can't create keep alive packet for channel that is not registered for a player.");
        }

        WrappedEnumProtocol protocol = Converters.getEnumProtocolConverter().convertDelegate(channel.attr(key).get());

        ByteBuf serializer = Unpooled.buffer();
        serializer.writeBytes(new byte[64]);
        serializer.writeDouble(Double.MAX_VALUE);
        serializer.writeDouble(Double.MAX_VALUE);
        serializer.writeDouble(Double.MAX_VALUE);
        serializer.writeBoolean(false);

        for (int id = 0; ; id++) {
            Object packet = null;
            try {
                packet = protocol.createPacket(PacketBound.SERVER, id, new WrappedPacketDataSerializer(serializer));
            } catch (Exception ignored) {
            }

            if (packet != null && packet.getClass().getSimpleName().equals("PacketPlayInPosition")) {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(id);

                buf.writeDouble(Double.MAX_VALUE);
                buf.writeDouble(Double.MAX_VALUE);
                buf.writeDouble(Double.MAX_VALUE);
                buf.writeBoolean(false);

                return buf;
            }
            serializer.readerIndex(0);
        }
    }

    private final AsyncDecoder asyncDecoder;
    private Method decodeInvoker;
    private ChannelHandler decoder;

    public InboundPacketDecoder(AsyncDecoder asyncDecoder, ChannelPipeline pipeline) {
        this.asyncDecoder = asyncDecoder;
        setupMethodInvoke(pipeline);
    }

    private void setupMethodInvoke(ChannelPipeline pipeline) {
        try {
            decoder = pipeline.get(decoderPosition);

            decodeInvoker = decoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            decodeInvoker.setAccessible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to setup packet decoder environment!");
        }
    }

    private void invokeRead(PacketContainer<? extends WrappedPacket<?>> packetContainer, ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)  {
        try {
            if (packetContainer != null) packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
            decodeInvoker.invoke(decoder, ctx, byteBuf, list);
            if (packetContainer != null) packetContainer.getEventloopStateTimings().stopTimings();

            byteBuf.release();
            for (Object element : list) {
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

        PacketContainer<? extends WrappedPacket<?>> packetContainer = InboundPacketFilter.getEarlyPacketInitializer().get();
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
