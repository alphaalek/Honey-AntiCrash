package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.handler.HoneyDebugger;
import me.alek.honey.netty.handler.HoneyPacketDecodePreparation;
import me.alek.honey.netty.handler.HoneyPacketEncodePreparation;
import me.alek.honey.netty.handler.InboundPacketFilter;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class ServerPacketManager {

    public void writePacket(HoneyPlayer player, Object rawPacket, boolean flushAfter) {
        writePacket(player.channel, rawPacket, flushAfter);
    }

    public <WP extends WrappedPacket<WP>> void writeForcedPacket(HoneyPlayer player, ObjectWrappedPacket<WP> packet) throws Exception {
        ServerPacketManager packetManager = HoneyPlugin.get().getPacketLibrary().getPacketManager();

        HoneyPacketEncodePreparation encoder = packetManager.getEncoder(player.channel);
        if (encoder == null) {
            return;
        }

        encoder.writeForcedPacket(packet.getHandle());
    }

    public <WP extends WrappedPacket<WP>> void writePacket(HoneyPlayer player, ObjectWrappedPacket<WP> packet, boolean flushAfter) {
        writePacket(player, packet.getHandle(), flushAfter);
    }

    public void writePacket(Channel channel, Object rawPacket, boolean flushAfter) {
        if (!flushAfter)
            channel.write(rawPacket);
        else
            channel.writeAndFlush(rawPacket);
    }

    public <WP extends WrappedPacket<WP>> void writePacket(Channel channel, ObjectWrappedPacket<WP> packet, boolean flushAfter) {
        writePacket(channel, packet.getHandle(), flushAfter);
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

    public boolean bothPreparationHandlerExists(Channel channel) {
        return getDecoder(channel) != null && getEncoder(channel) != null;
    }

    public InboundPacketFilter getFilter(Channel channel) {
        return (InboundPacketFilter) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getFilterName());
    }

    public HoneyPacketDecodePreparation getDecoder(Channel channel) {
        return (HoneyPacketDecodePreparation) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getDecoderName());
    }

    public HoneyPacketEncodePreparation getEncoder(Channel channel) {
        return (HoneyPacketEncodePreparation) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getEncoderName());
    }

    public HoneyDebugger getDebugger(Channel channel) {
        return (HoneyDebugger) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getDebuggerName());
    }
}
