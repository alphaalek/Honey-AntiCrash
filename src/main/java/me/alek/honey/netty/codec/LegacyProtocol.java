package me.alek.honey.netty.codec;

import com.google.common.collect.BiMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;
import me.alek.honey.netty.handler.MinecraftHandlers;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class LegacyProtocol extends WrappedObject<LegacyProtocol> {

    private static final AtomicReference<MethodInvoker<Object>> CREATE_PACKET_INVOKER_CACHE = new AtomicReference<>();
    private static final AtomicReference<MethodInvoker<Object>> GET_PACKET_ID_INVOKER_CACHE = new AtomicReference<>();

    public static AttributeKey<Object> getProtocolAttributeKey(Channel channel, PacketBound bound) {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
            ChannelHandler connection = channel.pipeline().get(MinecraftHandlers.CONNECTION);
            return ReflectStructure.Provider.acquireStructure(NMS.getNetworkManagerClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class)
                    .withTarget(connection).readField(0);
        } else if (NetProtocol.getProtocol().isBetween(NetProtocol.v1_20_2, NetProtocol.v1_20_4)) {
            ChannelHandler codecHandler = channel.pipeline().get(bound.getProtocolHandlerName());
            return ReflectStructure.Provider.acquireStructure(codecHandler.getClass())
                    .<AttributeKey<Object>>withWeakType(AttributeKey.class)
                    .withTarget(codecHandler).readField(0);
        } else {
            throw new UnsupportedOperationException("Protocol attribute key does not exist in modern versions");
        }
    }

    public static Object getEnumProtocol(PacketState state) {
        if (PacketState.isValid(state)) {
            return NMS.getEnumProtocolClass().getEnumConstants()[state.ordinal()];
        } else {
            throw new IllegalArgumentException("Invalid packet state");
        }
    }

    public static LegacyProtocol getEnumProtocolDelegate(PacketBound bound, PacketState state) {
        Object enumProtocol = getEnumProtocol(state);
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            throw new IllegalStateException("Can not get enum protocol in this version");
        } else if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
            return new LegacyProtocol(getProtocolInfo(enumProtocol, bound), bound);
        } else {
            return new LegacyProtocol(enumProtocol, bound);
        }
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
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_6)) {
            throw new UnsupportedOperationException("Can't use legacy protocol in modern version");
        }

        this.bound = bound;
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_15)) {
            Object protocolRegistry;
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                protocolRegistry = ReflectStructure.Provider.acquireStructure(handle.getClass())
                        .withType(Reflection.getClass("{nms}.network.EnumProtocol$b")).withTarget(handle)
                        .readField(0);
            } else {
                protocolRegistry = getProtocolInfo(handle, bound);
            }

            this.classToIdMap = ReflectStructure.Provider.acquireStructure(protocolRegistry.getClass())
                    .withType(NMS.getObject2IntMapClass()).withTarget(protocolRegistry)
                    .readField(0);

            this.getPacketIdModifier = ReflectStructure.Provider.acquireStructure(NMS.getObject2IntMapClass())
                    .withInts().withTarget(this.classToIdMap).getMethod(GET_PACKET_ID_INVOKER_CACHE, "getInt", 0, Object.class);
        } else {
            Object protocolMap = getProtocolInfo(handle, bound);
            this.classToIdMap = ((BiMap<Integer, Class<? extends Object>>) protocolMap).inverse();

            this.getPacketIdModifier = (MethodInvoker<Integer>) (MethodInvoker<?>) ReflectStructure.Provider.acquireStructure(Map.class)
                    .withTarget(this.classToIdMap).getMethod(GET_PACKET_ID_INVOKER_CACHE, "get", 0, Object.class);
        }
    }

    public PacketBound getBound() {
        return this.bound;
    }

    public int getPacketId(Class<?> clazz) {
        return this.getPacketIdModifier.invoke(this.classToIdMap, clazz);
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
        return this.packetModifier.invokeMethodWithException(CREATE_PACKET_INVOKER_CACHE, 0, params);
    }
}