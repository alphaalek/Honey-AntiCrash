package me.alek.honey.netty.packetwrappers.impl.handshake;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import org.bukkit.Bukkit;

public class WrappedHandshakeInSetProtocol extends ByteInWrappedPacket<WrappedHandshakeInSetProtocol> {

    @Property
    private final int protocolVersion;
    @Property
    private final String host;
    @Property
    private final int port;
    @Property
    private final int intent;

    public WrappedHandshakeInSetProtocol(PacketDataSerializer serializer, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(serializer, packetContainer);
        this.protocolVersion = serializer.readVarInt();
        this.host = serializer.readString(Short.MAX_VALUE);
        this.port = serializer.readUnsignedShort();
        this.intent = serializer.readVarInt();
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
