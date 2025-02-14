package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.via.ViaHandlers;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.handler.*;

import java.util.ArrayList;
import java.util.List;

public class SpigotPipelineInjector {

    public void injectChannel(Channel channel) {
        if (channel.pipeline().get(MinecraftHandlers.SPLITTER) == null) {
            // rare case
            ChannelCloseInspection.close(channel);
            return;
        }

        ejectHandlersIfExists(channel);

        HoneyPacketHandler packetHandler = new HoneyPacketHandler(channel);
        channel.pipeline().addBefore(MinecraftHandlers.DECODER, HoneyPlugin.HONEY_DECODER_HANDLER_NAME, new HoneyPacketInStream(packetHandler));
        channel.pipeline().addAfter(getMinecraftEncoderName(channel), HoneyPlugin.HONEY_ENCODER_HANDLER_NAME, new HoneyPacketOutStream(packetHandler));

        channel.pipeline().addAfter(MinecraftHandlers.SPLITTER, HoneyPlugin.HONEY_FILTER_HANDLER_NAME, new InboundPacketFilter(packetHandler));

        channel.pipeline().addBefore(MinecraftHandlers.CONNECTION, PreMainThreadInterceptor.NAME, new PreMainThreadInterceptor(packetHandler));
        channel.pipeline().addBefore(MinecraftHandlers.CONNECTION, HoneyPlugin.HONEY_DEBUGGER_HANDLER_NAME, new HoneyDebugger(packetHandler));

        if (channel.pipeline().get(HoneyPlugin.HONEY_CHANNEL_INITIALIZER_HANDLER_NAME) != null) {
            channel.pipeline().remove(HoneyPlugin.HONEY_CHANNEL_INITIALIZER_HANDLER_NAME);
        }
    }

    private String getMinecraftDecoderName(Channel channel) {
        if (channel.pipeline().get(MinecraftHandlers.DECODER) != null) {
            return MinecraftHandlers.DECODER;
        } else {
            return MinecraftHandlers.INBOUND_CONFIG;
        }
    }

    private String getMinecraftEncoderName(Channel channel) {
        if (channel.pipeline().get(MinecraftHandlers.ENCODER) != null) {
            return MinecraftHandlers.ENCODER;
        } else {
            return MinecraftHandlers.OUTBOUND_CONFIG;
        }
    }

    public void ensureCorrectPipelineOrder(Channel channel) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().execute(() -> ensureCorrectPipelineOrder(channel));
            return;
        }

        ChannelPipeline pipeline = channel.pipeline();
        List<String> handlers = new ArrayList<>(pipeline.toMap().keySet());

        if (handlers.indexOf(HoneyPlugin.HONEY_DECODER_HANDLER_NAME) != handlers.indexOf(getMinecraftDecoderName(channel)) - 1) {
            HoneyPacketInStream decoder = HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel);

            pipeline.remove(HoneyPlugin.HONEY_DECODER_HANDLER_NAME);
            pipeline.addBefore(getMinecraftDecoderName(channel), HoneyPlugin.HONEY_DECODER_HANDLER_NAME, decoder);
        }

        if (handlers.indexOf(HoneyPlugin.HONEY_ENCODER_HANDLER_NAME) != handlers.indexOf(getMinecraftEncoderName(channel)) + 1) {
            HoneyPacketOutStream encoder = HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel);

            pipeline.remove(HoneyPlugin.HONEY_ENCODER_HANDLER_NAME);
            pipeline.addAfter(getMinecraftEncoderName(channel), HoneyPlugin.HONEY_ENCODER_HANDLER_NAME, encoder);
        }

        if (
            // inject filter after decompress
            channel.pipeline().get(MinecraftHandlers.DECOMPRESS) != null
                    && handlers.indexOf(MinecraftHandlers.DECOMPRESS) > handlers.indexOf(HoneyPlugin.HONEY_FILTER_HANDLER_NAME)
                    ||
                    // inject filter before ViaVersion
                    channel.pipeline().get(ViaHandlers.DECODER) != null
                            && handlers.indexOf(ViaHandlers.DECODER) < handlers.indexOf(HoneyPlugin.HONEY_FILTER_HANDLER_NAME)
        ) {
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getChannelManager().getFilter(channel);

            channel.pipeline().remove(filter);
            if (channel.pipeline().get(MinecraftHandlers.DECOMPRESS) != null) {
                channel.pipeline().addAfter(MinecraftHandlers.DECOMPRESS, HoneyPlugin.HONEY_FILTER_HANDLER_NAME, filter);
            } else if (channel.pipeline().get(MinecraftHandlers.SPLITTER) != null) {
                channel.pipeline().addAfter(MinecraftHandlers.SPLITTER, HoneyPlugin.HONEY_FILTER_HANDLER_NAME, filter);
            } else {
                ChannelCloseInspection.close(channel);
            }
        }

        ViaClientboundInterceptor viaInterceptor = (ViaClientboundInterceptor) channel.pipeline().get(ViaClientboundInterceptor.NAME);
        if (viaInterceptor != null && handlers.indexOf(ViaClientboundInterceptor.NAME) != handlers.indexOf(ViaHandlers.ENCODER) - 1) {
            channel.pipeline().remove(viaInterceptor);
            channel.pipeline().addBefore(ViaHandlers.ENCODER, ViaClientboundInterceptor.NAME, viaInterceptor);
        }

        PreMainThreadInterceptor mainThreadInterceptor = (PreMainThreadInterceptor) channel.pipeline().get(PreMainThreadInterceptor.NAME);
        if (mainThreadInterceptor == null) {
            ChannelCloseInspection.close(channel);
            return;
        }
        if (handlers.indexOf(PreMainThreadInterceptor.NAME) != handlers.indexOf(HoneyPlugin.HONEY_DEBUGGER_HANDLER_NAME) - 1) {
            channel.pipeline().remove(mainThreadInterceptor);
            channel.pipeline().addBefore(HoneyPlugin.HONEY_DEBUGGER_HANDLER_NAME, PreMainThreadInterceptor.NAME, mainThreadInterceptor);
        }
    }

    public void ejectHandlersIfExists(Channel channel) {
        if (channel.pipeline().get(HoneyPlugin.HONEY_FILTER_HANDLER_NAME) != null) {
            channel.pipeline().remove(HoneyPlugin.HONEY_FILTER_HANDLER_NAME);
        }

        if (channel.pipeline().get(HoneyPlugin.HONEY_DEBUGGER_HANDLER_NAME) != null) {
            channel.pipeline().remove(HoneyPlugin.HONEY_DEBUGGER_HANDLER_NAME);
        }

        if (channel.pipeline().get(HoneyPlugin.HONEY_DECODER_HANDLER_NAME) != null) {
            channel.pipeline().remove(HoneyPlugin.HONEY_DECODER_HANDLER_NAME);
        }

        if (channel.pipeline().get(HoneyPlugin.HONEY_ENCODER_HANDLER_NAME) != null) {
            channel.pipeline().remove(HoneyPlugin.HONEY_ENCODER_HANDLER_NAME);
        }

        if (channel.pipeline().get(PreMainThreadInterceptor.NAME) != null) {
            channel.pipeline().remove(PreMainThreadInterceptor.NAME);
        }

        if( channel.pipeline().get(ViaClientboundInterceptor.NAME) != null) {
            channel.pipeline().remove(ViaClientboundInterceptor.NAME);
        }
    }

    public void ejectChannel(Channel channel) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> ejectChannel(channel));
            return;
        }
        ejectHandlersIfExists(channel);
    }
}
