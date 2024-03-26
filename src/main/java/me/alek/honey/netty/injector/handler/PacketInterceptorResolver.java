package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.wrappers.WrappedChannelInboundHandler;
import me.alek.honey.netty.injector.handler.wrappers.WrappedChannelOutboundHandler;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutPosition;
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

    public static boolean isDecoderPositionResolved() {
        return decoderPosition != null;
    }

    public static boolean isEncoderPositionResolved() {
        return encoderPosition != null;
    }

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

        for (String handler : handlers) {
            pipeline.remove(handler);
        }

        return position.get();
    }

    private static List<String> setupDecoderInterceptors(ChannelPipeline pipeline, AtomicReference<String> position) {
        ByteBuf byteBuf = createByteBufPacket(pipeline.channel(), PacketBound.SERVER, "PacketPlayInFlying", serializer -> serializer.writeBoolean(false));

        AtomicReference<String> prev = new AtomicReference<>();
        List<String> handlers = setupInterceptorEnv(pipeline, true, handler -> handler instanceof ChannelInboundHandler,
                handler -> new WrappedChannelInboundHandler((ChannelInboundHandler) handler) {

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
                    if (intercepted != null && !name.equals("via-decoder"))
                        ((ChannelInboundHandler)intercepted).channelRead(ctx, byteBuf.copy());
                    else
                        ctx.fireChannelRead(o);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) {
            }

        });

        String start = pipeline.get("decompress") != null ? "decompress" : "splitter";
        for (String name : pipeline.names()) {
            if (pipeline.get(name) != null) pipeline.remove(name);
            handlers.remove(name);

            if (name.equals(start + INTERCEPTOR)) {
                break;
            }
        }

        pipeline.fireChannelRead(byteBuf);

        return handlers;
    }

    private static List<String> setupEncoderPosition(ChannelPipeline pipeline, AtomicReference<String> position) {
        ByteBuf buf = createByteBufPacket(pipeline.channel(), PacketBound.CLIENT, "PacketPlayOutPosition", (serializer) -> {
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

        AtomicReference<String> prev = new AtomicReference<>();
        List<String> handlers = setupInterceptorEnv(pipeline, false, handler -> handler instanceof ChannelOutboundHandler,
                handler -> new WrappedChannelOutboundHandler((ChannelOutboundHandler) handler) {
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

    private static List<String> setupInterceptorEnv(ChannelPipeline pipeline, boolean asc, Predicate<ChannelHandler> accept, Function<ChannelHandler, ChannelHandler> handlerSupplier) {
        List<String> wrappedHandlers = new ArrayList<>();
        String prevName = null;

        List<String> names = pipeline.names();
        Collections.reverse(names);
        for (String name : names) {

            ChannelHandler handler;
            if (pipeline.context(name) == null || !accept.test(handler = pipeline.context(name).handler())) {
                continue;
            }

            String prevInterceptor = pipeline.get(prevName + INTERCEPTOR) != null ? prevName + INTERCEPTOR : prevName;
            wrappedHandlers.add(name + INTERCEPTOR);
            ChannelHandler interceptor = handlerSupplier.apply(handler);
            try {
                if (prevName == null) {
                    if (asc) {
                        pipeline.addFirst(name + INTERCEPTOR, interceptor);
                    } else {
                        pipeline.addLast(name + INTERCEPTOR, interceptor);
                    }
                }
                if (prevName != null) {
                    pipeline.addBefore(prevInterceptor, name + INTERCEPTOR, interceptor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            prevName = name;
        }
        return wrappedHandlers;
    }

    private static ByteBuf createByteBufPacket(Channel channel, PacketBound bound, String name, Consumer<ByteBuf> write) {
        AttributeKey<Object> key;
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2))
            key = ReflectStructureCache.acquireStructure(NMS.getNetworkManagerClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get("packet_handler")).readField(0);
        else
            key = ReflectStructureCache.acquireStructure(NMS.getPacketDecoderClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(bound.getProtocolHandlerName()).readField(0);
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
                packet = protocol.createPacket(bound, id, new WrappedPacketDataSerializer(serializer));
            } catch (Exception ignored) {
            }

            if (packet != null && packet.getClass().getSimpleName().equals(name)) {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(id);
                write.accept(buf);

                return buf;
            }
            serializer.readerIndex(0);
        }
    }
}
