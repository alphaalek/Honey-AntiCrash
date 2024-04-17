package me.alek.honey.netty.modelwrappers.network;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {

    public static AttributeKey<?> getProtocolAttributeKey(Channel channel, PacketBound bound) {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2))
            return ReflectStructureCache.acquireStructure(NMS.getNetworkManagerClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get("packet_handler")).readField(0);
        else
            return ReflectStructureCache.acquireStructure(bound == PacketBound.SERVER ? NMS.getPacketDecoderClass() : NMS.getPacketEncoderClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get(bound.getProtocolHandlerName())).readField(0);
    }

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
        return getModifier(NMS.getPacketClass()).invokeMethodWithException(0, params);
    }
}