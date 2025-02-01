package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CodecFactory {

    public static final EmulatorCodec<Object, PacketDataSerializer> NO_OP_CODEC = new NoOpCodec();

    public static final int VIA_VERSION_PASSTHROUGH_ID = 1000;

    public static Object getProtocolInfo1_20_5(PacketBound bound, PacketState state) {
        Class<Object> protocolInfoClass = NMS.getProtocolInfoClass();
        Class<Object> protocolInfoBinderClass = NMS.getProtocolInfoBinderClass();

        BiFunction<Class<Object>, Class<Object>, IStructureModifier<Object>> get = (c, t) -> ReflectStructure.Provider.acquireStructure(c)
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
        return ReflectStructure.Provider.acquireStructure(NMS.getProtocolInfoBinderClass())
                .withType(NMS.getProtocolInfoClass()).withTarget(protocolInfoBinder)
                .invokeMethod(null, "bind", 0, getRegistryCustomFunction1_20_5());
    }

    public static Function<ByteBuf, Object> getRegistryCustomFunction1_20_5() {
        Object layeredRegistryAccess = ReflectStructure.Provider.acquireStructure(NMS.getMinecraftServerClass())
                .withType(NMS.getLayeredRegistryAccessClass()).withTarget(NMS.getMinecraftServer())
                .readField(0);

        Object registryCustomDimension = ReflectStructure.Provider.acquireStructure(NMS.getLayeredRegistryAccessClass())
                .withType(NMS.getRegistryCustomDimensionClass()).withTarget(layeredRegistryAccess)
                .readField(0);

        return ReflectStructure.Provider.acquireStructure(NMS.getRegistryFriendlyByteBufClass())
                .<Function<ByteBuf, Object>>withWeakType(Function.class).withTarget(null)
                .invokeMethod(null, 0, registryCustomDimension);
    }

    public static Collection<FieldAccessor<Object>> getPacketTypes1_20_5(PacketState state) {
        Function<Class<Object>, Collection<FieldAccessor<Object>>> get = c -> ReflectStructure.Provider.acquireStructure(c)
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

        return ReflectStructure.Provider.acquireStructure(NMS.getProtocolInfoClass())
                .withType(NMS.getStreamCodecClass()).withTarget(protocolInfo)
                .invokeMethod(null, 0);
    }

    public static EmulatorCodec<Object, ByteBuf> createFromStreamCodec1_20_5(Object codec) {
        return new EmulatorCodec<Object, ByteBuf>() {

            private final MethodInvoker<Object> decoderInvoker = ReflectStructure.Provider.acquireStructure(NMS.getStreamDecoderClass())
                    .withTarget(codec).getMethod(null, "decode", 0, Object.class);

            private final MethodInvoker<Object> encoderInvoker = ReflectStructure.Provider.acquireStructure(NMS.getStreamEncoderClass())
                    .withTarget(codec).getMethod(null, "encode", 0, Object.class, Object.class);

            @Override
            public Object decodeModern(ByteBuf buf, PacketState state) throws CodecException {
                return this.decoderInvoker.invoke(codec, buf);
            }

            @Override
            public Object decodeLegacy(ByteBuf buf, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ByteBuf encodeModern(ByteBuf buf, Object val, PacketState state) throws EncoderException {
                this.encoderInvoker.invoke(codec, buf, val);
                return buf;
            }

            @Override
            public ByteBuf encodeLegacy(Object val, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws EncoderException {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static EmulatorCodec.In<Object, PacketDataSerializer> createInboundCodec(PacketState state) {
        return new EmulatorCodec.In<Object, PacketDataSerializer>() {

            private final LegacyProtocol enumProtocol = NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_4)
                    ? LegacyProtocol.getEnumProtocolDelegate(PacketBound.SERVER, state)
                    : null;

            private final EmulatorCodec<Object, ByteBuf> wrappedStreamCodec = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)
                    ? createFromStreamCodec1_20_5(getStreamCodec1_20_5(PacketBound.SERVER, state))
                    : null;

            @Override
            public Object decodeModern(ByteBuf buf, PacketState state) throws CodecException {
                int index = buf.readerIndex();

                PacketDataSerializer dataSerializer = new PacketDataSerializer(buf);
                int id = dataSerializer.readVarInt();

                if (id < 0) {
                    throw new CodecException("Bad Packet ID");
                }
                if (id == VIA_VERSION_PASSTHROUGH_ID) {
                    return decodeModern(buf.skipBytes(16).readBytes(buf.readableBytes()), state);
                }

                Object packet;
                if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_4)) {
                    try {
                        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
                            packet = this.enumProtocol.createModernPacket1_20_1(PacketBound.SERVER, dataSerializer, id);
                        } else {
                            packet = this.enumProtocol.createModernPacket1_20_2(dataSerializer, id);
                        }
                    } catch (Exception ex) {
                        throw new CodecException(ex);
                    }
                } else {
                    buf.readerIndex(index);
                    packet = this.wrappedStreamCodec.decodeModern(buf, state);
                }

                if (packet == null) {
                    throw new CodecException("Bad Packet ID");
                }

                Optional<PacketType> typeOptional = PacketTypes.getPacketType(packet.getClass(), state);
                if (!typeOptional.isPresent()) {
                    throw new CodecException("Wrong protocol state");
                }

                return packet;
            }

            @Override
            public Object decodeLegacy(ByteBuf buf, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException {
                PacketDataSerializer dataSerializer = new PacketDataSerializer(buf);
                int id = dataSerializer.readVarInt();
                if (id < 0) {
                    throw new CodecException("Bad Packet ID");
                }
                if (id == VIA_VERSION_PASSTHROUGH_ID) {
                    // TODO: call ViaVersion
                    return decodeLegacy(buf.skipBytes(16).readBytes(buf.readableBytes()), state, codec);
                }

                Object packet;
                try {
                    packet = this.enumProtocol.createLegacyPacket(PacketBound.SERVER, id);
                } catch (Exception ex) {
                    throw new CodecException(ex);
                }
                if (packet == null) {
                    throw new CodecException( "Bad Packet ID");
                }

                Optional<PacketType> typeOptional = PacketTypes.getPacketType(packet.getClass(), state);
                if (!typeOptional.isPresent()) {
                    throw new CodecException("Wrong protocol state");
                }

                PacketType type = typeOptional.get();
                codec.accept(type, new LegacyCodec.Callback() {
                    @Override
                    public void apply(LegacyCodec codec) throws CodecException {
                        codec.apply(packet,
                                LegacyCodec.getStructureModifier(type, packet),
                                dataSerializer);
                    }
                });

                return packet;
            }
        };
    }

    public static EmulatorCodec.Out<Object, PacketDataSerializer> createOutboundCodec(PacketState state) {
        return new EmulatorCodec.Out<Object, PacketDataSerializer>() {

            private final EmulatorCodec<Object, ByteBuf> wrappedStreamCodec = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)
                    ? createFromStreamCodec1_20_5(getStreamCodec1_20_5(PacketBound.CLIENT, state))
                    : null;

            private PacketType getType(Object val) {
                Optional<PacketType> typeOptional = PacketTypes.getPacketType(val.getClass(), state);
                if (!typeOptional.isPresent()) {
                    throw new EncoderException("Trying to encode a non-packet object: " + val.getClass());
                }
                return typeOptional.get();
            }

            @Override
            public PacketDataSerializer encodeModern(PacketDataSerializer serializer, Object val, PacketState state) throws CodecException {
                PacketType type = getType(val);
                if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                    return new PacketDataSerializer(this.wrappedStreamCodec.encodeModern(serializer, val, state));
                } else {
                    serializer.writeVarInt(type.getMinecraftID().get());
                    LegacyCodec.createDefault(type, PacketBound.CLIENT)
                            .apply(val, LegacyCodec.getStructureModifier(type, val), serializer);
                    return serializer;
                }
            }

            @Override
            public PacketDataSerializer encodeLegacy(Object val, PacketState state, PacketCodecInitializer<PacketType, LegacyCodec.Callback> codec) throws CodecException {
                PacketType type = getType(val);
                PacketDataSerializer serializer = new PacketDataSerializer();
                serializer.writeVarInt(type.getMinecraftID().get());

                codec.accept(type, new LegacyCodec.Callback() {
                    @Override
                    public void apply(LegacyCodec codec) throws CodecException {
                        codec.apply(val, LegacyCodec.getStructureModifier(type, val), serializer);
                    }
                });

                return serializer;
            }
        };
    }
}
