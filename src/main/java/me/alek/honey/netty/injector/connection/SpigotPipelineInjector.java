package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.MissingHoneyHandlerException;
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

            channel.pipeline().addBefore("packet_handler", PacketLibrary.get().getHandlerName(), new DuplexChannelHandler(channel));
            channel.pipeline().addBefore("decoder", PacketLibrary.get().getDecoderName(), new InboundPacketDecoder());
        }
    }

    public void ejectChannel(Channel channel) {
        channel.pipeline().remove(PacketLibrary.get().getHandlerName());
        channel.pipeline().remove(PacketLibrary.get().getDecoderName());
    }

    public void updatePlayer(Player player) throws MissingHoneyHandlerException {
        Channel channel = NMSUtils.getChannel(player);
        if (channel == null) {
            throw new MissingHoneyHandlerException();
        }

        synchronized (channel) {
            UpdatableChannel handler = PacketLibrary.get().getPacketManager().getHandler(channel);
            UpdatableChannel decoder = PacketLibrary.get().getPacketManager().getDecoder(channel);
            if (handler == null || decoder == null) {
                throw new MissingHoneyHandlerException();
            }

            handler.setPlayer(player);
            decoder.setPlayer(player);
        }
    }
}
