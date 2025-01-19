package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.async.WorkerPool;
import me.alek.honey.compat.protocollib.ProtocolLib;
import me.alek.honey.compat.via.ViaHandlers;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.handler.*;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Provider;
import me.alek.honey.util.RemoteProviderBridge;
import me.alek.honey.util.RemoteProviderUtils;
import me.alek.honey.util.reflection.NMS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpigotPipelineInjector {

    private static final String PRE_RESOLVED_PACKET_HANDLER = "pre-resolved-packet-handler";

    private final Provider.ProviderNoInput<Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>>> channelProxyRemoteFactoryProvider = RemoteProviderBridge.Factory.<Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>>>createWithNoInput()
            .createProviderRemotely(null, RemoteProviderUtils::isProviderMethod, RemoteProviderUtils.getNewProviderWithException(ChannelProxyFactory.Provider.class));
    private final Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>> channelProxyRemoteFactory = RemoteProviderBridge.Factory.<Channel, Channel, Consumer<Channel>>createWithTwoInput()
            .createProviderRemotely(null, RemoteProviderUtils::isProviderMethod, channelProxyRemoteFactoryProvider.get());

    public void injectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get(MinecraftHandlers.SPLITTER) == null) {
                if (channel.isOpen()) {
                    channel.close();
                }
                return;
            }

            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
            String debuggerName = HoneyPlugin.get().getPacketLibrary().getDebuggerName();

            if (channel.pipeline().get(filterName) != null
                    && channel.pipeline().get(debuggerName) != null) {
                return;
            }

            ejectHandlersIfExists(channel);

            HoneyPacketHandler packetHandler = new HoneyPacketHandler(channel);

            setupPreparationHandlers(channel, packetHandler);
            togglePreResolvedBounds(channel, new PreResolvedPacketHandler(packetHandler));

            channel.pipeline().addAfter(MinecraftHandlers.SPLITTER, filterName, new InboundPacketFilter(packetHandler));
            channel.pipeline().addBefore(MinecraftHandlers.MANAGER, debuggerName, new HoneyDebugger(packetHandler.getPlayer()));
        }
    }

    public void injectChannelProxy(Channel channel) throws MissingHandlerException {
        Object networkManager = HoneyPlugin.get().getPacketLibrary().getServerInjector().getNetworkManagerFor(channel);
        if (networkManager == null
                || !NMS.getNetworkManagerClass().isInstance(networkManager)) {
            throw new MissingHandlerException();
        }

        IStructureModifier<Object> channelStructureModifier = ReflectStructure.Provider.acquireStructure(NMS.getNetworkManagerClass())
                .withWeakType(Channel.class)
                .withTarget(networkManager);
        Channel registeredChannel = (Channel) channelStructureModifier.readField(0);

        Object channelProxy = channelProxyRemoteFactory.get(channel, c -> HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(c));

        if (ProtocolLib.ProtocolLibValidator.VALIDATOR.find().get()
                && ProtocolLib.CHANNEL_PROXY_CLASS.isInstance(registeredChannel)) {
            ReflectStructure.Provider.acquireStructure(ProtocolLib.CHANNEL_PROXY_CLASS)
                    .withWeakType(Channel.class).withTarget(registeredChannel)
                    .writeField(0, channelProxy);
        } else {
            channelStructureModifier.writeField(0, channelProxy);
        }
    }

    public void ensureCorrectPipelineOrder(Channel channel) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().execute(() -> ensureCorrectPipelineOrder(channel));
            return;
        }
        synchronized (channel) {
            ChannelPipeline pipeline = channel.pipeline();
            List<String> handlers = new ArrayList<>(pipeline.toMap().keySet());

            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
            if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel) != null
                    && PacketInterceptorResolver.hasDecoderHandler(pipeline)) {
                if (handlers.indexOf(decoderName) != handlers.indexOf(PacketInterceptorResolver.getDecoderPosition(pipeline)) - 1) {
                    HoneyPacketDecodePreparation decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);

                    pipeline.remove(decoderName);
                    pipeline.addBefore(PacketInterceptorResolver.getDecoderPosition(pipeline), decoderName, decoder);
                }
            }

            String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();
            if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) != null
                    && PacketInterceptorResolver.hasEncoderHandler(pipeline)) {
                if (handlers.indexOf(encoderName) != handlers.indexOf(PacketInterceptorResolver.getEncoderPosition(pipeline)) + 1) {
                    HoneyPacketEncodePreparation encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);

                    pipeline.remove(encoderName);
                    pipeline.addAfter(PacketInterceptorResolver.getEncoderPosition(pipeline), encoderName, encoder);
                }
            }

            if (
                // inject filter after decompress
                    channel.pipeline().get(MinecraftHandlers.DECOMPRESS) != null
                            && handlers.indexOf(MinecraftHandlers.DECOMPRESS) > handlers.indexOf(HoneyPlugin.get().getPacketLibrary().getFilterName())
                            ||
                            // inject filter before ViaVersion
                            channel.pipeline().get(ViaHandlers.DECODER) != null
                                    && handlers.indexOf(ViaHandlers.DECODER) < handlers.indexOf(HoneyPlugin.get().getPacketLibrary().getFilterName())
            ) {
                InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);

                channel.pipeline().remove(filter);
                if (channel.pipeline().get(MinecraftHandlers.DECOMPRESS) != null) {
                    channel.pipeline().addAfter(MinecraftHandlers.DECOMPRESS, HoneyPlugin.get().getPacketLibrary().getFilterName(), filter);
                } else if (channel.pipeline().get(MinecraftHandlers.SPLITTER) != null) {
                    channel.pipeline().addAfter(MinecraftHandlers.SPLITTER, HoneyPlugin.get().getPacketLibrary().getFilterName(), filter);
                } else {
                    channel.close();
                }
            }
        }
    }

    public void setupPreparationHandlers(Channel channel, HoneyPacketHandler packetHandler) {
        String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
        String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();

        HoneyPacketDecodePreparation decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
        HoneyPacketEncodePreparation encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);

        AsyncPacketStream async;
        if (decoder == null && encoder == null) {
            async = new AsyncPacketStream(packetHandler.getPlayer(), WorkerPool.ASYNC_PACKET_DECODER_POOL);
        } else {
            if (decoder != null) {
                async = decoder.getAsyncPacketStream();
            } else {
                async = encoder.getAsyncPacketStream();
            }
        }

        forceResolving(packetHandler, channel);

        if (decoder == null
                && PacketInterceptorResolver.hasDecoderHandler(channel.pipeline())
                && !isConfigurationActive(channel.pipeline(), PacketBound.SERVER)) {
            channel.pipeline().addBefore(PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), decoderName,
                    new HoneyPacketDecodePreparation(channel, async, packetHandler));
        }

        if (encoder == null
                && PacketInterceptorResolver.hasEncoderHandler(channel.pipeline())
                && !isConfigurationActive(channel.pipeline(), PacketBound.CLIENT)) {
            channel.pipeline().addAfter(PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), encoderName,
                    new HoneyPacketEncodePreparation(channel, async, packetHandler));
        }
    }

    public void forceResolving(HoneyPacketHandler packetHandler, Channel channel) {
        if (!PacketInterceptorResolver.isDecoderPositionResolved()
                && packetHandler.getProtocolState(PacketBound.SERVER) == PacketState.PLAY
                && !isConfigurationActive(channel.pipeline())) {
            PacketInterceptorResolver.getDecoderPosition(channel.pipeline());
        }

        if (!PacketInterceptorResolver.isEncoderPositionResolved()
                && packetHandler.getProtocolState(PacketBound.CLIENT) == PacketState.PLAY
                && !isConfigurationActive(channel.pipeline())) {
            PacketInterceptorResolver.getEncoderPosition(channel.pipeline());
        }
    }

    public boolean relocatePreparationHandlers(Channel channel) throws MissingHandlerException {
        ChannelHandler preResolvedPacketHandler = channel.pipeline().get(PRE_RESOLVED_PACKET_HANDLER);
        if (preResolvedPacketHandler != null) {
            togglePreResolvedBounds(channel, (PreResolvedPacketHandler) preResolvedPacketHandler);
        }

        if (!HoneyPlugin.get().getPacketLibrary().getPacketManager().bothPreparationHandlerExists(channel)) {
            setupPreparationHandlers(channel, getPacketHandler(channel));

            if (preResolvedPacketHandler != null) {
                togglePreResolvedBounds(channel, (PreResolvedPacketHandler) preResolvedPacketHandler);
            }

            if (!HoneyPlugin.get().getPacketLibrary().getPacketManager().bothPreparationHandlerExists(channel)) {
                ensureCorrectPipelineOrder(channel);
                return false;
            }
        }

        ensureCorrectPipelineOrder(channel);

        return true;
    }

    public void ejectHandlersIfExists(Channel channel) {
        String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
        String debuggerName = HoneyPlugin.get().getPacketLibrary().getDebuggerName();
        String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
        String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();

        if (channel.pipeline().get(filterName) != null) {
            channel.pipeline().remove(filterName);
        }

        if (channel.pipeline().get(debuggerName) != null) {
            channel.pipeline().remove(debuggerName);
        }

        if (channel.pipeline().get(decoderName) != null) {
            channel.pipeline().remove(decoderName);
        }

        if (channel.pipeline().get(encoderName) != null) {
            channel.pipeline().remove(encoderName);
        }

        if (channel.pipeline().get(PRE_RESOLVED_PACKET_HANDLER) != null) {
            channel.pipeline().remove(PRE_RESOLVED_PACKET_HANDLER);
        }
    }

    public void ejectChannel(Channel channel) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> ejectChannel(channel));
            return;
        }
        synchronized (channel) {
            ejectHandlersIfExists(channel);

            if (channel.isOpen()) {
                channel.close();
            }
        }
    }

    public void decrementTaskSupplierCount(Channel channel) {
        synchronized (channel) {
            if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel) != null) {
                HoneyPacketDecodePreparation decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                decoder.getAsyncPacketStream().farewell();
            } else if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) != null) {
                HoneyPacketEncodePreparation encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
                encoder.getAsyncPacketStream().farewell();
            }
        }
    }

    public HoneyPacketHandler getPacketHandler(Channel channel) throws MissingHandlerException {
        ChannelHandler preResolvedPacketHandler = channel.pipeline().get(PRE_RESOLVED_PACKET_HANDLER);
        if (preResolvedPacketHandler != null) {
            PreResolvedPacketHandler preResolvedPacketHandler0 = (PreResolvedPacketHandler) preResolvedPacketHandler;
            return preResolvedPacketHandler0.getPacketHandler();
        }

        if (PacketInterceptorResolver.isDecoderPositionResolved()) {
            HoneyPacketDecodePreparation decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
            if (decoder != null) {
                return decoder.getPacketHandler();
            }
        }
        if (PacketInterceptorResolver.isEncoderPositionResolved()) {
            HoneyPacketEncodePreparation encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
            if (encoder != null) {
                return encoder.getPacketHandler();
            }
        }

        throw new MissingHandlerException();
    }

    public boolean isConfigurationActive(ChannelPipeline pipeline, PacketBound bound) {
        switch (bound) {
            case SERVER:
                return pipeline.get("inbound_config") != null;
            case CLIENT:
                return pipeline.get("outbound_config") != null;
            default:
                return false;
        }
    }

    public boolean isConfigurationActive(ChannelPipeline pipeline) {
        return isConfigurationActive(pipeline, PacketBound.CLIENT)
                || isConfigurationActive(pipeline, PacketBound.SERVER);
    }

    public void togglePreResolvedBounds(Channel channel, PreResolvedPacketHandler handler) {
        boolean decoderResolved = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel) != null;
        boolean encoderResolved = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) != null;

        if (decoderResolved && encoderResolved) {
            if (channel.pipeline().context(handler) != null) {
                channel.pipeline().remove(handler);
            }
            return;
        } else if (decoderResolved) {
            handler.read = false;
            handler.write = true;
        } else if (encoderResolved) {
            handler.read = true;
            handler.write = false;
        }

        if (channel.pipeline().context(handler) == null) {
            channel.pipeline().addBefore(MinecraftHandlers.MANAGER, PRE_RESOLVED_PACKET_HANDLER, handler);
        }
    }
}
