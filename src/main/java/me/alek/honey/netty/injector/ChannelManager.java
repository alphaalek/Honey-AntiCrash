package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.handler.*;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;

public class ChannelManager {

    public <WP extends SourcedWrappedPacket<WP>> void writePacket(HoneyPlayer player, ObjectWrappedPacket<WP> packet) {
        HoneyPacketOutStream out = getOutStream(player.channel);
        if (out == null) {
            return;
        }

        out.writePacket(packet.getHandle());
    }

    public InboundPacketFilter getFilter(Channel channel) {
        return (InboundPacketFilter) channel.pipeline().get(HoneyPlugin.HONEY_FILTER_HANDLER_NAME);
    }

    public HoneyPacketInStream getInStream(Channel channel) {
        return (HoneyPacketInStream) channel.pipeline().get(HoneyPlugin.HONEY_DECODER_HANDLER_NAME);
    }

    public HoneyPacketOutStream getOutStream(Channel channel) {
        return (HoneyPacketOutStream) channel.pipeline().get(HoneyPlugin.HONEY_ENCODER_HANDLER_NAME);
    }
}
