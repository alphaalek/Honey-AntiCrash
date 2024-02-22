package me.alek.honey.netty.injector;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.handler.UpdatableChannel;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NMSUtils;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;

public class ServerPacketManager {

    public void writePacket(HoneyPlayer player, Object rawPacket, boolean flushAfter) {
        writePacket(player.channel, rawPacket, flushAfter);
    }

    public <WP extends WrappedPacket<WP>> void writePacket(HoneyPlayer player, WrappedPacket<WP> packet, boolean flushAfter) {
        writePacket(player, packet.getHandle(), flushAfter);
    }

    public void writePacket(Channel channel, Object rawPacket, boolean flushAfter) {
        if (!flushAfter)
            channel.write(rawPacket);
        else
            channel.writeAndFlush(rawPacket);
    }

    public <WP extends WrappedPacket<WP>> void writePacket(Channel channel, WrappedPacket<WP> packet, boolean flushafter) {
        writePacket(channel, packet.getHandle(), flushafter);
    }

    public void flushPackets(HoneyPlayer player) {
        flushPackets(player.channel);
    }

    public void flushPackets(Channel channel) {
        channel.flush();
    }

    public void receivePacket(HoneyPlayer player, Object rawPacket) {
        receivePacket(player.channel, rawPacket);
    }

    public void receivePacket(Channel channel, Object rawPacket) {
        channel.pipeline().fireChannelRead(rawPacket);
    }

    public UpdatableChannel getHandler(Channel channel) {
        return (UpdatableChannel) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getHandlerName());
    }

    public UpdatableChannel getDecoder(Channel channel) {
        return (UpdatableChannel) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getDecoderName());
    }
}
