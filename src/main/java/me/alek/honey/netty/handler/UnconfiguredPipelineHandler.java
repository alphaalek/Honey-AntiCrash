package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

public class UnconfiguredPipelineHandler {

    private static final ConstructorInvoker<Object> PACKET_DECODER_CONST = new ConstructorInvoker<>(NMS.getPacketDecoderClass(), NMS.getProtocolInfoClass());
    private static final ConstructorInvoker<Object> PACKET_ENCODER_CONST = new ConstructorInvoker<>(NMS.getPacketEncoderClass(), NMS.getProtocolInfoClass());

    public static class Inbound extends ChannelDuplexHandler {

        private boolean acceptInitial = true;

        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!this.acceptInitial && ((msg instanceof ByteBuf) || NMS.getPacketClass().isInstance(msg))) {
                ReferenceCountUtil.release(msg);
                throw new DecoderException("Pipeline has no inbound protocol configured, can't process packet " + msg);
            }
            this.acceptInitial = false;
            ctx.fireChannelRead(msg);
        }

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            Consumer<ChannelHandlerContext> task = asConsumer(msg);
            if (task != null) {
                try {
                    task.accept(ctx);
                    ReferenceCountUtil.release(msg);
                    promise.setSuccess();
                    return;
                } catch (Throwable th) {
                    ReferenceCountUtil.release(msg);
                    throw th;
                }
            }
            ctx.write(msg, promise);
        }
    }

    public static class Outbound extends ChannelOutboundHandlerAdapter {

        private boolean acceptInitial = true;

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (!this.acceptInitial && NMS.getPacketClass().isInstance(msg)) {
                ReferenceCountUtil.release(msg);
                throw new EncoderException("Pipeline has no outbound protocol configured, can't process packet " + msg);
            }
            this.acceptInitial = true;
            Consumer<ChannelHandlerContext> task = asConsumer(msg);
            if (task != null) {
                try {
                    task.accept(ctx);
                    ReferenceCountUtil.release(msg);
                    promise.setSuccess();
                    return;
                } catch (Throwable th) {
                    ReferenceCountUtil.release(msg);
                    throw th;
                }
            }
            ctx.write(msg, promise);
        }
    }

    private static Consumer<ChannelHandlerContext> asConsumer(Object msg) {
        try {
            MethodInvoker<Void> consumerMethod = Reflection.getMethod(msg.getClass(),
                    m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(ChannelHandlerContext.class));
            return ctx -> consumerMethod.invoke(msg, ctx);
        } catch (Exception ex) {
            return null;
        }
    }

    @FunctionalInterface
    public interface InboundConfigurationTask {
        void run(ChannelHandlerContext ctx);

        default InboundConfigurationTask andThen(InboundConfigurationTask taskToWrap) {
            return ctx -> {
                run(ctx);
                taskToWrap.run(ctx);
            };
        }
    }

    @FunctionalInterface
    public interface OutboundConfigurationTask {
        void run(ChannelHandlerContext ctx);

        default OutboundConfigurationTask andThen(OutboundConfigurationTask taskToWrap) {
            return ctx -> {
                run(ctx);
                taskToWrap.run(ctx);
            };
        }
    }

    public static InboundConfigurationTask setupInboundProtocolTask(PacketState state) {
        return setupInboundHandler((ChannelInboundHandler) PACKET_DECODER_CONST.invoke(CodecFactory.getProtocolInfo1_20_5(PacketBound.SERVER, state)));
    }

    private static InboundConfigurationTask setupInboundHandler(ChannelInboundHandler handler) {
        return ctx -> {
            ctx.pipeline().replace(ctx.name(), MinecraftHandlers.DECODER, handler);
            ctx.channel().config().setAutoRead(true);
        };
    }

    public static OutboundConfigurationTask setupOutboundProtocolTask(PacketState state) {
        return setupOutboundHandler((ChannelOutboundHandler) PACKET_ENCODER_CONST.invoke(CodecFactory.getProtocolInfo1_20_5(PacketBound.CLIENT, state)));
    }

    private static OutboundConfigurationTask setupOutboundHandler(ChannelOutboundHandler $$0) {
        return ctx -> ctx.pipeline().replace(ctx.name(), MinecraftHandlers.ENCODER, $$0);
    }
}
