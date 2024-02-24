package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.async.WorkerPool;

public class SpigotPipelineInjector {

    public void injectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get("splitter") == null) {
                channel.close();
                return;
            }

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            String decoderName = HoneyPlugin.get().getPacketLibrary().getFilterName();

            if (channel.pipeline().get(handlerName) != null
                    || channel.pipeline().get(decoderName) != null) {
                ejectChannel(channel);
            }

            InboundPacketFilter filter = new InboundPacketFilter(channel);
            channel.pipeline().addAfter("splitter", decoderName, filter);
            channel.pipeline().addBefore("packet_handler", handlerName, new DuplexChannelHandler(filter));
        }
    }

    public boolean relocateHandlers(Channel channel, HoneyPlayer player) {
        synchronized (channel) {
            InboundPacketFilter filter = (InboundPacketFilter) HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            if (filter != null) {
                String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();

                channel.pipeline().remove(filterName);

                if (channel.pipeline().get("decompress") != null) {
                    channel.pipeline().addAfter("decompress", filterName, new InboundPacketFilter(filter));

                    AsyncDecoder asyncDecoder = new AsyncDecoder(player, WorkerPool.ASYNC_PACKET_DECODER_POOL);
                    channel.pipeline().replace(channel.pipeline().get("decoder"), "decoder", new InboundPacketDecoder(asyncDecoder));
                    channel.pipeline().replace(channel.pipeline().get("encoder"), "encoder", new OutboundPacketEncoder(channel, asyncDecoder));
                    return true;
                }
            }
        }
        return false;
    }

    public void ejectChannel(Channel channel) {
        synchronized (channel) {

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
