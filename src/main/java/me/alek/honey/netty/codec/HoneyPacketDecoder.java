package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.type.*;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.MethodInvoker;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.util.Array;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.function.BiConsumer;

public class HoneyPacketDecoder {

    public static Object decodePacket(HoneyPlayer player, PacketState state, EmulatorCodec<Object, PacketDataSerializer> protocol, ByteBuf byteBuf) throws DecoderException {
        if (protocol == null) {
            return null;
        }

        Object packet;
        try {
            if (EmulatorCodec.LEGACY) {
                packet = protocol.decodeLegacy(byteBuf, (packetType, applier) -> {
                    if (packetType.equals(PacketType.Play.Client.BLOCK_PLACE)
                            && ConfigurationOptions.Decoder.DISABLE_NBT
                            && NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                        applier.apply(DisabledNBTCodec.BLOCK_PLACE);
                    }
                    else if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)
                            && ConfigurationOptions.Decoder.DISABLE_NBT) {
                        applier.apply(DisabledNBTCodec.WINDOW_CLICK);
                    }
                    else if (packetType.equals(PacketType.Play.Client.SET_CREATIVE_SLOT)
                            && ConfigurationOptions.Decoder.DISABLE_CREATIVE_SLOT_FOR_NON_CREATIVE
                            && player != null && !player.player.getGameMode().equals(GameMode.CREATIVE)) {
                        // don't apply anything
                    } else {
                        applier.apply(LegacyCodec.createDefault(packetType));
                    }
                });
            } else {
                packet = protocol.decodeModern(byteBuf);
            }

            if (byteBuf.isReadable()) {
                int end = byteBuf.readerIndex();
                int remaining = byteBuf.readableBytes();
                throw new DecoderException("Found " + remaining + " (" + (end + byteBuf.readableBytes()) + "/" + end + ") extra bytes while decoding packet " + packet + ", " + state + " " + ReflectStructureCache.acquireStructure(packet.getClass()).withoutStatic().withTarget(packet).readFieldEntries());
            }

        } catch (CodecException ex) {
            throw new DecoderException("Failed to create packet with ID " + ex.getPacketID() + ", " + state);
        }

        PacketTypeEnum type = PacketType.getPacketType(packet.getClass());
        if (!PacketType.isUnknown(type) && !PacketType.getCommonStates(type).contains(state)) {
            return null;
        }

        return packet;
    }
}
