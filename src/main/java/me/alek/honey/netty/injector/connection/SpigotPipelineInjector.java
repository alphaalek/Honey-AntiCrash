package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
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

            if (channel.pipeline().get(PacketLibrary.get().getHandlerName()) != null
                    || channel.pipeline().get(PacketLibrary.get().getDecoderName()) != null) {
                ejectChannel(channel);
            }

            InboundPacketDecoder decoder = new InboundPacketDecoder(channel);
            channel.pipeline().addAfter("splitter", PacketLibrary.get().getDecoderName(), decoder);
            channel.pipeline().addBefore("packet_handler", PacketLibrary.get().getHandlerName(), new DuplexChannelHandler(decoder));
        }
    }

    public boolean relocateHandlers(Channel channel) {
        synchronized (channel) {
            InboundPacketDecoder decoder = (InboundPacketDecoder) PacketLibrary.get().getPacketManager().getDecoder(channel);
            if (decoder != null) {
                channel.pipeline().remove(PacketLibrary.get().getDecoderName());

                if (channel.pipeline().get("decompress") != null) {
                    channel.pipeline().addAfter("decompress", PacketLibrary.get().getDecoderName(), new InboundPacketDecoder(decoder));
                    return true;
                }
            }
        }
        return false;
    }

    public void ejectChannel(Channel channel) {
        synchronized (channel) {
            if (channel.pipeline().get(PacketLibrary.get().getHandlerName()) != null) {
                channel.pipeline().remove(PacketLibrary.get().getHandlerName());
            }
            if (channel.pipeline().get(PacketLibrary.get().getDecoderName()) != null) {
                channel.pipeline().remove(PacketLibrary.get().getDecoderName());
            }
        }
    }

    public void updatePlayer(HoneyPlayer player) throws MissingHandlerException {
        Channel channel = player.channel;
        if (channel == null) {
            throw new MissingHandlerException();
        }

        UpdatableChannel handler = PacketLibrary.get().getPacketManager().getHandler(channel);
        UpdatableChannel decoder = PacketLibrary.get().getPacketManager().getDecoder(channel);
        if (handler == null || decoder == null) {
            throw new MissingHandlerException();
        }

        handler.setPlayer(player);
        decoder.setPlayer(player);
    }
}
