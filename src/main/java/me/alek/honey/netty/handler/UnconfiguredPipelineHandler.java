package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;

import java.util.function.Consumer;

public class UnconfiguredPipelineHandler {

    public static class Inbound extends ChannelDuplexHandler {

        private boolean acceptInitial = true;

        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!this.acceptInitial && ((msg instanceof ByteBuf) || MinecraftReflection.getPacketClass().isInstance(msg))) {
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
                    if (!(msg instanceof InboundConfigurationTask)) {
                        relocate(ctx.channel(), PacketBound.SERVER);
                    }
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
            if (!this.acceptInitial && MinecraftReflection.getPacketClass().isInstance(msg)) {
                ReferenceCountUtil.release(msg);
                throw new EncoderException("Pipeline has no outbound protocol configured, can't process packet " + msg);
            }
            this.acceptInitial = true;
            Consumer<ChannelHandlerContext> task = asConsumer(msg);
            if (task != null) {
                try {
                    task.accept(ctx);
                    if (!(msg instanceof OutboundConfigurationTask)) {
                        relocate(ctx.channel(), PacketBound.CLIENT);
                    }
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
        return setupInboundHandler(ModernCodecHandlerFactory.createDecodeHandler(state));
    }

    private static InboundConfigurationTask setupInboundHandler(ChannelInboundHandler handler) {
        return ctx -> {
            ctx.pipeline().replace(ctx.name(), MinecraftHandlers.DECODER, handler);
            ctx.channel().config().setAutoRead(true);
            relocate(ctx.channel(), PacketBound.SERVER);
        };
    }

    public static OutboundConfigurationTask setupOutboundProtocolTask(PacketState state) {
        return setupOutboundHandler(ModernCodecHandlerFactory.createEncodeHandler(state));
    }

    private static OutboundConfigurationTask setupOutboundHandler(ChannelOutboundHandler handler) {
        return ctx -> {
            ctx.pipeline().replace(ctx.name(), MinecraftHandlers.ENCODER, handler);
            relocate(ctx.channel(), PacketBound.CLIENT);
        };
    }

    private static void relocate(Channel channel, PacketBound bound) {
        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(channel, bound);
        } catch (MissingHandlerException ex) {
            ChannelCloseInspection.close(channel);
        }
    }
}
