package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.wrappers.WrappedChannelInboundHandler;
import me.alek.honey.netty.injector.handler.wrappers.WrappedChannelOutboundHandler;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PacketInterceptorResolver {

    private static final String INTERCEPTOR = "-interceptor";
    private static volatile String decoderPosition;
    private static volatile String encoderPosition;

    public static String getDecoderPosition(ChannelPipeline pipeline) {
        if (decoderPosition == null)
            decoderPosition = setupInterceptors(pipeline, false);
        return decoderPosition;
    }

    public static String getEncoderPosition(ChannelPipeline pipeline) {
        if (encoderPosition == null)
            encoderPosition = setupInterceptors(pipeline, true);
        return encoderPosition;
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
        ByteBuf byteBuf = createPositionByteBufPacket(pipeline.channel(), (serializer) -> {
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeBoolean(false);
        });
        List<String> handlers = setupInterceptorEnv(pipeline, false, "decompress", handler -> handler instanceof ChannelInboundHandler, handler -> new WrappedChannelInboundHandler((ChannelInboundHandler) handler) {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) {
                String name = ctx.name().substring(0, ctx.name().length() - INTERCEPTOR.length());
                if (!(o instanceof ByteBuf)) {
                    position.set(prev.get());
                    return;
                }
                try {
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
        ByteBuf buf = createPositionByteBufPacket(pipeline.channel(), (serializer) -> {
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeFloat(Float.MAX_VALUE);
            serializer.writeFloat(Float.MAX_VALUE);
            serializer.writeShort(0);
            serializer.writeInt(0);
        }).copy();

        PacketContainer<WrappedPlayOutPosition> packet = PacketFactory.<WrappedPlayOutPosition>createWrappedPacket(
                PacketType.Play.Server.POSITION, PacketFactory.createRawPacket(PacketType.Play.Server.POSITION, buf), null, pipeline.channel(), null).getContainer();
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

    private static ByteBuf createPositionByteBufPacket(Channel channel, Consumer<ByteBuf> write) {
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
        write.accept(serializer);

        for (int id = 0; ; id++) {
            Object packet = null;
            try {
                packet = protocol.createPacket(PacketBound.SERVER, id, new WrappedPacketDataSerializer(serializer));
            } catch (Exception ignored) {
            }

            if (packet != null && packet.getClass().getSimpleName().equals("PacketPlayInPosition")) {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(id);
                write.accept(buf);

                return buf;
            }
            serializer.readerIndex(0);
        }
    }
}
