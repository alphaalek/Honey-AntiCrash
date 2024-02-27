package me.alek.honey.netty.modelwrappers.network;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.Bukkit;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {

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
        Bukkit.broadcastMessage(invoke(id, serializer.toNMSSerializer()) + "");
        return invoke(id, serializer.toNMSSerializer());
    }

    private Object invoke(Object... params) throws Exception {
        return getModifier(NMSUtils.getPacketClass()).invokeMethodWithException(0, params);
    }
}