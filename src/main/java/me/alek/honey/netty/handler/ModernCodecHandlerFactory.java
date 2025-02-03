package me.alek.honey.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.MinecraftReflection;

public class ModernCodecHandlerFactory {

    private static final ConstructorInvoker<Object> PACKET_DECODER_CONST = new ConstructorInvoker<>(MinecraftReflection.getPacketDecoderClass(), MinecraftReflection.getProtocolInfoClass());
    private static final ConstructorInvoker<Object> PACKET_ENCODER_CONST = new ConstructorInvoker<>(MinecraftReflection.getPacketEncoderClass(), MinecraftReflection.getProtocolInfoClass());

    public static ChannelInboundHandler createDecodeHandler(PacketState state) {
        return (ChannelInboundHandler) PACKET_DECODER_CONST.invoke(CodecFactory.getProtocolInfo1_20_5(PacketBound.SERVER, state));
    }

    public static ChannelOutboundHandler createEncodeHandler(PacketState state) {
        return (ChannelOutboundHandler) PACKET_ENCODER_CONST.invoke(CodecFactory.getProtocolInfo1_20_5(PacketBound.CLIENT, state));
    }

    public static ChannelHandler createCodecHandler(PacketBound bound, PacketState state) {
        if (bound.equals(PacketBound.SERVER)) {
            return createDecodeHandler(state);
        } else {
            return createEncodeHandler(state);
        }
    }
}
