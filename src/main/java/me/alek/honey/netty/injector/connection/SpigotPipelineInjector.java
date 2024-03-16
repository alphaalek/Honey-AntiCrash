package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.async.WorkerPool;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

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
                    || channel.pipeline().get(filterName) != null) {
                ejectChannel(channel);
            }

            channel.pipeline().addAfter("splitter", filterName, new InboundPacketFilter(channel));
            channel.pipeline().addBefore("packet_handler", handlerName, new DuplexChannelHandler());
        }
    }

    public void relocateHandlers(Channel channel, HoneyPlayer player) throws MissingHandlerException {
        synchronized (channel) {
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();

            if (filter != null) {
                filter = filter.copy();
                channel.pipeline().remove(filterName);
            } else {
                filter = new InboundPacketFilter(channel);
            }

            String[] requiredHandlers = new String[3];
            channel.pipeline().addAfter(channel.pipeline().get("decompress") != null ?"decompress" : "splitter", requiredHandlers[0] = filterName, filter);

            AsyncDecoder asyncDecoder = new AsyncDecoder(player, WorkerPool.ASYNC_PACKET_DECODER_POOL);
            channel.pipeline().addBefore(requiredHandlers[1] = PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), HoneyPlugin.get().getPacketLibrary().getDecoderName(),
                    new InboundPacketDecoder(asyncDecoder, channel.pipeline()));
            channel.pipeline().addAfter(requiredHandlers[2] = PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), HoneyPlugin.get().getPacketLibrary().getEncoderName(),
                    new OutboundPacketEncoder(channel, asyncDecoder));

            for (String requiredHandler : requiredHandlers) {
                if (channel.pipeline().get(requiredHandler) == null)
                    throw new MissingHandlerException("No \"" + requiredHandler + "\" channel handler in pipeline!");
            }
        }
    }

    public void ejectChannel(Channel channel) {
        synchronized (channel) {
            if (!channel.eventLoop().inEventLoop()) {
                channel.eventLoop().submit(() -> ejectChannel(channel));
                return;
            }

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();

            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }
            if (channel.pipeline().get(filterName) != null) {
                channel.pipeline().remove(filterName);
            }
        }
    }

    public void updatePlayer(HoneyPlayer player) throws MissingHandlerException {
        Channel channel = player.channel;
        if (channel == null) {
            throw new MissingHandlerException();
        }

        UpdatableChannel handler = HoneyPlugin.get().getPacketLibrary().getPacketManager().getHandler(channel);
        UpdatableChannel filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
        if (handler == null || filter == null) {
            throw new MissingHandlerException();
        }

        handler.setPlayer(player);
        filter.setPlayer(player);
    }
}
