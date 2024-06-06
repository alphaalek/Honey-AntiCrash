package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;

import java.util.Map;

public class CodecFactory {

    public static EmulatorCodec.In<Object, PacketDataSerializer> createInboundCodec(PacketState state) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            return new EmulatorCodec.In<Object, PacketDataSerializer>() {

                @Override
                public Object decodeModern(ByteBuf buf) throws CodecException {
                    return null;
                }

                @Override
                public Object decodeLegacy(ByteBuf buf, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) throws CodecException {
                    return null;
                }
            };
        }

        WrappedEnumProtocol enumProtocol = WrappedEnumProtocol.getEnumProtocolDelegate(PacketBound.SERVER, state);
        return new EmulatorCodec.In<Object, PacketDataSerializer>() {

            @Override
            public Object decodeModern(ByteBuf buf) throws CodecException {
                PacketDataSerializer dataSerializer = new PacketDataSerializer(buf);
                int id = dataSerializer.readVarInt();
                if (id < 0) {
                    throw new CodecException(id, "Bad Packet ID");
                }

                try {
                    if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
                        return enumProtocol.createModernPacket1_20_1(PacketBound.SERVER, dataSerializer, id);
                    } else {
                        return enumProtocol.createModernPacket1_20_2(dataSerializer, id);
                    }
                } catch (Exception ex) {
                    throw new CodecException(id, ex);
                }
            }

            @Override
            public Object decodeLegacy(ByteBuf buf, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) throws CodecException {
                PacketDataSerializer dataSerializer = new PacketDataSerializer(buf);
                int id = dataSerializer.readVarInt();
                if (id < 0) {
                    throw new CodecException(id, "Bad Packet ID");
                }

                Object packet;
                try {
                    packet = enumProtocol.createLegacyPacket(PacketBound.SERVER, id);
                } catch (Exception ex) {
                    throw new CodecException(id, ex);
                }

                if (packet == null) {
                    throw new CodecException(id, "Bad Packet ID");
                }

                codec.accept(PacketType.getPacketType(packet.getClass()), new LegacyCodec.Applier() {
                    @Override
                    public void apply(LegacyCodec codec) throws CodecException {
                        codec.apply(packet,
                                ReflectStructureCache.acquireStructure(packet.getClass())
                                        .withoutStatic().withTarget(packet),
                                dataSerializer);
                    }
                });

                return packet;
            }
        };
    }

    public static EmulatorCodec.Out<Object, PacketDataSerializer> createOutboundCodec(PacketState state) {
        return null;
    }
}
