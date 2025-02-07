package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.via.ViaHandlers;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.handler.wrappers.WrappedChannelInboundHandler;
import me.alek.honey.netty.handler.wrappers.WrappedChannelOutboundHandler;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            toggleResolving(pipeline.channel(), PacketBound.SERVER, true);
            decoderPosition = setupInterceptors(pipeline, false);
            toggleResolving(pipeline.channel(), PacketBound.SERVER, false);
        }


        return decoderPosition;
    }

    public static String getEncoderPosition(ChannelPipeline pipeline) {
        if (encoderPosition == null) {
            toggleResolving(pipeline.channel(), PacketBound.CLIENT, true);
            encoderPosition = setupInterceptors(pipeline, true);
            toggleResolving(pipeline.channel(), PacketBound.CLIENT, false);
        }

        return encoderPosition;
    }

    private static void toggleResolving(Channel channel, PacketBound bound, boolean state) {
        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(channel).resolve(bound, state);
        } catch (Exception ex) {
            ChannelCloseInspection.close(channel);
        }
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
        pipeline.channel().config().setAutoRead(false);
        ByteBuf buf = createByteBuf(PacketTypes.Play.Client.KEEP_ALIVE, serializer -> {
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2)) {
                serializer.writeLong(System.currentTimeMillis());
            } else {
                serializer.writeVarInt((int) System.currentTimeMillis());
            }
        });

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
                        position.set(prev.get() != null ? prev.get() : name);
                        return;
                    }
                    try {
                        prev.set(name);

                        ChannelHandler intercepted = ctx.pipeline().get(name);
                        if (intercepted != null && !name.equals(ViaHandlers.DECODER) && !name.equals(MinecraftHandlers.FLOW_CONTROL) && !name.startsWith("pe-decoder")) {
                            ((ChannelInboundHandler) intercepted).channelRead(ctx, buf.copy());
                        } else {
                            ctx.fireChannelRead(o);
                        }
                    } catch (Exception ignored) {
                        // to do nothing
                    }
                }
            }
        );

        String start = pipeline.get(MinecraftHandlers.DECOMPRESS) != null ? MinecraftHandlers.DECOMPRESS : MinecraftHandlers.SPLITTER;
        for (String name : pipeline.names()) {
            pipeline.remove(name);
            handlers.remove(name);

            if (name.equals(start + INTERCEPTOR)) {
                break;
            }
        }

        pipeline.fireChannelRead(buf);
        pipeline.channel().config().setAutoRead(true);

        return handlers;
    }

    private static List<String> setupEncoderPosition(ChannelPipeline pipeline, AtomicReference<String> pos) {
        pipeline.channel().config().setAutoRead(false);
        ByteBuf buf = createByteBuf(PacketTypes.Play.Server.KEEP_ALIVE, serializer -> {
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12_2)) {
                serializer.writeLong(System.currentTimeMillis());
            } else {
                serializer.writeVarInt((int) System.currentTimeMillis());
            }
        });
        Object raw = PacketFactory.createPacket(PacketTypes.Play.Server.KEEP_ALIVE, buf);

        AtomicReference<String> prev = new AtomicReference<>();
        List<String> handlers = setupInterceptorEnv(pipeline, false, handler -> handler instanceof ChannelOutboundHandler,
            handler -> new WrappedChannelOutboundHandler((ChannelOutboundHandler) handler) {
                @Override
                public void write(ChannelHandlerContext ctx, Object o, ChannelPromise promise) {
                    if (pos.get() != null) {
                        return;
                    }

                    String name = ctx.name().substring(0, ctx.name().length() - INTERCEPTOR.length());
                    if (o instanceof ByteBuf) {
                        pos.set(prev.get() != null ? prev.get() : name);
                        return;
                    }

                    try {
                        prev.set(name);

                        ChannelHandler intercepted = ctx.pipeline().get(name);
                        if (intercepted != null && !name.equals(ViaHandlers.ENCODER)) {
                            ((ChannelOutboundHandler) intercepted).write(ctx, raw, promise);
                        } else {
                            ctx.write(raw, promise);
                        }
                    } catch (Exception ignored) {
                        // to do nothing
                    }
                }
            }
        );

        pipeline.channel().writeAndFlush(raw);
        pipeline.channel().config().setAutoRead(true);

        return handlers;
    }

    private static List<String> setupInterceptorEnv(ChannelPipeline pipeline, boolean asc, Predicate<ChannelHandler> accept, Function<ChannelHandler, ChannelHandler> handlerSupplier) {
        List<String> wrappedHandlers = new ArrayList<>();
        String prevName = null;

        List<String> names = pipeline.names().stream()
                .filter(ch -> pipeline.context(ch) != null) // filter head and tail context
                .collect(Collectors.toList());
        if (!asc) {
            Collections.reverse(names);
        }

        for (String name : names) {
            ChannelHandlerContext ctx = pipeline.context(name);
            if (!accept.test(ctx.handler())) {
                continue;
            }

            String interceptorName = name + INTERCEPTOR;

            String prevInterceptor = pipeline.get(prevName + INTERCEPTOR) != null ? prevName + INTERCEPTOR : prevName;
            wrappedHandlers.add(interceptorName);
            ChannelHandler interceptor = handlerSupplier.apply(ctx.handler());
            try {
                if (prevName == null) {
                    if (asc) {
                        pipeline.addFirst(interceptorName, interceptor);
                    } else {
                        pipeline.addLast(interceptorName, interceptor);
                    }
                } else {
                    if (asc) {
                        pipeline.addAfter(prevInterceptor, interceptorName, interceptor);
                    } else {
                        pipeline.addBefore(prevInterceptor, interceptorName, interceptor);
                    }
                }
            } catch (Exception ignored) {
                // to do nothing
            }
            prevName = name;
        }

        return wrappedHandlers;
    }

    private static ByteBuf createByteBuf(PacketType packetType, Consumer<PacketDataSerializer> write) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        serializer.writeByte(packetType.getMinecraftID().get());
        write.accept(serializer);
        return serializer;
    }
}
