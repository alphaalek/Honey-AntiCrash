package me.alek.honey.netty.packetwrappers;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;

public class ByteOutWrappedPacket<WP extends WrappedPacket<WP>> implements ByteWrappedPacket<WP> {

    protected final PacketType type;
    protected final NetProtocol protocol;

    public ByteOutWrappedPacket(PacketType type, NetProtocol protocol) {
        this.type = type;
        this.protocol = protocol;
    }

    public void sendSilently(Channel channel) {
        PacketDataSerializer serializer = new PacketDataSerializer();
        write(serializer);

        HoneyPlugin.get().getPacketLibrary().getPacketManager().sendPacketSilently(channel, serializer);
    }

    public void write(PacketDataSerializer serializer) {

    }

    @Override
    public NetProtocol getProtocol() {
        return this.protocol;
    }

    @Override
    public PacketType getType() {
        return this.type;
    }
}
