package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.GameMode;

import java.util.function.Supplier;

public class HoneyPacketCodec {

    public static void checkReadable(NetProtocol protocol, PacketType type, Object packetObject, ByteBuf buf) throws DecoderException {
        if (buf.isReadable()) {
            int end = buf.readerIndex();
            int remaining = buf.readableBytes();
            throw new DecoderException(
                    "Found " + remaining
                            + " (" + (end + buf.readableBytes()) + "/" + end + ") extra bytes while decoding packet "
                            + HoneyPlugin.get().getLogger().formatPacketType(type) + ","
                            + " protocol " + protocol.getName()
                            + (packetObject == null ? "" : ", properties " +
                            HoneyPlugin.get().getLogger().filterTooMuchData(
                                    HoneyPlugin.get().getLogger().formatPacketEntries(
                                            ReflectStructure.Provider.acquireStructure(packetObject.getClass())
                                                    .withStatic(false)
                                                    .withTarget(packetObject)
                                                    .readFieldEntries()
                                    ))
                            )
            );
        }
    }
}
