package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.async.WorkerPool;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpigotPipelineInjector {

    public void injectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get("splitter") == null) {
                channel.close();
                return;
            }

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
            String debuggerName = HoneyPlugin.get().getPacketLibrary().getDebuggerName();

            if (channel.pipeline().get(handlerName) != null
                    && channel.pipeline().get(filterName) != null
                    && channel.pipeline().get(debuggerName) != null) {
                return;
            }

            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }

            if (channel.pipeline().get(filterName) != null) {
                channel.pipeline().remove(filterName);
            }

            if (channel.pipeline().get(debuggerName) != null) {
                channel.pipeline().remove(debuggerName);
            }

            HoneyPacketHandler packetHandler = new HoneyPacketHandler();
            channel.pipeline().addBefore("packet_handler", handlerName, packetHandler);
            channel.pipeline().addBefore("packet_handler", debuggerName, new HoneyDebugger());
            channel.pipeline().addAfter("splitter", filterName, new InboundPacketFilter(channel, packetHandler));
        }
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

    public void relocateHandlers(Channel channel, HoneyPacketHandler packetHandler) throws MissingHandlerException {
        synchronized (channel) {
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();

            if (channel.pipeline().get("decompress") != null) {
                channel.pipeline().remove(filter);
                channel.pipeline().addAfter("decompress", filterName, filter);
            }

            String[] requiredHandlers = new String[3];
            requiredHandlers[0] = filterName;

            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
            String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();

            if (PacketInterceptorResolver.getDecoderPosition(channel.pipeline()) != null) {
                requiredHandlers[1] = decoderName;
            }

            if (PacketInterceptorResolver.getEncoderPosition(channel.pipeline()) != null) {
                requiredHandlers[2] = encoderName;
            }

            AsyncDecoder asyncDecoder = null;

            if (channel.pipeline().get(decoderName) == null && PacketInterceptorResolver.isDecoderPositionResolved()) {
                channel.pipeline().addBefore(PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), decoderName,
                        new InboundPacketDecoder(channel, asyncDecoder = new AsyncDecoder(WorkerPool.ASYNC_PACKET_DECODER_POOL), packetHandler));
            }

            if (channel.pipeline().get(encoderName) == null && PacketInterceptorResolver.isEncoderPositionResolved()) {
                channel.pipeline().addAfter(PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), encoderName,
                        new OutboundPacketEncoder(channel, (asyncDecoder != null ? asyncDecoder : new AsyncDecoder(WorkerPool.ASYNC_PACKET_DECODER_POOL)), packetHandler));
            }

            for (String requiredHandler : requiredHandlers) {
                if (requiredHandler != null && channel.pipeline().get(requiredHandler) == null)
                    throw new MissingHandlerException("No \"" + requiredHandler + "\" channel handler in pipeline!");
            }
        }
    }

    public void ejectChannel(Channel channel) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> ejectChannel(channel));
            return;
        }
        synchronized (channel) {
            HoneyPlugin.get().getPacketLibrary().getServerInjector().removeNetworkManager(channel.pipeline().get("packet_handler"));

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }

            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
            if (channel.pipeline().get(filterName) != null) {
                channel.pipeline().remove(filterName);
            }

            String debuggerName = HoneyPlugin.get().getPacketLibrary().getDebuggerName();
            if (channel.pipeline().get(debuggerName) != null) {
                channel.pipeline().remove(debuggerName);
            }

            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
            if (channel.pipeline().get(decoderName) != null) {
                channel.pipeline().remove(decoderName);
            }

            String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();
            if (channel.pipeline().get(encoderName) != null) {
                channel.pipeline().remove(encoderName);
            }

            channel.close();
        }
    }

    public void decrementTaskSupplierCount(Channel channel) {
        synchronized (channel) {
            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
            if (channel.pipeline().get(decoderName) != null) {
                InboundPacketDecoder decoder = (InboundPacketDecoder) channel.pipeline().get(decoderName);
                decoder.getAsyncDecoder().farewell();
            }

            String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();
            if (channel.pipeline().get(encoderName) != null) {
                OutboundPacketEncoder encoder = (OutboundPacketEncoder) channel.pipeline().get(encoderName);
                encoder.getAsyncDecoder().farewell();
            }
        }
    }

    public void updatePlayer(HoneyPlayer player) throws MissingHandlerException {
        Channel channel = player.channel;
        if (channel == null) {
            throw new MissingHandlerException("Channel");
        }

        synchronized (channel) {
            HoneyPacketHandler handler = HoneyPlugin.get().getPacketLibrary().getPacketManager().getHandler(channel);
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            HoneyDebugger debugger = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDebugger(channel);
            if ((handler == null && !player.removedPacketHandlerFromPipeline)
                    || filter == null || debugger == null) {
                throw new MissingHandlerException("Handler: " + handler + ", Filter: " + filter + ", Debugger: " + debugger + ", Removed: " + player.removedPacketHandlerFromPipeline);
            }

            if (handler != null) {
                handler.setPlayer(player);
            }
            filter.setPlayer(player);
            debugger.setPlayer(player);

            if (PacketInterceptorResolver.isDecoderPositionResolved()) {
                InboundPacketDecoder decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                if (decoder == null) {
                    throw new MissingHandlerException("Decoder");
                }

                decoder.getAsyncDecoder().setPlayer(player);
            } else {
                player.updateHandlersLater = true;
            }

            if (PacketInterceptorResolver.isEncoderPositionResolved()) {
                OutboundPacketEncoder encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
                if (encoder == null) {
                    throw new MissingHandlerException("Encoder");
                }

                encoder.getAsyncDecoder().setPlayer(player);
            } else {
                player.updateHandlersLater = true;
            }

            if (handler != null) {
                channel.pipeline().remove(handler);
            }
            player.removedPacketHandlerFromPipeline = true;
        }
    }
}
