package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.handler.*;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class ServerPacketManager {

    public <WP extends SourcedWrappedPacket<WP>> void writeForcedPacket(HoneyPlayer player, ObjectWrappedPacket<WP> packet) {
        HoneyPacketEncodePreparation encoder = getEncoder(player.channel);
        if (encoder == null) {
            return;
        }

        encoder.writeForcedPacket(packet.getHandle());
    }

    public void sendPacketSilently(Channel channel, Object obj) {
       channel.pipeline().context(MinecraftHandlers.ENCODER).writeAndFlush(obj);
    }

    public void receivePacketSilently(Channel channel, Object obj) {
        channel.pipeline().context(MinecraftHandlers.DECODER).fireChannelRead(obj);
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
