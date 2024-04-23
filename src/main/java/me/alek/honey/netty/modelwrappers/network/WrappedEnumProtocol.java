package me.alek.honey.netty.modelwrappers.network;

import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.AttributeKey;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import org.w3c.dom.Attr;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {

    public static AttributeKey<Object> getProtocolAttributeKey(Channel channel, PacketBound bound) {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2) && channel.pipeline().get("packet_handler") != null)
            return ReflectStructureCache.acquireStructure(NMS.getNetworkManagerClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get("packet_handler")).readField(0);
        else if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_20_2) && channel.pipeline().get(bound.getProtocolHandlerName()) != null)
            return ReflectStructureCache.acquireStructure(bound == PacketBound.SERVER ? NMS.getPacketDecoderClass() : NMS.getPacketEncoderClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get(bound.getProtocolHandlerName())).readField(0);
        return null;
    }

    public static WrappedEnumProtocol getEnumProtocol(Channel channel, PacketBound bound) {
        AttributeKey<?> protocolAttributeKey = getProtocolAttributeKey(channel, bound);
        if (protocolAttributeKey == null) {
            throw new DecoderException("Couldn't get protocol due to invalid enum protocol from channel attribute.");
        }
        return new WrappedEnumProtocol(channel.attr(protocolAttributeKey).get());
    }

    public static Object getEnumProtocolDelegate(Channel channel, PacketBound bound) {
        Object protocol = getEnumProtocol(channel, bound).getHandle();
        if (protocol.getClass().equals(NMS.getEnumProtocolClass())) {
            return protocol;
        }

        IStructureModifier<Enum<?>> structure = ReflectStructureCache.acquireStructure(protocol.getClass())
                .withType(NMS.getEnumProtocolClass())
                .withTarget(protocol);
        if (!structure.getAccessors().isEmpty()) {
            return structure.readField(0);
        }
        throw new DecoderException("Couldn't get protocol due to invalid enum protocol from channel attribute.");
    }

    public static int getEnumProtocolDelegateOrdinal(Channel channel, PacketBound bound) {
        return ((Enum<?>)getEnumProtocolDelegate(channel, bound)).ordinal();
    }

    private final IStructureModifier<Object> packetModifier = getModifier(NMS.getPacketClass());

    public WrappedEnumProtocol(Object handle) {
        super(handle);
    }

    public Object createPacket(PacketBound direction, int id, WrappedPacketDataSerializer serializer) throws Exception {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            return createLegacyPacket(direction, id);
        }
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
            return createModernPacket(direction, serializer, id);
        }
        return createModernPacket(serializer, id);
    }

    public Object createLegacyPacket(PacketBound direction, int id) throws Exception {
        return invoke(direction.getNMSWrapperType(), id);
    }

    public Object createModernPacket(PacketBound direction, WrappedPacketDataSerializer serializer, int id) throws Exception {
        return invoke(direction.getNMSWrapperType(), id, serializer.toNMSSerializer());
    }

    public Object createModernPacket(WrappedPacketDataSerializer serializer, int id) throws Exception {
        return invoke(id, serializer.toNMSSerializer());
    }

    private Object invoke(Object... params) throws Exception {
        return packetModifier.invokeMethodWithException(0, params);
    }
}