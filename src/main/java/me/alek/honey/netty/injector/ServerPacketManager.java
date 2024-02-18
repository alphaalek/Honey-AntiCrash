package me.alek.honey.netty.injector;

import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.handler.UpdatableChannel;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NMSUtils;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;

public class ServerPacketManager {

    public void writePacket(Player player, Object rawPacket) {
        writePacket(NMSUtils.getChannel(player), rawPacket);
    }

    public <WP extends WrappedPacket<WP>> void writePacket(Player player, WrappedPacket<WP> packet) {
        writePacket(player, packet.getHandle());
    }

    public void writePacket(Channel channel, Object rawPacket) {
        channel.write(rawPacket);
    }

    public <WP extends WrappedPacket<WP>> void writePacket(Channel channel, WrappedPacket<WP> packet) {
        writePacket(channel, packet.getHandle());
    }

    public void flushPackets(Player player) {
        flushPackets(NMSUtils.getChannel(player));
    }

    public void flushPackets(Channel channel) {
        channel.flush();
    }

    public void receivePacket(Player player, Object rawPacket) {
        receivePacket(NMSUtils.getChannel(player), rawPacket);
    }

    public void receivePacket(Channel channel, Object rawPacket) {
        channel.pipeline().fireChannelRead(rawPacket);
    }

    public UpdatableChannel getHandler(Channel channel) {
        return (UpdatableChannel) channel.pipeline().get(PacketLibrary.get().getHandlerName());
    }

    public UpdatableChannel getDecoder(Channel channel) {
        return (UpdatableChannel) channel.pipeline().get(PacketLibrary.get().getDecoderName());
    }
}
