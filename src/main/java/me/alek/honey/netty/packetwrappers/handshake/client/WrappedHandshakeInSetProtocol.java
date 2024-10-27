package me.alek.honey.netty.packetwrappers.handshake.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;

public class WrappedHandshakeInSetProtocol extends ByteWrappedPacket<WrappedHandshakeInSetProtocol> {

    private final int protocolVersion;
    private final String host;
    private final int port;
    private final int intent;

    public WrappedHandshakeInSetProtocol(PacketDataSerializer serializer, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(serializer, packetContainer);
        this.protocolVersion = Types.VAR_INT.read(serializer);
        this.host = Types.UTF8.read(serializer);
        this.port = serializer.readUnsignedShort();
        this.intent = Types.VAR_INT.read(serializer);
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

    public int getIntent() {
        return this.intent;
    }
}
