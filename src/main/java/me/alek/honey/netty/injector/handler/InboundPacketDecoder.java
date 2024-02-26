package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.*;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class InboundPacketDecoder extends ChannelInboundHandlerAdapter {

    private static final String INTERCEPTOR = "_interceptor";
    private static volatile String decoderPosition;

    public static String getObjectDecoderPosition(ChannelPipeline pipeline) {
        if (decoderPosition == null)
            resolveObjectDecoderPosition(pipeline);
        return decoderPosition;
    }

    private static void resolveObjectDecoderPosition(ChannelPipeline pipeline) { // only called once for each server start
        boolean started = false;
        boolean ended = false;
        List<String> wrappedHandlers = new ArrayList<>();
        String prevName = null;
        for (String name : pipeline.names()) {

            ChannelHandler handler = pipeline.context(name).handler();
            if (!(handler instanceof ChannelInboundHandler)) {
                continue;
            }

            if (prevName != null && prevName.equals("decoder")) ended = true;

            String prevInterceptor = pipeline.get(prevName + INTERCEPTOR) != null ? prevName + INTERCEPTOR : prevName;
            if (started) {
                wrappedHandlers.add(name + INTERCEPTOR);
                String prevNameFinal = prevName;
                pipeline.addAfter(prevInterceptor, name + INTERCEPTOR, new WrappedChannelInboundHandler((ChannelInboundHandler) handler) {
                    @Override
                    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
                        if (!(o instanceof ByteBuf)) {
                            decoderPosition = prevNameFinal;
                            return;
                        }
                        ((ChannelInboundHandler)pipeline.context(name).handler()).channelRead(channelHandlerContext, o);
                    }
                });
            }
            if (ended) break;

            if (name.equals("decompress")) started = true;
            prevName = name;
        }
        pipeline.context("decompress").fireChannelRead(createPositionByteBufPacket(pipeline.channel()));

        for (String name : wrappedHandlers) {
            pipeline.remove(name);
        }
    }

    private static ByteBuf createPositionByteBufPacket(Channel channel) {
        @SuppressWarnings("unchecked")
        AttributeKey<Object> key = ReflectStructureCache.acquireStructure(NMSUtils.getNetworkManagerClass())
                .withType(AttributeKey.class).withTarget(PacketLibrary.get().getServerInjector().getNetworkManagerFor(channel)).readField(0);
        if (key == null) {
            throw new UnsupportedOperationException("Can't create keep alive packet for channel that is not registered for a player.");
        }

        WrappedEnumProtocol protocol = Converters.getEnumProtocolConverter().convertDelegate(channel.attr(key).get());
        if (!protocol.getProtocol().equalsIgnoreCase("play")) {
            throw new UnsupportedOperationException("Can't create keep alive packet because the current gamestate protocol for this channel is not PLAY.");
        }

        WrappedPacketDataSerializer dataSerializer = new WrappedPacketDataSerializer(Unpooled.buffer());
        dataSerializer.writeDouble(1);
        dataSerializer.writeDouble(1);
        dataSerializer.writeDouble(1);
        dataSerializer.writeBoolean(false);

        for (int id = 0; ; id++) {
            Object packet = protocol.createLegacyPacket(PacketBound.SERVER, id);
            if (packet == null) return null;
            if (packet.getClass().getSimpleName().equals("PacketPlayInPosition")) {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(id);
                buf.writeDouble(1);
                buf.writeDouble(1);
                buf.writeDouble(1);
                buf.writeBoolean(false);

                return buf;
            }
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

    public void read(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        List<Object> nonRecycledList = new ArrayList<>();

        if (!ConfigurationOptions.AsyncDecoder.ENABLED) {
            decodeInvoker.invoke(decoder, ctx, byteBuf.retain(), nonRecycledList);
            for (Object element : nonRecycledList) {
                ctx.fireChannelRead(element);
            }
            return;
        }

        ByteBuf wrappedBuffer = Unpooled.buffer().writeBytes(byteBuf).retain();
        byteBuf.release();

        PacketContainer<? extends WrappedPacket<?>> packetContainer = InboundPacketFilter.getEarlyPacketInitializer().get();
        asyncDecoder.decode((s) -> {
            if (asyncDecoder.getPlayer().isOnline()) {
                try {
                    if (packetContainer != null) packetContainer.getEventloopStateTimings().startTimings(EventloopState.SERVER_DECODE);
                    decodeInvoker.invoke(decoder, ctx, wrappedBuffer, nonRecycledList);
                    if (packetContainer != null) packetContainer.getEventloopStateTimings().stopTimings();

                    wrappedBuffer.release();
                    for (Object element : nonRecycledList) {
                        ctx.fireChannelRead(element);
                    }

                } catch (Exception ex) {
                    try {
                        super.exceptionCaught(ctx, ex);
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) {
        if (!(packet instanceof ByteBuf)) return;

        try {
            read(ctx, (ByteBuf) packet);

        } catch (Exception ex) {
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {

            }
        }
    }

}
