package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.netty.injector.handler.async.AsyncDecoder;
import me.alek.honey.netty.injector.handler.async.WorkerPool;
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
                    || channel.pipeline().get(filterName) != null) {
                ejectChannel(channel);
            }

            channel.pipeline().addAfter("splitter", filterName, new InboundPacketFilter(channel));
            channel.pipeline().addBefore("packet_handler", handlerName, new DuplexChannelHandler());

            Bukkit.getLogger().info("Init " + channel.pipeline().toMap());

        }
    }

    public boolean relocateHandlers(Channel channel, HoneyPlayer player) {
        synchronized (channel) {
            InboundPacketFilter filter = HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(channel);
            if (filter != null) {
                String filterName = HoneyPlugin.get().getPacketLibrary().getFilterName();
                channel.pipeline().remove(filterName);

                if (channel.pipeline().get("decompress") != null) {
                    channel.pipeline().addAfter("decompress", filterName, filter.copy());

                    AsyncDecoder asyncDecoder = new AsyncDecoder(player, WorkerPool.ASYNC_PACKET_DECODER_POOL);

                    channel.pipeline().addBefore(PacketInterceptorResolver.getDecoderPosition(channel.pipeline()), HoneyPlugin.get().getPacketLibrary().getDecoderName(),
                            new InboundPacketDecoder(asyncDecoder, channel.pipeline()));
                    channel.pipeline().addAfter(PacketInterceptorResolver.getEncoderPosition(channel.pipeline()), HoneyPlugin.get().getPacketLibrary().getEncoderName(),
                            new OutboundPacketEncoder(channel, asyncDecoder));

                    return true;
                }
            }
        }
        return false;
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
