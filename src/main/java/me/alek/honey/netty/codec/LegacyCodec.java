package me.alek.honey.netty.codec;

import io.netty.handler.codec.CodecException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Array;
import me.alek.honey.util.protocol.NetProtocol;
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

    class LookupProvider {

        private final Array<MethodHandle> legacySerializerLookup = new Array<>();

        public Array<MethodHandle> getLookup() {
            return this.legacySerializerLookup;
        }

        public MethodHandle get(PacketType type) {
            return this.legacySerializerLookup.get(type);
        }
    }

    LookupProvider PROVIDER = new LookupProvider();

    static IStructureModifier<Object> getStructureModifier(PacketType type, Object packet) {
        return ReflectStructure.Provider.acquireStructure(type.getNmsClass())
                .withStatic(false).withTarget(packet);
    }

    void apply(Object raw, IStructureModifier<Object> structure, PacketDataSerializer serializer) throws CodecException;

    interface Callback {

        void apply(LegacyCodec codec) throws CodecException;
    }

    static LegacyCodec createDefault(PacketType type, PacketBound bound) {
        return (raw, structure, serializer) -> {
            // otherwise apply the default implementation for the serializer
            MethodHandle method = PROVIDER.getLookup().get(type, () -> {
                int methodIdx = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17) || bound.equals(PacketBound.SERVER) ? 0 : 1;
                MethodInvoker<?> invoker = Reflection.getMethodFromReturnType(type.getNmsClass(), methodIdx, void.class, NMS.getPacketDataSerializerClass());
                try {
                    return MethodHandles.lookup().unreflect(invoker.getHandle());
                } catch (IllegalAccessException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to create legacy serializer lookup for packet " + type + ", " + type.getState(), ex);
                    return null;
                }
            });

            try {
                // explicit set variable, so Proguard recognizes the method
                Object empty = method.invokeWithArguments(new Object[]{raw, serializer.getSerializer()});
            } catch (Throwable ex) {
                throw new CodecException("Failed to apply serializer for packet " + type + ", " + type.getState(), ex);
            }
        };
    }
}
