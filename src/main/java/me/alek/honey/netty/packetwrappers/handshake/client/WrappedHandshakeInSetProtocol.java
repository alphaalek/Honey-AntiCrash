package me.alek.honey.netty.packetwrappers.handshake.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import org.bukkit.Bukkit;

public class WrappedHandshakeInSetProtocol extends ByteWrappedPacket<WrappedHandshakeInSetProtocol> {

    private final int protocolVersion;
    private final String host;
    private final int port;
    private final int intent;

    public WrappedHandshakeInSetProtocol(PacketDataSerializer serializer, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        super(serializer, packetContainer);
        this.protocolVersion = serializer.readVarInt();
        this.host = Types.UTF8.read(serializer, Short.MAX_VALUE);
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
