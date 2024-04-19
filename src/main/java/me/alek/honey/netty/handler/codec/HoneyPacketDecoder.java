package me.alek.honey.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
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

public class HoneyPacketDecoder {

    private static final Class<?> packetDataSerializerClass = NMS.getPacketDataSerializerClass();
    private static final EnumMap<PacketState, Array<MethodHandle>> legacySerializerApplicationLookup = new EnumMap<>(PacketState.class);

    static {
        for (PacketState state : PacketState.values()) {
            legacySerializerApplicationLookup.put(state, new Array<>());
        }
    }

    public static Object decodePacket(HoneyPlayer player, PacketState state, WrappedEnumProtocol protocol, ByteBuf byteBuf) throws DecoderException {
        WrappedPacketDataSerializer dataSerializer = new WrappedPacketDataSerializer(byteBuf);
        int start = dataSerializer.readerIndex();

        int packetId = dataSerializer.readVarInt();
        if (packetId < 0) {
            throw new DecoderException("Bad packet ID " + packetId);
        }
        Object packet;
        try {
            packet = protocol.createPacket(PacketBound.SERVER, packetId, dataSerializer);
            if (packet == null) {
                throw new DecoderException("Bad packet ID " + packetId);
            }
        } catch (Exception ex) {
            throw new DecoderException("Failed to create packet with ID " + packetId);
        }

        if (PacketType.getPacketType(packet.getClass()).getState() != state) {
            return null;
        }

        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            // we will now check if the packet has an ItemStack property, because then we will disable NBT
            if (packet.getClass().equals(PacketType.Play.Client.BLOCK_PLACE.getNmsClass())
                    && NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                DisabledNBTCodec.BLOCK_PLACE.apply(
                        ReflectStructureCache.acquireStructure(packet.getClass()).disableStaticFields().withTarget(packet), dataSerializer);
            }
            else if (packet.getClass().equals(PacketType.Play.Client.WINDOW_CLICK.getNmsClass())) {
                DisabledNBTCodec.WINDOW_CLICK.apply(
                        ReflectStructureCache.acquireStructure(packet.getClass()).disableStaticFields().withTarget(packet), dataSerializer);
            }
            else if (packet.getClass().equals(PacketType.Play.Client.SET_CREATIVE_SLOT.getNmsClass())
                    && player != null && !player.player.getGameMode().equals(GameMode.CREATIVE)) {
                return null;
            }
            else {
                // otherwise apply the default implementation for the serializer
                MethodHandle applicationMethod = legacySerializerApplicationLookup.get(state).get(packetId, () -> {
                    MethodInvoker<?> invoker = Reflection.getMethod(packet.getClass(), 1, packetDataSerializerClass);
                    try {
                        return MethodHandles.lookup().unreflect(invoker.getMethod());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        Bukkit.getLogger().severe("Failed to create legacy serializer application lookup for packet " + packetId + ", " + packet + ", " + state);
                        return null;
                    }
                });
                try {
                    applicationMethod.invoke(packet, dataSerializer.getHandle());
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    Bukkit.getLogger().info(legacySerializerApplicationLookup.get(state) + "");
                    throw new DecoderException("Failed to apply serializer for packet " + packetId + ", " + packet + ", " + state);
                }
            }

            if (dataSerializer.isReadable()) {
                throw new DecoderException("Found " + dataSerializer.readableBytes() + "/" + (dataSerializer.readerIndex() + dataSerializer.readableBytes() - start) + " extra bytes while decoding packet " + packetId + ", " + packet + ", " + state);
            }
        }

        return packet;
    }
}
