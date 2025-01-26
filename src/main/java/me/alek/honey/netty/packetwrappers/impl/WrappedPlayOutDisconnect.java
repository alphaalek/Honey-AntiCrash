package me.alek.honey.netty.packetwrappers.impl;

import com.github.retrooper.packetevents.protocol.nbt.codec.NBTCodec;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.netty.channel.Channel;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteOutWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class WrappedPlayOutDisconnect extends ByteOutWrappedPacket<WrappedPlayOutDisconnect> {

    public static WrappedPlayOutDisconnect of(PacketState state, Component reason) {
        if (state == PacketState.PLAY) {
            return new WrappedPlayOutDisconnect(PacketTypes.Play.Server.DISCONNECT, reason);
        } else if (state == PacketState.LOGIN) {
            return new WrappedPlayOutDisconnect(PacketTypes.Login.Server.DISCONNECT, reason);
        } else if (state == PacketState.CONFIGURATION) {
            return new WrappedPlayOutDisconnect(PacketTypes.Configuration.Server.DISCONNECT, reason);
        } else {
            throw new IllegalArgumentException("Can't make disconnect packet in this state.");
        }
    }

    private final Component reason;

    public WrappedPlayOutDisconnect(PacketType type, Component reason) {
        super(type);
        this.reason = reason;
    }

    @Override
    public void write(PacketDataSerializer serializer) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_3)) {
            NBTCodec.writeNBTToBuffer(serializer, NetProtocol.getProtocol().toPESVersion(), AdventureSerializer.toNbt(this.reason));
        } else {
            serializer.writeString(AdventureSerializer.toJson(this.reason));
        }
    }

    @Override
    public void postWrite(Channel channel) {
        channel.close();
    }
}
