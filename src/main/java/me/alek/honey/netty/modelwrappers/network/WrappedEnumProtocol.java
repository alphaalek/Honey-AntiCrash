package me.alek.honey.netty.modelwrappers.network;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.utility.NMSUtils;
import org.bukkit.Bukkit;

public class WrappedEnumProtocol extends WrappedObject<WrappedEnumProtocol> {

    public WrappedEnumProtocol(Object handle) {
        super(handle);
    }

    public Object createLegacyPacket(PacketBound direction, int id) {
        try {
            return getModifier(NMSUtils.getPacketClass()).invokeMethodWithException(0, direction.getNMSWrapperType(), id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Object createModernPacket(WrappedPacketDataSerializer serializer, int id) {
        try {
            return getModifier(NMSUtils.getPacketClass()).invokeMethodWithException(0, id, serializer.getHandle());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String getProtocol() {
        return getModifier(String.class).readField(0);
    }
}