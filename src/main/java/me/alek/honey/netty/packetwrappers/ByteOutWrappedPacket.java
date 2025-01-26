package me.alek.honey.netty.packetwrappers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

public class ByteOutWrappedPacket<WP extends WrappedPacket<WP>> implements ByteWrappedPacket<WP> {

    protected final PacketType type;

    public ByteOutWrappedPacket(PacketType type) {
        this.type = type;
    }

    public void write(HoneyPlayer player) {
        PacketDataSerializer serializer = new PacketDataSerializer();
        serializer.writeVarInt(this.type.getMinecraftID().get());
        write(serializer);

        ChannelFuture future = player.channel.writeAndFlush(serializer);
        future.addListener(f -> postWrite(player.channel));
    }

    public void write(PacketDataSerializer serializer) {
    }

    public void postWrite(Channel channel) {
    }

    @Override
    public NetProtocol getProtocol() {
        return NetProtocol.getProtocol();
    }

    @Override
    public PacketType getType() {
        return this.type;
    }
}
