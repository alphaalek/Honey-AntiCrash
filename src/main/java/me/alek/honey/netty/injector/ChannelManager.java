package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.handler.*;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;

public class ChannelManager {

    public <WP extends SourcedWrappedPacket<WP>> void writeForcedPacket(HoneyPlayer player, ObjectWrappedPacket<WP> packet) {
        HoneyPacketOutStream out = getOutStream(player.channel);
        if (out == null) {
            return;
        }

        out.writeForcedPacket(packet.getHandle());
    }

    public void sendPackets(Channel channel, ByteBuf buf) {
        channel.writeAndFlush(buf);
    }

    public void receivePacketSilently(Channel channel, Object obj) {
        channel.pipeline().context(MinecraftHandlers.DECODER).fireChannelRead(obj);
    }

    public boolean bothStreamBoundsHandlersExists(Channel channel) {
        return getInStream(channel) != null && getOutStream(channel) != null;
    }

    public InboundPacketFilter getFilter(Channel channel) {
        return (InboundPacketFilter) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getFilterName());
    }

    public HoneyPacketInStream getInStream(Channel channel) {
        return (HoneyPacketInStream) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getInStreamName());
    }

    public HoneyPacketOutStream getOutStream(Channel channel) {
        return (HoneyPacketOutStream) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getOutStreamName());
    }

    public HoneyDebugger getDebugger(Channel channel) {
        return (HoneyDebugger) channel.pipeline().get(HoneyPlugin.get().getPacketLibrary().getDebuggerName());
    }
}
