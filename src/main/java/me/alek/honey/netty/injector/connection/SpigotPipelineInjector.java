package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
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

            channel.pipeline().addBefore("packet_handler", PacketLibrary.get().getHandlerName(), new DuplexChannelHandler(channel));
            channel.pipeline().addAfter("splitter", PacketLibrary.get().getDecoderName(), new InboundPacketDecoder(channel));
        }
    }

    public boolean relocateHandlers(Channel channel) {
        InboundPacketDecoder decoder = (InboundPacketDecoder) PacketLibrary.get().getPacketManager().getDecoder(channel);
        if (decoder != null) {
            channel.pipeline().remove(PacketLibrary.get().getDecoderName());

            if (channel.pipeline().get("decompress") != null) {
                channel.pipeline().addAfter("decompress", PacketLibrary.get().getDecoderName(), new InboundPacketDecoder(decoder));
                return true;
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

    public void updatePlayer(Player player) throws MissingHandlerException {
        Channel channel = NMSUtils.getChannel(player);
        if (channel == null) {
            throw new MissingHandlerException();
        }

        synchronized (channel) {
            UpdatableChannel handler = PacketLibrary.get().getPacketManager().getHandler(channel);
            UpdatableChannel decoder = PacketLibrary.get().getPacketManager().getDecoder(channel);
            if (handler == null || decoder == null) {
                throw new MissingHandlerException();
            }

            handler.setPlayer(player);
            decoder.setPlayer(player);
        }
    }
}
