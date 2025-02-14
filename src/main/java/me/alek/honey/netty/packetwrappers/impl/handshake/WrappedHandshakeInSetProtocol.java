package me.alek.honey.netty.packetwrappers.impl.handshake;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.packetwrappers.protocol.ClientIntention;

public class WrappedHandshakeInSetProtocol extends ByteInWrappedPacketImpl<WrappedHandshakeInSetProtocol> {

    @Property
    private final int protocolVersion;
    @Property
    private final String host;
    @Property
    private final int port;
    @Property
    private final ClientIntention intention;

    public WrappedHandshakeInSetProtocol(PacketDataSerializer serializer, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(serializer, packetContainer);
        this.protocolVersion = serializer.readVarInt();
        this.host = serializer.readString();
        this.port = serializer.readUnsignedShort();
        this.intention = ClientIntention.values()[serializer.readVarInt() - 1];
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public ClientIntention getIntention() {
        return this.intention;
    }
}
