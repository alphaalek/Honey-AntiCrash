package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.handler.HoneyPacketDecodePreparation;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CodecFactory {

    public static Object getProtocolInfo1_20_5(PacketBound bound, PacketState state) {
        Class<Object> protocolInfoClass = NMS.getProtocolInfoClass();
        Class<Object> protocolInfoBinderClass = NMS.getProtocolInfoBinderClass();

        BiFunction<Class<Object>, Class<Object>, IStructureModifier<Object>> get = (c, t) -> ReflectStructureCache.acquireStructure(c)
                .withType(t).withTarget(null);

        int index = bound.equals(PacketBound.CLIENT) ? 1 : 0;

        switch (state) {
            case HANDSHAKE:
                if (bound == PacketBound.CLIENT) {
                    throw new NoSuchElementException();
                }
                return get.apply(Protocols.HANDSHAKE_CLASS, protocolInfoClass).readField(0);
            case PLAY:
                return bindProtocolInfo1_20_5(
                        get.apply(Protocols.GAME_CLASS, protocolInfoBinderClass).readField(index));
            case STATUS:
                return get.apply(Protocols.STATUS_CLASS, protocolInfoClass).readField(index);
            case LOGIN:
                return get.apply(Protocols.LOGIN_CLASS, protocolInfoClass).readField(index);
            case CONFIGURATION:
                return get.apply(Protocols.CONFIGURATION_CLASS, protocolInfoClass).readField(index);
            default:
                return null;
        }
    }

    public static Object bindProtocolInfo1_20_5(Object protocolInfoBinder) {
        return ReflectStructureCache.acquireStructure(NMS.getProtocolInfoBinderClass())
                .withType(NMS.getProtocolInfoClass()).withTarget(protocolInfoBinder)
                .invokeMethod(null, "bind", 0, getRegistryCustomFunction1_20_5());
    }

    public static Function<ByteBuf, Object> getRegistryCustomFunction1_20_5() {
        Object layeredRegistryAccess = ReflectStructureCache.acquireStructure(NMS.getMinecraftServerClass())
                .withType(NMS.getLayeredRegistryAccessClass()).withTarget(NMS.getMinecraftServer())
                .readField(0);

        Object registryCustomDimension = ReflectStructureCache.acquireStructure(NMS.getLayeredRegistryAccessClass())
                .withType(NMS.getRegistryCustomDimensionClass()).withTarget(layeredRegistryAccess)
                .readField(0);

        return ReflectStructureCache.acquireStructure(NMS.getRegistryFriendlyByteBufClass())
                .<Function<ByteBuf, Object>>withWeakType(Function.class).withTarget(null)
                .invokeMethod(null, 0, registryCustomDimension);
    }

    public static Collection<FieldAccessor<Object>> getPacketTypes1_20_5(PacketState state) {
        Function<Class<Object>, Collection<FieldAccessor<Object>>> get = c -> ReflectStructureCache.acquireStructure(c)
                .withType(NMS.getPacketTypeClass()).withTarget(null).getAccessors();

        switch (state) {
            case HANDSHAKE:
                return get.apply(Protocols.HANDSHAKE_TYPES_CLASS);
            case PLAY:
                return get.apply(Protocols.GAME_TYPES_CLASS);
            case STATUS:
                return get.apply(Protocols.STATUS_TYPES_CLASS);
            case LOGIN:
                return get.apply(Protocols.LOGIN_TYPES_CLASS);
            case CONFIGURATION:
                return get.apply(Protocols.CONFIGURATION_TYPES_CLASS);
            default:
                return null;
        }
    }

    public static Object getStreamCodec1_20_5(PacketBound bound, PacketState state) {
        Object protocolInfo = getProtocolInfo1_20_5(bound, state);

        return ReflectStructureCache.acquireStructure(NMS.getProtocolInfoClass())
                .withType(NMS.getStreamCodecClass()).withTarget(protocolInfo)
                .invokeMethod(null, 0);
    }

    public static EmulatorCodec<Object, ByteBuf> createFromStreamCodec1_20_5(Object codec, Supplier<? extends ByteBuf> byteBuf) {
        return new EmulatorCodec<Object, ByteBuf>() {

            private final MethodInvoker<Object> decoderInvoker = ReflectStructureCache.acquireStructure(NMS.getStreamDecoderClass())
                    .withTarget(codec).getMethod(null, "decode", 0, Object.class);

            private final MethodInvoker<Object> encoderInvoker = ReflectStructureCache.acquireStructure(NMS.getStreamEncoderClass())
                    .withTarget(codec).getMethod(null, "encode", 0, Object.class, Object.class);

            @Override
            public Object decodeModern(ByteBuf buf, PacketState state) throws CodecException {
                return decoderInvoker.invoke(codec, buf);
            }

            @Override
            public Object decodeLegacy(ByteBuf buf, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) throws CodecException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ByteBuf encodeModern(Object val, PacketState state, PacketBuilder<PacketType, ByteBuf> function) throws CodecException {
                ByteBuf buf = byteBuf.get();
                encoderInvoker.invoke(codec, buf, val);
                return buf;
            }

            @Override
            public ByteBuf encodeLegacy(Object val, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) throws CodecException {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static EmulatorCodec.In<Object, PacketDataSerializer> createInboundCodec(PacketState state) {
        return new EmulatorCodec.In<Object, PacketDataSerializer>() {

            private final WrappedEnumProtocol enumProtocol = NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_4)
                    ? WrappedEnumProtocol.getEnumProtocolDelegate(PacketBound.SERVER, state)
                    : null;

            private final EmulatorCodec<Object, ByteBuf> wrappedStreamCodec = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_6)
                    ? createFromStreamCodec1_20_5(getStreamCodec1_20_5(PacketBound.SERVER, state), Unpooled::buffer)
                    : null;

            @Override
            public Object decodeModern(ByteBuf buf, PacketState state) throws CodecException {
                if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_4)) {
                    PacketDataSerializer dataSerializer = new PacketDataSerializer(buf);
                    int id = dataSerializer.readVarInt();
                    if (id < 0) {
                        throw new CodecException(id, "Bad Packet ID");
                    }
                    if (id == HoneyPacketDecodePreparation.VIA_VERSION_PASSTHROUGH_ID) {
                        return null;
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
                    return wrappedStreamCodec.decodeModern(buf, state);
                }
            }

            @Override
            public Object decodeLegacy(ByteBuf buf, PacketState state, PacketBuilder<PacketType, LegacyCodec.Applier> codec) throws CodecException {
                PacketDataSerializer dataSerializer = new PacketDataSerializer(buf);
                int id = dataSerializer.readVarInt();
                if (id < 0) {
                    throw new CodecException(id, "Bad Packet ID");
                }
                if (id == HoneyPacketDecodePreparation.VIA_VERSION_PASSTHROUGH_ID) {
                    return null;
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

                Optional<PacketType> type = PacketTypes.getPacketType(packet.getClass(), state);
                if (!type.isPresent()) {
                    throw new CodecException(id, "Wrong protocol state");
                }

                codec.accept(type.get(), new LegacyCodec.Applier() {
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
