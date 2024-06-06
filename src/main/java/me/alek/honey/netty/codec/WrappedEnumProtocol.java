package me.alek.honey.netty.codec;

import com.google.common.collect.BiMap;
import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.MethodInvoker;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {

    private static final AtomicReference<MethodInvoker<Object>> createPacketCachedInvoker = new AtomicReference<>();
    private static final AtomicReference<MethodInvoker<Object>> getPacketIdCachedInvoker = new AtomicReference<>();

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
            throw new DecoderException("Could not get protocol due to invalid enum protocol from channel attribute.");
        }
        return new WrappedEnumProtocol(channel.attr(protocolAttributeKey).get(), bound);
    }

    public static Object getEnumProtocolDelegate(Channel channel, PacketBound bound) {
        Object protocol = getEnumProtocol(channel, bound).getHandle();

        IStructureModifier<Enum<?>> structure = ReflectStructureCache.acquireStructure(protocol.getClass())
                .withType(NMS.getEnumProtocolClass())
                .withoutStatic()
                .withTarget(protocol);
        if (!structure.getAccessors().isEmpty()) {
            return structure.readField(0);
        }

        return protocol;
    }

    public static Object getEnumProtocol(PacketState state) {
        if (state == PacketState.ANY || state == PacketState.UNKNOWN) {
            return null;
        }

        return NMS.getEnumProtocolClass().getEnumConstants()[state.ordinal()];
    }

    public static WrappedEnumProtocol getEnumProtocolDelegate(PacketBound bound, PacketState state) {
        Object enumProtocol = getEnumProtocol(state);
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)
                || NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            return new WrappedEnumProtocol(enumProtocol, bound);
        }

        return new WrappedEnumProtocol(getProtocolInfo(enumProtocol, bound), bound);
    }

    public static Object getProtocolInfo(Object enumProtocol, PacketBound bound) {
        Map<Object, Object> enumProtocolDelegateFlowMap = ReflectStructureCache.acquireStructure(NMS.getEnumProtocolClass())
                .<Map<Object, Object>>withWeakType(Map.class).withoutStatic().withTarget(enumProtocol).readField(0);
        return enumProtocolDelegateFlowMap.get(bound.getNMSWrapperType());
    }

    private final PacketBound bound;
    private final IStructureModifier<Object> packetModifier = getModifier(NMS.getPacketClass());
    private final MethodInvoker<Integer> getPacketIdModifier;
    private final Object classToIdMap;

    @SuppressWarnings("unchecked")
    public WrappedEnumProtocol(Object handle, PacketBound bound) {
        super(handle);
        this.bound = bound;

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            classToIdMap = null;
            getPacketIdModifier = null;
        }
        else if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_15)) {
            Object protocolRegistry;
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                protocolRegistry = ReflectStructureCache.acquireStructure(handle.getClass())
                        .withType(Reflection.getClass("{nms}.network.EnumProtocol$b")).withTarget(handle)
                        .readField(0);
            } else {
                protocolRegistry = getProtocolInfo(handle, bound);
            }

            classToIdMap = ReflectStructureCache.acquireStructure(protocolRegistry.getClass())
                    .withType(NMS.getObject2IntMapClass()).withTarget(protocolRegistry)
                    .readField(0);

            getPacketIdModifier = ReflectStructureCache.acquireStructure(NMS.getObject2IntMapClass())
                    .asInt().withTarget(classToIdMap).getMethod(getPacketIdCachedInvoker, "getInt", 0, Class.class);
        } else {
            Object protocolMap = getProtocolInfo(handle, bound);
            classToIdMap = ((BiMap<Integer, Class<? extends Object>>)protocolMap).inverse();

            getPacketIdModifier = (MethodInvoker<Integer>) (MethodInvoker<?>) ReflectStructureCache.acquireStructure(Map.class)
                    .withTarget(classToIdMap).getMethod(getPacketIdCachedInvoker, "get", 0, Object.class);
        }
    }

    public PacketBound getBound() {
        return bound;
    }

    public int getPacketId(Class<?> clazz) {
        return getPacketIdModifier.invoke(classToIdMap, clazz);
    }

    public Object createPacket(PacketBound direction, int id, PacketDataSerializer serializer) throws Exception {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            return createLegacyPacket(direction, id);
        }
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
            return createModernPacket1_20_1(direction, serializer, id);
        }
        return createModernPacket1_20_2(serializer, id);
    }

    public Object createLegacyPacket(PacketBound direction, int id) throws Exception {
        return invoke(direction.getNMSWrapperType(), id);
    }

    public Object createModernPacket1_20_1(PacketBound direction, PacketDataSerializer serializer, int id) throws Exception {
        return invoke(direction.getNMSWrapperType(), id, serializer.getSerializer());
    }

    public Object createModernPacket1_20_2(PacketDataSerializer serializer, int id) throws Exception {
        return invoke(id, serializer.getSerializer());
    }

    private Object invoke(Object... params) throws Exception {
        return packetModifier.invokeMethodWithException(createPacketCachedInvoker, 0, params);
    }
}