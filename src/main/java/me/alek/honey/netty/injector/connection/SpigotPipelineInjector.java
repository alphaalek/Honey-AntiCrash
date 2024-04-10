package me.alek.honey.netty.injector.connection;

import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.injector.ChannelProxy;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.async.AsyncDecoder;
import me.alek.honey.async.WorkerPool;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class SpigotPipelineInjector {

    public void injectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get("splitter") == null) {
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

            channel.pipeline().addBefore("packet_handler", debuggerName, new HoneyDebugger());
            channel.pipeline().addAfter("splitter", filterName, new InboundPacketFilter(channel));

            HoneyPacketHandler packetHandler = new HoneyPacketHandler();
            if (!PacketInterceptorResolver.isDecoderPositionResolved() || !PacketInterceptorResolver.isEncoderPositionResolved()) {
                channel.pipeline().addBefore("packet_handler", "pre-resolved-packet-handler", new PreResolvedPacketHandler(packetHandler));
            } else {
                setupPreparationHandlers(channel, packetHandler);
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
            if (pipeline.get(decoderName) != null && PacketInterceptorResolver.isDecoderPositionResolved()) {
                if (handlers.indexOf(decoderName) != handlers.indexOf(PacketInterceptorResolver.getDecoderPosition(pipeline)) - 1) {
                    InboundPacketDecoder decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);

                    pipeline.remove(decoderName);
                    pipeline.addBefore(PacketInterceptorResolver.getDecoderPosition(pipeline), decoderName, decoder);
                }
            }

            String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();
            if (pipeline.get(encoderName) != null && PacketInterceptorResolver.isEncoderPositionResolved()) {
                if (handlers.indexOf(encoderName) != handlers.indexOf(PacketInterceptorResolver.getEncoderPosition(pipeline)) + 1) {
                    OutboundPacketEncoder encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);

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

        if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel) == null && PacketInterceptorResolver.isDecoderPositionResolved()) {
            channel.pipeline().addBefore(PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), decoderName,
                    new InboundPacketDecoder(channel, asyncDecoder, packetHandler));
        }

        if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) == null && PacketInterceptorResolver.isEncoderPositionResolved()) {
            channel.pipeline().addAfter(PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), encoderName,
                    new OutboundPacketEncoder(channel, asyncDecoder, packetHandler));
        }
    }

    public void relocateHandlers(HoneyPlayer player, Channel channel) throws MissingHandlerException {
        synchronized (channel) {
            if (channel.pipeline().get("decompress") != null) {
                InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);

                channel.pipeline().remove(filter);
                channel.pipeline().addAfter("decompress", HoneyPlugin.get().getPacketLibrary().getFilterName(), filter);
            }

            if (/* is PLAY game state */ player != null && !PacketInterceptorResolver.isDecoderPositionResolved()) {
                PacketInterceptorResolver.getDecoderPosition(channel.pipeline());
            }

            if (/* is PLAY game state */ player != null && !PacketInterceptorResolver.isEncoderPositionResolved()) {
                PacketInterceptorResolver.getEncoderPosition(channel.pipeline());
            }

            ChannelHandler preResolvedPacketHandler = channel.pipeline().get("pre-resolved-packet-handler");
            if (preResolvedPacketHandler != null) {
                PreResolvedPacketHandler preResolvedPacketHandler0 = (PreResolvedPacketHandler) preResolvedPacketHandler;
                setupPreparationHandlers(channel, preResolvedPacketHandler0.getPacketHandler());
            }

            if (!HoneyPlugin.get().getPacketLibrary().getPacketManager().bothPreparationHandlerExists(channel)) {
                if (player != null && player.packetHandler != null) {
                    setupPreparationHandlers(channel, player.packetHandler);
                } else {
                    setupPreparationHandlers(channel, new HoneyPacketHandler());
                }

                if (!HoneyPlugin.get().getPacketLibrary().getPacketManager().bothPreparationHandlerExists(channel)) {
                    throw new MissingHandlerException();
                }
            }

            if (player != null && player.updateHandlersLater &&
                    PacketInterceptorResolver.isDecoderPositionResolved() && PacketInterceptorResolver.isEncoderPositionResolved()) {
                try {
                    HoneyPlugin.get().getPacketLibrary().getPipelineInjector().updatePlayer(player);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                player.updateHandlersLater = false;
            }

            ensureCorrectPipelineOrder(channel);
        }
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
                InboundPacketDecoder decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                decoder.getAsyncDecoder().farewell();
            } else if (HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel) != null) {
                OutboundPacketEncoder encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
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
            InboundPacketDecoder decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
            if (decoder != null) {
                return decoder.getPacketHandler();
            }
        }
        if (PacketInterceptorResolver.isEncoderPositionResolved()) {
            OutboundPacketEncoder encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
            if (encoder != null) {
                return encoder.getPacketHandler();
            }
        }

        throw new MissingHandlerException();
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

            if (PacketInterceptorResolver.isDecoderPositionResolved()) {
                InboundPacketDecoder decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                if (decoder == null) {
                    throw new MissingHandlerException("Decoder");
                }

                player.setPacketHandler(decoder.getPacketHandler());
                decoder.getAsyncDecoder().setPlayer(player);
            } else {
                player.updateHandlersLater = true;
            }

            if (PacketInterceptorResolver.isEncoderPositionResolved()) {
                OutboundPacketEncoder encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
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
                packetHandler.setPlayer(player);
                player.packetHandler = packetHandler;

                channel.pipeline().remove("pre-resolved-packet-handler");
            }
        }
    }
}
