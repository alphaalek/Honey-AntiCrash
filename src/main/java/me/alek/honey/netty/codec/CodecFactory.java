package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.FieldAccessor;
import me.alek.honey.netty.util.MethodInvoker;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

public class CodecFactory {

    public static Object getProtocolInfo1_20_5(PacketBound bound, PacketState state) {
        Function<Class<Object>, IStructureModifier<Object>> get = c -> ReflectStructureCache.acquireStructure(c)
                .withType(NMS.getProtocolInfoClass()).withTarget(null);

        int index = bound.equals(PacketBound.SERVER) ? 0 : 1;

        switch (state) {
            case HANDSHAKE:
                if (bound == PacketBound.CLIENT) {
                    throw new NoSuchElementException();
                }
                return get.apply(Protocols.HANDSHAKE_CLASS).readField(0);
            case PLAY:
                return get.apply(Protocols.GAME_CLASS).readField(index);
            case STATUS:
                return get.apply(Protocols.STATUS_CLASS).readField(index);
            case LOGIN:
                return get.apply(Protocols.LOGIN_CLASS).readField(index);
            case CONFIGURATION:
                return get.apply(Protocols.CONFIGURATION_CLASS).readField(index);
            default:
                return null;
        }
    }

    public static Collection<FieldAccessor<Object>> getPacketTypes1_20_5(PacketState state) {
        Function<Class<Object>, Collection<FieldAccessor<Object>>> get = c -> ReflectStructureCache.acquireStructure(c)
                .withType(NMS.getPacketTypeClass()).withTarget(null).getAccessors();

        switch (state) {
            case HANDSHAKE:
                return get.apply(NMS.getHandshakePacketTypesClass());
            case PLAY:
                return get.apply(NMS.getGamePacketTypesClass());
            case STATUS:
                return get.apply(NMS.getStatusPacketTypesClass());
            case LOGIN:
                return get.apply(NMS.getLoginPacketTypesClass());
            case CONFIGURATION:
                return get.apply(NMS.getConfigurationPacketTypesClass());
            default:
                return null;
        }
    }

    public static Object getStreamCodec1_20_5(PacketBound bound, PacketState state) {
        Object protocolInfo = getProtocolInfo1_20_5(bound, state);

        return ReflectStructureCache.acquireStructure(NMS.getEnumProtocolClass())
                .withType(NMS.getStreamCodecClass()).withTarget(protocolInfo)
                .invokeMethod(null, 0);
    }

    public static EmulatorCodec<Object, ByteBuf> createFromStreamCodec1_20_5(Object codec, Supplier<? extends ByteBuf> byteBuf) {

        MethodInvoker<Object> decoderInvoker = ReflectStructureCache.acquireStructure(NMS.getStreamCodecClass())
                .withTarget(codec).getMethod(null, "decode", 0, Object.class);

        MethodInvoker<Object> encoderInvoker = ReflectStructureCache.acquireStructure(NMS.getStreamCodecClass())
                .withTarget(codec).getMethod(null, "encode", 0, Object.class, Object.class);

        return new EmulatorCodec<Object, ByteBuf>() {
            @Override
            public Object decodeModern(ByteBuf buf) throws CodecException {
                return decoderInvoker.invoke(codec, buf);
            }

            @Override
            public Object decodeLegacy(ByteBuf buf, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) throws CodecException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ByteBuf encodeModern(Object val, PacketBuilder<PacketTypeEnum, ByteBuf> function) throws CodecException {
                ByteBuf buf = byteBuf.get();
                encoderInvoker.invoke(codec, buf, val);
                return buf;
            }

            @Override
            public ByteBuf encodeLegacy(Object val, PacketBuilder<PacketTypeEnum, LegacyCodec.Applier> codec) throws CodecException {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static EmulatorCodec.In<Object, PacketDataSerializer> createInboundCodec(PacketState state) {
        return new EmulatorCodec.In<Object, PacketDataSerializer>() {

            private final WrappedEnumProtocol enumProtocol = NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_4)
                    ? WrappedEnumProtocol.getEnumProtocolDelegate(PacketBound.SERVER, state)
                    : null;

            private final EmulatorCodec<Object, ByteBuf> wrappedStreamCodec = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)
                    ? createFromStreamCodec1_20_5(getStreamCodec1_20_5(PacketBound.SERVER, state), Unpooled::buffer)
                    : null;

            @Override
            public Object decodeModern(ByteBuf buf) throws CodecException {
                if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_4)) {
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
                } else {
                    return wrappedStreamCodec.decodeModern(buf);
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
