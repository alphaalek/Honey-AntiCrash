package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.EmulatorCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.handler.wrappers.WrappedChannelInboundHandler;
import me.alek.honey.netty.handler.wrappers.WrappedChannelOutboundHandler;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutPosition;

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

    public static boolean hasDecoderHandler(ChannelPipeline pipeline) {
        return isDecoderPositionResolved() && pipeline.get(decoderPosition) != null;
    }

    public static boolean hasEncoderHandler(ChannelPipeline pipeline) {
        return isEncoderPositionResolved() && pipeline.get(encoderPosition) != null;
    }

    public static String getDecoderPosition(ChannelPipeline pipeline) {
        if (decoderPosition == null) {
            decoderPosition = setupInterceptors(pipeline, false);
        }

        return decoderPosition;
    }

    public static String getEncoderPosition(ChannelPipeline pipeline) {
        if (encoderPosition == null) {
            encoderPosition = setupInterceptors(pipeline, true);
        }

        return encoderPosition;
    }

    public static String setupInterceptors(ChannelPipeline pipeline, boolean isEncoder) {
        AtomicReference<String> position = new AtomicReference<>();
        List<String> handlers;
        if (isEncoder) {
            handlers = setupEncoderPosition(pipeline, position);
        } else {
            handlers = setupDecoderInterceptors(pipeline, position);
        }

        for (String handler : handlers) {
            pipeline.remove(handler);
        }

        return position.get();
    }

    private static List<String> setupDecoderInterceptors(ChannelPipeline pipeline, AtomicReference<String> position) {
        ByteBuf byteBuf = createByteBuf(EmulatorCodec.LEGACY ? PacketTypes.Play.Client.FLYING : PacketTypes.Play.Client.GROUND,
                serializer -> serializer.writeBoolean(false));

        AtomicReference<String> prev = new AtomicReference<>();
        List<String> handlers = setupInterceptorEnv(pipeline, true, handler -> handler instanceof ChannelInboundHandler,
                handler -> new WrappedChannelInboundHandler((ChannelInboundHandler) handler) {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) {
                if (position.get() != null) {
                    return;
                }

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

                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) {
            }

        });

        String start = pipeline.get(MinecraftHandlers.DECOMPRESS) != null ? MinecraftHandlers.DECOMPRESS : MinecraftHandlers.SPLITTER;
        for (String name : pipeline.names()) {
            pipeline.remove(name);
            handlers.remove(name);

            if (name.equals(start + INTERCEPTOR)) {
                break;
            }
        }

        pipeline.fireChannelRead(byteBuf);

        return handlers;
    }

    private static List<String> setupEncoderPosition(ChannelPipeline pipeline, AtomicReference<String> position) {
        ByteBuf buf = createByteBuf(PacketTypes.Play.Server.POSITION, serializer -> {
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeDouble(Double.MAX_VALUE);
            serializer.writeFloat(Float.MAX_VALUE);
            serializer.writeFloat(Float.MAX_VALUE);
            serializer.writeShort(0);
            serializer.writeInt(0);
        }).copy();
        PacketContainer<WrappedPlayOutPosition> packet = PacketFactory.<WrappedPlayOutPosition>createWrappedPacket(
                PacketTypes.Play.Server.POSITION, PacketFactory.createRawPacket(PacketTypes.Play.Server.POSITION, buf), null, pipeline.channel(), null).getContainer();

        AtomicReference<String> prev = new AtomicReference<>();
        List<String> handlers = setupInterceptorEnv(pipeline, false, handler -> handler instanceof ChannelOutboundHandler,
                handler -> new WrappedChannelOutboundHandler((ChannelOutboundHandler) handler) {
            @Override
            public void write(ChannelHandlerContext ctx, Object o, ChannelPromise promise) {
                if (position.get() != null) {
                    return;
                }

                String name = ctx.name().substring(0, ctx.name().length() - INTERCEPTOR.length());
                if (o instanceof ByteBuf) {
                    position.set(prev.get() != null ? prev.get() : name);
                    return;
                }
                try {
                    prev.set(name);

                    ChannelHandler intercepted = ctx.pipeline().get(name);
                    ((ChannelOutboundHandler)intercepted).write(ctx, packet.getHandle(), promise);

                } catch (Exception ignored) {
                    ignored.printStackTrace();
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
            if (pipeline.context(name) == null
                    || !accept.test(handler = pipeline.context(name).handler())) {
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
                } else {
                    pipeline.addBefore(prevInterceptor, name + INTERCEPTOR, interceptor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            prevName = name;
        }
        return wrappedHandlers;
    }

    private static ByteBuf createByteBuf(PacketType packetType, Consumer<ByteBuf> write) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        serializer.writeByte(packetType.getMinecraftID().get());
        write.accept(serializer);
        return serializer;
    }
}
