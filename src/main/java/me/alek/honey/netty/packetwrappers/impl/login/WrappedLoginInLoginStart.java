package me.alek.honey.netty.packetwrappers.impl.login;

import com.github.retrooper.packetevents.util.crypto.MinecraftEncryptionUtil;
import com.github.retrooper.packetevents.util.crypto.SignatureData;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.UUID;

public class WrappedLoginInLoginStart extends ByteInWrappedPacket<WrappedLoginInLoginStart> {

    public static final int USERNAME_MAX_LENGTH = 16;

    @Property
    private final String username;
    @Property
    private UUID UUID;
    @Property
    private SignatureData signatureData;

    public WrappedLoginInLoginStart(PacketDataSerializer serializer, PacketContainer<WrappedLoginInLoginStart> packet) {
        super(serializer, packet);

        this.username = serializer.readString(USERNAME_MAX_LENGTH);
        NetProtocol protocol = packet.getProtocol();
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_19)) {
            if (protocol.isOlderThan(NetProtocol.v1_19_4) && serializer.readBoolean()) {
                this.signatureData = new SignatureData(
                        Instant.ofEpochMilli(serializer.readLong()), MinecraftEncryptionUtil.publicKey(serializer.readByteArray(512)), serializer.readByteArray(4096));
            }

            if (protocol.isNewerThanOrEqual(NetProtocol.v1_19_1)) {
                if (protocol.isOlderThan(NetProtocol.v1_20_2) && !serializer.readBoolean()) {
                    return;
                }
                this.UUID = serializer.readUUID();
            }
        }
    }

    public String getUsername() {
        return this.username;
    }

    public UUID getUUID() {
        return this.UUID;
    }

    public SignatureData getSignatureData() {
        return this.signatureData;
    }
}
