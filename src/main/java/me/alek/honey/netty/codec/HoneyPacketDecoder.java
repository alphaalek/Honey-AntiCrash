package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.GameMode;

public class HoneyPacketDecoder {

    public static Object decodePacket(HoneyPlayer player, PacketState state, EmulatorCodec<Object, PacketDataSerializer> protocol, ByteBuf byteBuf) throws DecoderException {
        if (protocol == null) {
            return null;
        }

        Object packet;
        try {
            if (EmulatorCodec.LEGACY) {
                packet = protocol.decodeLegacy(byteBuf, state, (type, callback) -> {
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
                        callback.apply(LegacyCodec.createDefault(type));
                    }
                });
            } else {
                packet = protocol.decodeModern(byteBuf, state);
            }

            if (packet == null) {
                // failed to decode packet object for this byte buffer
                return null;
            }

            if (byteBuf.isReadable()) {
                int end = byteBuf.readerIndex();
                int remaining = byteBuf.readableBytes();
                throw new DecoderException(
                        "Found " + remaining
                                + " (" + (end + byteBuf.readableBytes()) + "/" + end + ") extra bytes while decoding packet "
                                + HoneyPlugin.get().getLogger().formatPacketType(PacketTypes.getPacketType(packet.getClass(), state).orElse(PacketType.UNKNOWN)) + ", "
                                + state + " (" + NetProtocol.getProtocol().getName() + "): "
                                + HoneyPlugin.get().getLogger().filterTooMuchData(
                                HoneyPlugin.get().getLogger().formatPacketEntries(
                                        ReflectStructure.Provider.acquireStructure(packet.getClass())
                                                .withStatic(false)
                                                .withTarget(packet)
                                                .readFieldEntries()
                                )
                        )
                );
            }
        } catch (CodecException ex) {
            throw new DecoderException("Failed to create packet with ID " + ex.getPacketID() + ", " + state, ex);
        }

        if (!PacketTypes.getPacketType(packet.getClass(), state).isPresent()) {
            return null;
        }

        return packet;
    }
}
