package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
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

    private static final Supplier<PacketDataSerializer> OUTBOUND_SERIALIZER_CONST = PacketDataSerializer::new;

    public static PacketDataSerializer encodePacket(HoneyPlayer player, Object packet) throws CodecException {
        EmulatorCodec<Object, PacketDataSerializer> protocol = player.packetHandler.getProtocolCodec(PacketBound.CLIENT);
        if (protocol == null) {
            throw new IllegalStateException("No outbound protocol");
        }

        PacketState state = player.packetHandler.getProtocolState(PacketBound.CLIENT);
        try {
            if (EmulatorCodec.LEGACY) {
                return protocol.encodeLegacy(packet, state, (type, callback) -> callback.apply(LegacyCodec.createDefault(type, PacketBound.CLIENT)));
            } else {
                return protocol.encodeModern(OUTBOUND_SERIALIZER_CONST.get(), packet, state);
            }
        } catch (CodecException ex) {
            throw new CodecException("Failed to serialize packet " + PacketTypes.getPacketType(packet.getClass(), state).orElse(PacketType.UNKNOWN) + ", state " + state, ex);
        }
    }

    public static Object decodePacket(HoneyPlayer player, ByteBuf buf) throws DecoderException {
        EmulatorCodec<Object, PacketDataSerializer> protocol = player.packetHandler.getProtocolCodec(PacketBound.SERVER);
        if (protocol == null) {
            throw new IllegalStateException("No inbound protocol");
        }

        PacketState state = player.packetHandler.getProtocolState(PacketBound.SERVER);
        Object packet;
        try {
            if (EmulatorCodec.LEGACY) {
                packet = protocol.decodeLegacy(buf, state, (type, callback) -> {
                    if (type.equals(PacketTypes.Play.Client.BLOCK_PLACE)
                            && ConfigurationOptions.Decoder.DISABLE_NBT
                            && NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                        callback.apply(DisabledNBTCodec.BLOCK_PLACE);
                    } else if (type.equals(PacketTypes.Play.Client.CLICK_WINDOW)
                            && ConfigurationOptions.Decoder.DISABLE_NBT) {
                        callback.apply(DisabledNBTCodec.WINDOW_CLICK);
                    } else if (type.equals(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION)
                            && ConfigurationOptions.Decoder.DISABLE_CREATIVE_SLOT_FOR_NON_CREATIVE
                            && player.isBukkitReady() && !player.bukkit.getGameMode().equals(GameMode.CREATIVE)) {
                        // don't apply anything
                    } else {
                        callback.apply(LegacyCodec.createDefault(type, PacketBound.SERVER));
                    }
                });
            } else {
                packet = protocol.decodeModern(buf, state);
            }

            if (packet == null) {
                // failed to decode packet object for this byte buffer
                return null;
            }

            checkReadable(NetProtocol.getProtocol(), PacketTypes.getPacketType(packet.getClass(), state).orElse(PacketType.UNKNOWN), packet, buf);
        } catch (CodecException ex) {
            buf.resetReaderIndex();
            throw new DecoderException("Failed to create packet with ID " + new PacketDataSerializer(buf).readVarInt() + ", state " + state, ex);
        }

        if (!PacketTypes.getPacketType(packet.getClass(), state).isPresent()) {
            return null;
        }

        return packet;
    }

    public static void checkReadable(NetProtocol protocol, PacketType type, Object packetObject, ByteBuf buf) throws DecoderException {
        if (buf.isReadable()) {
            int end = buf.readerIndex();
            int remaining = buf.readableBytes();
            throw new DecoderException(
                    "Found " + remaining
                            + " (" + (end + buf.readableBytes()) + "/" + end + ") extra bytes while decoding packet "
                            + HoneyPlugin.get().getLogger().formatPacketType(type) + ", "
                            + " (" + protocol.getName() + ")"
                            +
                            (packetObject == null ? "" : ": " +
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
