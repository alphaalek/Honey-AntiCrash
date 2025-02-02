package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.async.WorkerPool;
import me.alek.honey.compat.floodgate.Floodgate;
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
import java.util.Optional;
import java.util.function.Consumer;

public class SpigotPipelineInjector {

    private final Provider.ProviderNoInput<Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>>> CHANNEL_PROXY_REMOTE_FACTORY_PROVIDER = RemoteProviderBridge.Factory.<Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>>>createWithNoInput()
            .createProviderRemotely(null, RemoteProviderUtils::isProviderMethod, RemoteProviderUtils.getNewProviderWithException(ChannelProxyFactory.Provider.class));
    private final Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>> CHANNEL_PROXY_REMOTE_FACTORY = RemoteProviderBridge.Factory.<Channel, Channel, Consumer<Channel>>createWithTwoInput()
            .createProviderRemotely(null, RemoteProviderUtils::isProviderMethod, CHANNEL_PROXY_REMOTE_FACTORY_PROVIDER.get());

    public void injectChannel(Channel channel) {
        if (channel.pipeline().get(MinecraftHandlers.SPLITTER) == null) {
            ChannelCloseInspection.close(channel);
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
        channel.pipeline().addBefore(MinecraftHandlers.CONNECTION, debuggerName, new HoneyDebugger(packetHandler.getPlayer()));

        if (channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getChannelInitName()) != null) {
            channel.pipeline().remove(HoneyPlugin.get().getPacketLibrary().getChannelInitName());
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

        Object channelProxy = CHANNEL_PROXY_REMOTE_FACTORY.get(channel, c -> HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(c));

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

        ChannelPipeline pipeline = channel.pipeline();
        List<String> handlers = new ArrayList<>(pipeline.toMap().keySet());

        String inStreamName = HoneyPlugin.get().getPacketLibrary().getInStreamName();
        if (HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel) != null
                && PacketInterceptorResolver.hasDecoderHandler(pipeline)) {
            if (handlers.indexOf(inStreamName) != handlers.indexOf(PacketInterceptorResolver.getDecoderPosition(pipeline)) - 1) {
                HoneyPacketInStream decoder = HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel);

                pipeline.remove(inStreamName);
                pipeline.addBefore(PacketInterceptorResolver.getDecoderPosition(pipeline), inStreamName, decoder);
            }
        }

        String outStreamName = HoneyPlugin.get().getPacketLibrary().getOutStreamName();
        if (HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel) != null
                && PacketInterceptorResolver.hasEncoderHandler(pipeline)) {
            if (handlers.indexOf(outStreamName) != handlers.indexOf(PacketInterceptorResolver.getEncoderPosition(pipeline)) + 1) {
                HoneyPacketOutStream encoder = HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel);

                pipeline.remove(outStreamName);
                pipeline.addAfter(PacketInterceptorResolver.getEncoderPosition(pipeline), outStreamName, encoder);
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
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getChannelManager().getFilter(channel);

            channel.pipeline().remove(filter);
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
            if (channel.pipeline().get(MinecraftHandlers.DECOMPRESS) != null) {
                channel.pipeline().addAfter(MinecraftHandlers.DECOMPRESS, filterName, filter);
            } else if (channel.pipeline().get(MinecraftHandlers.SPLITTER) != null) {
                channel.pipeline().addAfter(MinecraftHandlers.SPLITTER, filterName, filter);
             }else {
                ChannelCloseInspection.close(channel);
            }
        }

        ViaClientboundInterceptor interceptor = (ViaClientboundInterceptor) channel.pipeline().get(ViaClientboundInterceptor.NAME);
        if (interceptor != null && handlers.indexOf(ViaClientboundInterceptor.NAME) != handlers.indexOf(ViaHandlers.ENCODER) - 1) {
            channel.pipeline().remove(interceptor);
            channel.pipeline().addBefore(ViaHandlers.ENCODER, ViaClientboundInterceptor.NAME, interceptor);
        }
    }

    public void setupPreparationHandlers(Channel channel, HoneyPacketHandler packetHandler) {
        String inStreamName = HoneyPlugin.get().getPacketLibrary().getInStreamName();
        String outStreamName = HoneyPlugin.get().getPacketLibrary().getOutStreamName();

        HoneyPacketInStream inStream = HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel);
        HoneyPacketOutStream outStream = HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel);

        AsyncPacketStream async;
        if (inStream == null && outStream == null) {
            async = new AsyncPacketStream(packetHandler.getPlayer(), WorkerPool.ASYNC_PACKET_DECODER_POOL);
        } else {
            if (inStream != null) {
                async = inStream.getAsyncPacketStream();
            } else {
                async = outStream.getAsyncPacketStream();
            }
        }

        forceResolving(packetHandler, channel);

        if (inStream == null
                && PacketInterceptorResolver.hasDecoderHandler(channel.pipeline())
                && !isConfigurationActive(channel.pipeline(), PacketBound.SERVER)) {
            channel.pipeline().addBefore(PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), inStreamName,
                    new HoneyPacketInStream(channel, async, packetHandler));
        }

        if (outStream == null
                && PacketInterceptorResolver.hasEncoderHandler(channel.pipeline())
                && !isConfigurationActive(channel.pipeline(), PacketBound.CLIENT)) {
            channel.pipeline().addAfter(PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), outStreamName,
                    new HoneyPacketOutStream(channel, async, packetHandler));
        }
    }

    public void forceResolving(HoneyPacketHandler packetHandler, Channel channel) {
        if (!PacketInterceptorResolver.isDecoderPositionResolved()
                && packetHandler.getProtocolState(PacketBound.SERVER).equals(PacketState.PLAY)
                && packetHandler.getSimulatedServerboundProtocolState().equals(PacketState.PLAY)
                && !isConfigurationActive(channel.pipeline(), PacketBound.SERVER)) {
            PacketInterceptorResolver.getDecoderPosition(channel.pipeline());
        }

        if (!PacketInterceptorResolver.isEncoderPositionResolved()
                && packetHandler.getProtocolState(PacketBound.CLIENT).equals(PacketState.PLAY)
                && packetHandler.getSimulatedClientboundProtocolState().equals(PacketState.PLAY)
                && !isConfigurationActive(channel.pipeline(), PacketBound.CLIENT)) {
            PacketInterceptorResolver.getEncoderPosition(channel.pipeline());
        }
    }

    public void relocatePreparationHandlers(Channel channel) throws MissingHandlerException {
        ChannelHandler preResolvedPacketHandler = channel.pipeline().get(PreResolvedPacketHandler.NAME);
        if (preResolvedPacketHandler != null) {
            togglePreResolvedBounds(channel, (PreResolvedPacketHandler) preResolvedPacketHandler);
        }

        if (!HoneyPlugin.get().getPacketLibrary().getChannelManager().bothStreamBoundsHandlersExists(channel)) {
            setupPreparationHandlers(channel, getPacketHandler(channel));

            if (preResolvedPacketHandler != null) {
                togglePreResolvedBounds(channel, (PreResolvedPacketHandler) preResolvedPacketHandler);
            }
        }

        ensureCorrectPipelineOrder(channel);
    }

    public void ejectHandlersIfExists(Channel channel) {
        String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
        String debuggerName = HoneyPlugin.get().getPacketLibrary().getDebuggerName();
        String inStreamName = HoneyPlugin.get().getPacketLibrary().getInStreamName();
        String outStreamName = HoneyPlugin.get().getPacketLibrary().getOutStreamName();

        if (channel.pipeline().get(filterName) != null) {
            channel.pipeline().remove(filterName);
        }

        if (channel.pipeline().get(debuggerName) != null) {
            channel.pipeline().remove(debuggerName);
        }

        if (channel.pipeline().get(inStreamName) != null) {
            channel.pipeline().remove(inStreamName);
        }

        if (channel.pipeline().get(outStreamName) != null) {
            channel.pipeline().remove(outStreamName);
        }

        if (channel.pipeline().get(PreResolvedPacketHandler.NAME) != null) {
            channel.pipeline().remove(PreResolvedPacketHandler.NAME);
        }
    }

    public void ejectChannel(Channel channel) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> ejectChannel(channel));
            return;
        }
        ejectHandlersIfExists(channel);

        ChannelCloseInspection.close(channel);
    }

    public void decrementTaskSupplierCount(Channel channel) {
        if (HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel) != null) {
            HoneyPacketInStream decoder = HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel);
            decoder.getAsyncPacketStream().farewell();
        } else if (HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel) != null) {
            HoneyPacketOutStream encoder = HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel);
            encoder.getAsyncPacketStream().farewell();
        }
    }

    public HoneyPacketHandler getPacketHandler(Channel channel) throws MissingHandlerException {
        ChannelHandler preResolvedPacketHandler = channel.pipeline().get(PreResolvedPacketHandler.NAME);
        if (preResolvedPacketHandler != null) {
            PreResolvedPacketHandler preResolvedPacketHandler0 = (PreResolvedPacketHandler) preResolvedPacketHandler;
            return preResolvedPacketHandler0.getPacketHandler();
        }

        if (PacketInterceptorResolver.isDecoderPositionResolved()) {
            HoneyPacketInStream inStream = HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel);
            if (inStream != null) {
                return inStream.getPacketHandler();
            }
        }
        if (PacketInterceptorResolver.isEncoderPositionResolved()) {
            HoneyPacketOutStream outStream = HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel);
            if (outStream != null) {
                return outStream.getPacketHandler();
            }
        }

        throw new MissingHandlerException();
    }

    public boolean isConfigurationActive(ChannelPipeline pipeline, PacketBound bound) {
        switch (bound) {
            case SERVER:
                return pipeline.get(MinecraftHandlers.INBOUND_CONFIG) != null;
            case CLIENT:
                return pipeline.get(MinecraftHandlers.OUTBOUND_CONFIG) != null;
            default:
                return false;
        }
    }

    public boolean isConfigurationActive(ChannelPipeline pipeline) {
        return isConfigurationActive(pipeline, PacketBound.CLIENT)
                || isConfigurationActive(pipeline, PacketBound.SERVER);
    }

    public void togglePreResolvedBounds(Channel channel, HoneyPacketHandler handler) {
        togglePreResolvedBounds(channel,
                Optional.ofNullable((PreResolvedPacketHandler) channel.pipeline().get(PreResolvedPacketHandler.NAME))
                        .orElse(new PreResolvedPacketHandler(handler)));
    }

    public void togglePreResolvedBounds(Channel channel, PreResolvedPacketHandler handler) {
        boolean inStreamResolved = HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(channel) != null;
        boolean outStreamResolved = HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(channel) != null;

        if (inStreamResolved && outStreamResolved) {
            if (channel.pipeline().context(handler) != null) {
                channel.pipeline().remove(handler);
            }
            return;
        } else if (inStreamResolved) {
            handler.read = false;
            handler.write = true;
        } else if (outStreamResolved) {
            handler.read = true;
            handler.write = false;
        } else {
            handler.read = true;
            handler.write = true;
        }

        if (channel.pipeline().context(handler) == null) {
            if (channel.pipeline().get(Floodgate.DATA_HANDLER) != null) {
                // inject before floodgate, because floodgate is dropping some packets for the pipeline that we need to listen to
                channel.pipeline().addBefore(Floodgate.DATA_HANDLER, PreResolvedPacketHandler.NAME, handler);
            } else {
                channel.pipeline().addBefore(MinecraftHandlers.CONNECTION, PreResolvedPacketHandler.NAME, handler);
            }
        }
    }
}
