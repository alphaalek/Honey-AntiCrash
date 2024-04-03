package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.handler.*;
import me.alek.honey.packetwrappers.WrappedPacket;

public class ServerPacketManager {

    public void writePacket(HoneyPlayer player, Object rawPacket, boolean flushAfter) {
        writePacket(player.channel, rawPacket, flushAfter);
    }

    public <WP extends WrappedPacket<WP>> void writeForcedPacket(HoneyPlayer player, WrappedPacket<WP> packet) throws Exception {
        if (player == null || player.channel == null) return;

        ServerPacketManager packetManager = HoneyPlugin.get().getPacketLibrary().getPacketManager();

        OutboundPacketEncoder encoder = packetManager.getEncoder(player.channel);
        if (encoder == null) {
            return;
        }

        encoder.writeForcedPacket(packet.getHandle());
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

    public HoneyPacketHandler getHandler(Channel channel) {
        return (HoneyPacketHandler) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getHandlerName());
    }

    public InboundPacketFilter getFilter(Channel channel) {
        return (InboundPacketFilter) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getFilterName());
    }

    public InboundPacketDecoder getDecoder(Channel channel) {
        return (InboundPacketDecoder) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getDecoderName());
    }

    public OutboundPacketEncoder getEncoder(Channel channel) {
        return (OutboundPacketEncoder) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getEncoderName());
    }

    public HoneyDebugger getDebugger(Channel channel) {
        return (HoneyDebugger) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getDebuggerName());
    }
}
