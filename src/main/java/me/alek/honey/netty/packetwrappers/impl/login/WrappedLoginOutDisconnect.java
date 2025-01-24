package me.alek.honey.netty.packetwrappers.impl.login;

import com.github.retrooper.packetevents.protocol.nbt.codec.NBTCodec;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteOutWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;

public class WrappedLoginOutDisconnect extends ByteOutWrappedPacket<WrappedLoginOutDisconnect> {

    private final Component reason;

    public WrappedLoginOutDisconnect(NetProtocol protocol, Component reason) {
        super(PacketTypes.Login.Server.DISCONNECT, protocol);
        this.reason = reason;
    }

    public void write(PacketDataSerializer serializer) {
        if (this.protocol.isNewerThanOrEqual(NetProtocol.v1_20_3)) {
            NBTCodec.writeNBTToBuffer(serializer, this.protocol.toPESVersion(), AdventureSerializer.toNbt(this.reason));
        } else {
            serializer.writeString(AdventureSerializer.toJson(this.reason));
        }
    }
}
