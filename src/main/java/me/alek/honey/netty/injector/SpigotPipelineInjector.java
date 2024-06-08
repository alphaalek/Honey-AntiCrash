package me.alek.honey.netty.injector;

import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.handler.HoneyPacketDecodePreparation;
import me.alek.honey.netty.handler.*;
import me.alek.honey.async.AsyncDecoder;
import me.alek.honey.async.WorkerPool;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;

import java.util.ArrayList;
import java.util.List;

public class SpigotPipelineInjector {

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

            channel.pipeline().addBefore(MinecraftHandlers.MANAGER, debuggerName, new HoneyDebugger());
            channel.pipeline().addAfter(MinecraftHandlers.SPLITTER, filterName, new InboundPacketFilter());

            HoneyPacketHandler packetHandler = new HoneyPacketHandler(channel);
            setupPreparationHandlers(channel, packetHandler);

            boolean hasDecoder = PacketInterceptorResolver.hasDecoderHandler(channel.pipeline());
            boolean hasEncoder = PacketInterceptorResolver.hasEncoderHandler(channel.pipeline());
            if (!hasDecoder || !hasEncoder) {
                PreResolvedPacketHandler preResolvedPacketHandler = new PreResolvedPacketHandler(packetHandler);
                channel.pipeline().addBefore(MinecraftHandlers.MANAGER, "pre-resolved-packet-handler", preResolvedPacketHandler);
                if (hasDecoder) {
                    preResolvedPacketHandler.read = false;
                } else if (hasEncoder) {
                    preResolvedPacketHandler.write = false;
                }
            }
        }
    }

    public void injectChannelProxy(Channel channel) throws MissingHandlerException {
        Object networkManager = HoneyPlugin.get().getPacketLibrary().getServerInjector().getNetworkManagerFor(channel);
        if (networkManager == null || !NMS.getNetworkManagerClass().isInstance(networkManager)) {
            throw new MissingHandlerException();
        }

        IStructureModifier<Channel> channelStructureModifier = ReflectStructureCache.acquireStructure(NMS.getNetworkManagerClass())
                .withType(Channel.class)
                .withTarget(networkManager);

        Channel registeredChannel = channelStructureModifier.readField(0);
        if (registeredChannel instanceof ChannelProxy) {
            return;
        }

        ChannelProxy channelProxy = new ChannelProxy(registeredChannel);
        channelStructureModifier.writeField(0, channelProxy);
    }

    public void ensureCorrectPipelineOrder(Channel channel) {
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
        }
    }

    public void setupPreparationHandlers(Channel channel, HoneyPacketHandler packetHandler) {
        String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
        String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();

        AsyncDecoder asyncDecoder = new AsyncDecoder(WorkerPool.ASYNC_PACKET_DECODER_POOL);

        if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel) == null
                && PacketInterceptorResolver.isDecoderPositionResolved()
                && PacketInterceptorResolver.hasDecoderHandler(channel.pipeline())
                && !isConfigurationActive(channel.pipeline(), PacketBound.SERVER)) {
            channel.pipeline().addBefore(PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), decoderName,
                    new HoneyPacketDecodePreparation(channel, asyncDecoder, packetHandler));
        }

        if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) == null
                && PacketInterceptorResolver.isEncoderPositionResolved()
                && PacketInterceptorResolver.hasEncoderHandler(channel.pipeline())
                && !isConfigurationActive(channel.pipeline(), PacketBound.CLIENT)) {
            channel.pipeline().addAfter(PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), encoderName,
                    new HoneyPacketEncodePreparation(channel, asyncDecoder, packetHandler));
        }
    }

    public void relocateFilter(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get(MinecraftHandlers.DECOMPRESS) != null) {
                InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);

                channel.pipeline().remove(filter);
                channel.pipeline().addAfter(MinecraftHandlers.DECOMPRESS, HoneyPlugin.get().getPacketLibrary().getFilterName(), filter);
            }
        }
    }

    public boolean relocatePreparationHandlers(HoneyPlayer player, Channel channel) throws MissingHandlerException {
        HoneyPacketHandler packetHandler = getPacketHandler(channel);

        if (packetHandler.getProtocolState(PacketBound.SERVER) == PacketState.PLAY
                && !PacketInterceptorResolver.isDecoderPositionResolved()
                && !isConfigurationActive(channel.pipeline())) {
            PacketInterceptorResolver.getDecoderPosition(channel.pipeline());
        }

        if (packetHandler.getProtocolState(PacketBound.CLIENT) == PacketState.PLAY
                && !PacketInterceptorResolver.isEncoderPositionResolved()
                && !isConfigurationActive(channel.pipeline())) {
            PacketInterceptorResolver.getEncoderPosition(channel.pipeline());
        }

        ChannelHandler preResolvedPacketHandler = channel.pipeline().get("pre-resolved-packet-handler");
        if (preResolvedPacketHandler != null) {
            togglePreResolvedBounds(channel, (PreResolvedPacketHandler) preResolvedPacketHandler);
        }

        if (!HoneyPlugin.get().getPacketLibrary().getPacketManager().bothPreparationHandlerExists(channel)) {
            if (player != null && player.packetHandler != null) {
                setupPreparationHandlers(channel, player.packetHandler);
            } else {
                setupPreparationHandlers(channel, packetHandler);
            }

            if (preResolvedPacketHandler != null) {
                togglePreResolvedBounds(channel, (PreResolvedPacketHandler) preResolvedPacketHandler);
            }

            if (!HoneyPlugin.get().getPacketLibrary().getPacketManager().bothPreparationHandlerExists(channel)) {
                return false;
            }
        }

        if (player != null && player.updateHandlersLater) {
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().updatePlayer(player);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            player.updateHandlersLater = false;
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

        if (channel.pipeline().get("pre-resolved-packet-handler") != null) {
            channel.pipeline().remove("pre-resolved-packet-handler");
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
                decoder.getAsyncDecoder().farewell();
            } else if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) != null) {
                HoneyPacketEncodePreparation encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
                encoder.getAsyncDecoder().farewell();
            }
        }
    }

    public HoneyPacketHandler getPacketHandler(Channel channel) throws MissingHandlerException {
        ChannelHandler preResolvedPacketHandler = channel.pipeline().get("pre-resolved-packet-handler");
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
            channel.pipeline().remove(handler);
        } else if (decoderResolved) {
            handler.read = false;
        } else if (encoderResolved) {
            handler.read = false;
        }
    }

    public void updatePlayer(HoneyPlayer player) throws MissingHandlerException {
        Channel channel = player.channel;
        if (channel == null) {
            throw new MissingHandlerException("Channel");
        }

        synchronized (channel) {
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            HoneyDebugger debugger = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDebugger(channel);
            if (filter == null || debugger == null) {
                throw new MissingHandlerException(filter + ", " + debugger);
            }

            filter.setPlayer(player);
            debugger.setPlayer(player);

            if (PacketInterceptorResolver.isDecoderPositionResolved()
                    && !isConfigurationActive(channel.pipeline(), PacketBound.SERVER)) {
                HoneyPacketDecodePreparation decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                if (decoder == null) {
                    throw new MissingHandlerException("Decoder");
                }

                player.setPacketHandler(decoder.getPacketHandler());
                decoder.getAsyncDecoder().setPlayer(player);
            } else {
                player.updateHandlersLater = true;
            }

            if (PacketInterceptorResolver.isEncoderPositionResolved()
                    && !isConfigurationActive(channel.pipeline(), PacketBound.CLIENT)) {

                HoneyPacketEncodePreparation encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
                if (encoder == null) {
                    throw new MissingHandlerException("Encoder");
                }

                player.setPacketHandler(encoder.getPacketHandler());
                encoder.getAsyncDecoder().setPlayer(player);
            } else {
                player.updateHandlersLater = true;
            }

            ChannelHandler preResolvedPacketHandler = channel.pipeline().get("pre-resolved-packet-handler");
            if (preResolvedPacketHandler != null) {
                PreResolvedPacketHandler preResolvedPacketHandler0 = (PreResolvedPacketHandler) preResolvedPacketHandler;

                HoneyPacketHandler packetHandler = preResolvedPacketHandler0.getPacketHandler();
                player.setPacketHandler(packetHandler);

                togglePreResolvedBounds(channel, preResolvedPacketHandler0);
            }
        }
    }
}
