package me.alek.honey.netty.codec;

import com.google.common.collect.BiMap;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class LegacyProtocol extends WrappedObject<LegacyProtocol> {

    private static final AtomicReference<MethodInvoker<Object>> createPacketCachedInvoker = new AtomicReference<>();
    private static final AtomicReference<MethodInvoker<Object>> getPacketIdCachedInvoker = new AtomicReference<>();

    public static AttributeKey<Object> getProtocolAttributeKey(Channel channel, PacketBound bound) {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2) && channel.pipeline().get("packet_handler") != null)
            return ReflectStructure.Provider.acquireStructure(NMS.getNetworkManagerClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get("packet_handler")).readField(0);
        else if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_20_2) && channel.pipeline().get(bound.getProtocolHandlerName()) != null)
            return ReflectStructure.Provider.acquireStructure(bound == PacketBound.SERVER ? NMS.getPacketDecoderClass() : NMS.getPacketEncoderClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class).withTarget(channel.pipeline().get(bound.getProtocolHandlerName())).readField(0);
        return null;
    }

    public static Object getEnumProtocol(PacketState state) {
        if (state == PacketState.ANY || state == PacketState.UNKNOWN) {
            return null;
        }

        return NMS.getEnumProtocolClass().getEnumConstants()[state.ordinal()];
    }

    public static LegacyProtocol getEnumProtocolDelegate(PacketBound bound, PacketState state) {
        Object enumProtocol = getEnumProtocol(state);
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)
                || NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_6)) {
            return new LegacyProtocol(enumProtocol, bound);
        }

        return new LegacyProtocol(getProtocolInfo(enumProtocol, bound), bound);
    }

    public static Object getProtocolInfo(Object enumProtocol, PacketBound bound) {
        Map<Object, Object> enumProtocolDelegateFlowMap = ReflectStructure.Provider.acquireStructure(NMS.getEnumProtocolClass())
                .<Map<Object, Object>>withWeakType(Map.class).withStatic(false).withTarget(enumProtocol).readField(0);
        return enumProtocolDelegateFlowMap.get(bound.getNMSType());
    }

    private final PacketBound bound;
    private final IStructureModifier<Object> packetModifier = getModifier(NMS.getPacketClass());
    private final MethodInvoker<Integer> getPacketIdModifier;
    private final Object classToIdMap;

    @SuppressWarnings("unchecked")
    public LegacyProtocol(Object handle, PacketBound bound) {
        super(handle);
        this.bound = bound;

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_6)) {
            classToIdMap = null;
            getPacketIdModifier = null;
        } else if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_15)) {
            Object protocolRegistry;
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                protocolRegistry = ReflectStructure.Provider.acquireStructure(handle.getClass())
                        .withType(Reflection.getClass("{nms}.network.EnumProtocol$b")).withTarget(handle)
                        .readField(0);
            } else {
                protocolRegistry = getProtocolInfo(handle, bound);
            }

            classToIdMap = ReflectStructure.Provider.acquireStructure(protocolRegistry.getClass())
                    .withType(NMS.getObject2IntMapClass()).withTarget(protocolRegistry)
                    .readField(0);

            getPacketIdModifier = ReflectStructure.Provider.acquireStructure(NMS.getObject2IntMapClass())
                    .withInts().withTarget(classToIdMap).getMethod(getPacketIdCachedInvoker, "getInt", 0, Object.class);
        } else {
            Object protocolMap = getProtocolInfo(handle, bound);
            classToIdMap = ((BiMap<Integer, Class<? extends Object>>)protocolMap).inverse();

            getPacketIdModifier = (MethodInvoker<Integer>) (MethodInvoker<?>) ReflectStructure.Provider.acquireStructure(Map.class)
                    .withTarget(classToIdMap).getMethod(getPacketIdCachedInvoker, "get", 0, Object.class);
        }
    }

    public PacketBound getBound() {
        return bound;
    }

    public int getPacketId(Class<?> clazz) {
        return getPacketIdModifier.invoke(classToIdMap, clazz);
    }

    public Object createLegacyPacket(PacketBound direction, int id) throws Exception {
        return invokePacketFactory(direction.getNMSType(), id);
    }

    public Object createModernPacket1_20_1(PacketBound direction, PacketDataSerializer serializer, int id) throws Exception {
        return invokePacketFactory(direction.getNMSType(), id, serializer.getSerializer());
    }

    public Object createModernPacket1_20_2(PacketDataSerializer serializer, int id) throws Exception {
        return invokePacketFactory(id, serializer.getSerializer());
    }

    private Object invokePacketFactory(Object... params) throws Exception {
        return packetModifier.invokeMethodWithException(createPacketCachedInvoker, 0, params);
    }
}