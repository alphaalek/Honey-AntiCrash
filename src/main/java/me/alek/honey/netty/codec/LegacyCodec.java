package me.alek.honey.netty.codec;

import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.Array;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.function.Supplier;
import java.util.logging.Level;

public interface LegacyCodec {

    class LookupProvider implements Supplier<EnumMap<PacketState, Array<MethodHandle>>> {

        private final EnumMap<PacketState, Array<MethodHandle>> legacySerializerLookup = new EnumMap<>(PacketState.class);

        public LookupProvider() {
            for (PacketState state : PacketState.values()) {
                legacySerializerLookup.put(state, new Array<>());
            }
        }

        @Override
        public EnumMap<PacketState, Array<MethodHandle>> get() {
            return legacySerializerLookup;
        }
    }

    LookupProvider PROVIDER = new LookupProvider();

    void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) throws CodecException;

    interface Callback {

        void apply(LegacyCodec codec) throws CodecException;
    }

    static LegacyCodec createDefault(PacketType type) {
        return (raw, structure, serializer) -> {
            int packetId = type.getMinecraftID().get();

            // otherwise apply the default implementation for the serializer
            MethodHandle method = PROVIDER.get().get(type.getState()).get(packetId, () -> {
                MethodInvoker<?> invoker = Reflection.getMethod(type.getNmsClass(), 0, NMS.getPacketDataSerializerClass());
                try {
                    return MethodHandles.lookup().unreflect(invoker.getHandle());
                } catch (IllegalAccessException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to create legacy serializer lookup for packet " + packetId + ", " + type + ", " + type.getState(), ex);
                    return null;
                }
            });

            try {
                // explicit set variable, so Proguard recognizes the method
                Object empty = method.invokeWithArguments(new Object[]{raw, serializer.getSerializer()});
            } catch (Throwable ex) {
                throw new CodecException(packetId, "Failed to apply serializer for packet " + packetId + ", " + type + ", " + type.getState());
            }
        };
    }
}
