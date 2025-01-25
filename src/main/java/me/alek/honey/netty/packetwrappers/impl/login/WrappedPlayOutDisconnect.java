package me.alek.honey.netty.packetwrappers.impl.login;

import com.github.retrooper.packetevents.protocol.nbt.codec.NBTCodec;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteOutWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;

public class WrappedPlayOutDisconnect extends ByteOutWrappedPacket<WrappedPlayOutDisconnect> {

    public static WrappedPlayOutDisconnect of(PacketState state, NetProtocol protocol, Component reason) {
        if (state == PacketState.PLAY) {
            return new WrappedPlayOutDisconnect(PacketTypes.Play.Server.DISCONNECT, protocol, reason);
        } else if (state == PacketState.LOGIN) {
            return new WrappedPlayOutDisconnect(PacketTypes.Login.Server.DISCONNECT, protocol, reason);
        } else if (state == PacketState.CONFIGURATION) {
            return new WrappedPlayOutDisconnect(PacketTypes.Configuration.Server.DISCONNECT, protocol, reason);
        } else {
            throw new IllegalArgumentException("Can't make disconnect packet in this state.");
        }
    }

    private final Component reason;

    public WrappedPlayOutDisconnect(PacketType type, NetProtocol protocol, Component reason) {
        super(type, protocol);
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
