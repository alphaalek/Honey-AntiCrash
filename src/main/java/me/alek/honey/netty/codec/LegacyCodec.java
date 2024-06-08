package me.alek.honey.netty.codec;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.util.MethodInvoker;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.Reflection;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public interface LegacyCodec {

    void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) throws CodecException;

    interface Applier {
        void apply(LegacyCodec codec) throws CodecException;
    }

    static LegacyCodec createDefault(PacketTypeEnum type) {
        return new LegacyCodec() {
            @Override
            public void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) throws CodecException {
                int packetId = type.getMinecraftID().get();

                // otherwise apply the default implementation for the serializer
                MethodHandle applicationMethod = LegacyCodecLookup.get().get(type.getState()).get(packetId, () -> {
                    MethodInvoker<?> invoker = Reflection.getMethod(type.getNmsClass(), 0, NMS.getPacketDataSerializerClass());
                    try {
                        return MethodHandles.lookup().unreflect(invoker.getHandle());
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                        Bukkit.getLogger().severe("Failed to create legacy serializer application lookup for packet " + packetId + ", " + type + ", " + type.getState());
                        return null;
                    }
                });
                try {
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = applicationMethod.invokeWithArguments(new Object[]{raw, serializer.getSerializer()});
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    throw new CodecException(packetId, "Failed to apply serializer for packet " + packetId + ", " + type + ", " + type.getState());
                }
            }
        };
    }
}
