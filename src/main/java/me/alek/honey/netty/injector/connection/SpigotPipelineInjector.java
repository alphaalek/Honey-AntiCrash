package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.handler.DuplexChannelHandler;
import me.alek.honey.netty.injector.handler.InboundPacketDecoder;
import me.alek.honey.netty.injector.handler.UpdatableChannel;
import me.alek.honey.netty.utility.NMSUtils;
import org.bukkit.entity.Player;

public class SpigotPipelineInjector {

    public void injectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get("splitter") == null) {
                channel.close();
                return;
            }

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();

            if (channel.pipeline().get(handlerName) != null
                    || channel.pipeline().get(decoderName) != null) {
                ejectChannel(channel);
            }

            InboundPacketDecoder decoder = new InboundPacketDecoder(channel);
            channel.pipeline().addAfter("splitter", decoderName, decoder);
            channel.pipeline().addBefore("packet_handler", handlerName, new DuplexChannelHandler(decoder));
        }
    }

    public boolean relocateHandlers(Channel channel) {
        synchronized (channel) {
            InboundPacketDecoder decoder = (InboundPacketDecoder) HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
            if (decoder != null) {
                String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();

                channel.pipeline().remove(decoderName);

                if (channel.pipeline().get("decompress") != null) {
                    channel.pipeline().addAfter("decompress", decoderName, new InboundPacketDecoder(decoder));
                    return true;
                }
            }
        }
        return false;
    }

    public void ejectChannel(Channel channel) {
        synchronized (channel) {

            String handlerName = HoneyPlugin.get().getPacketLibrary().getHandlerName();
            String decoderName = HoneyPlugin.get().getPacketLibrary().getDecoderName();

            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }
            if (channel.pipeline().get(decoderName) != null) {
                channel.pipeline().remove(decoderName);
            }
        }
    }

    public void updatePlayer(HoneyPlayer player) throws MissingHandlerException {
        Channel channel = player.channel;
        if (channel == null) {
            throw new MissingHandlerException();
        }

        UpdatableChannel handler = HoneyPlugin.get().getPacketLibrary().getPacketManager().getHandler(channel);
        UpdatableChannel decoder = HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
        if (handler == null || decoder == null) {
            throw new MissingHandlerException();
        }

        handler.setPlayer(player);
        decoder.setPlayer(player);
    }
}
