package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.async.WorkerPool;
import me.alek.honey.netty.utility.NMS;
import org.bukkit.Bukkit;

public class SpigotPipelineInjector {

    public void injectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get("splitter") == null) {
                channel.close();
                return;
            }

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();

            if (channel.pipeline().get(handlerName) != null
                    && channel.pipeline().get(filterName) != null) {
                return;
            }

            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }

            if (channel.pipeline().get(filterName) != null) {
                channel.pipeline().remove(filterName);
            }

            HoneyPacketHandler packetHandler = new HoneyPacketHandler();
            channel.pipeline().addBefore("packet_handler", handlerName, packetHandler);
            channel.pipeline().addAfter("splitter", filterName, new InboundPacketFilter(channel, packetHandler));
        }
    }

    public void relocateHandlers(Channel channel, HoneyPacketHandler packetHandler) throws MissingHandlerException {
        synchronized (channel) {
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();

            if (filter != null) {
                filter = filter.copy();
                channel.pipeline().remove(filterName);
            } else {
                filter = new InboundPacketFilter(channel, packetHandler);
            }

            String[] requiredHandlers = new String[3];
            channel.pipeline().addAfter(channel.pipeline().get(
                    "decompress") != null ? "decompress" : /* no network compression */ "splitter", requiredHandlers[0] = filterName, filter);

            AsyncDecoder asyncDecoder = new AsyncDecoder(WorkerPool.ASYNC_PACKET_DECODER_POOL);
            channel.pipeline().addBefore(requiredHandlers[1] = PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), HoneyPlugin.get().getPacketLibrary().getDecoderName(),
                    new InboundPacketDecoder(channel, asyncDecoder, packetHandler));
            channel.pipeline().addAfter(requiredHandlers[2] = PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), HoneyPlugin.get().getPacketLibrary().getEncoderName(),
                    new OutboundPacketEncoder(channel, asyncDecoder, packetHandler));

            for (String requiredHandler : requiredHandlers) {
                if (channel.pipeline().get(requiredHandler) == null)
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
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();
            String encoderName = HoneyPlugin.get().getPacketLibrary().getEncoderName();

            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }
            if (channel.pipeline().get(filterName) != null) {
                channel.pipeline().remove(filterName);
            }
            if (channel.pipeline().get(decoderName) != null) {
                channel.pipeline().remove(decoderName);
            }
            if (channel.pipeline().get(encoderName) != null) {
                channel.pipeline().remove(encoderName);
            }

            channel.close();
        }
    }

    public void updatePlayer(HoneyPlayer player) throws MissingHandlerException {
        Channel channel = player.channel;
        if (channel == null) throw new MissingHandlerException("Channel");

        synchronized (channel) {
            HoneyPacketHandler handler = HoneyPlugin.get().getPacketLibrary().getPacketManager().getHandler(channel);
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            if ((handler == null && !player.removedPacketHandlerFromPipeline)
                    || filter == null) throw new MissingHandlerException("Handlers: " + handler + ", " + filter + ", " + player.removedPacketHandlerFromPipeline);

            if (handler != null) handler.setPlayer(player);
            filter.setPlayer(player);

            if (PacketInterceptorResolver.isDecoderPositionResolved()) {
                InboundPacketDecoder decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
                if (decoder == null) throw new MissingHandlerException("Decoder");

                decoder.getAsyncDecoder().setPlayer(player);
            }
            else player.updateHandlersLater = true;

            if (PacketInterceptorResolver.isEncoderPositionResolved()) {
                OutboundPacketEncoder encoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getEncoder(channel);
                if (encoder == null) throw new MissingHandlerException("Encoder");

                encoder.getAsyncDecoder().setPlayer(player);
            }
            else player.updateHandlersLater = true;

            if (handler != null) channel.pipeline().remove(handler);
            player.removedPacketHandlerFromPipeline = true;
        }
    }
}
