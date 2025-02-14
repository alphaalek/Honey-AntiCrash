package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.reflection.MinecraftReflection;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CodecFactory {

    public static Object getProtocolInfo1_20_5(PacketBound bound, PacketState state) {
        Class<Object> protocolInfoClass = MinecraftReflection.getProtocolInfoClass();
        Class<Object> protocolInfoBinderClass = MinecraftReflection.getProtocolInfoBinderClass();

        BiFunction<Class<Object>, Class<Object>, IStructureModifier<Object>> get = (c, t) -> ReflectStructure.Provider.acquireStructure(c)
                .withType(t).withTarget(null);

        int index = bound.equals(PacketBound.CLIENT) ? 1 : 0;

        switch (state) {
            case HANDSHAKE:
                if (bound == PacketBound.CLIENT) {
                    throw new NoSuchElementException();
                }
                return get.apply(Protocols.HANDSHAKE_CLASS, protocolInfoClass).readField(0);
            case PLAY:
                return bindProtocolInfo1_20_5(
                        get.apply(Protocols.GAME_CLASS, protocolInfoBinderClass).readField(index));
            case STATUS:
                return get.apply(Protocols.STATUS_CLASS, protocolInfoClass).readField(index);
            case LOGIN:
                return get.apply(Protocols.LOGIN_CLASS, protocolInfoClass).readField(index);
            case CONFIGURATION:
                return get.apply(Protocols.CONFIGURATION_CLASS, protocolInfoClass).readField(index);
            default:
                return null;
        }
    }

    public static Object bindProtocolInfo1_20_5(Object protocolInfoBinder) {
        return ReflectStructure.Provider.acquireStructure(MinecraftReflection.getProtocolInfoBinderClass())
                .withType(MinecraftReflection.getProtocolInfoClass()).withTarget(protocolInfoBinder)
                .invokeMethod(null, "bind", 0, getRegistryCustomFunction1_20_5());
    }

    public static Function<ByteBuf, Object> getRegistryCustomFunction1_20_5() {
        Object layeredRegistryAccess = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getMinecraftServerClass())
                .withType(MinecraftReflection.getLayeredRegistryAccessClass()).withTarget(MinecraftReflection.getMinecraftServer())
                .readField(0);

        Object registryCustomDimension = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getLayeredRegistryAccessClass())
                .withType(MinecraftReflection.getRegistryCustomDimensionClass()).withTarget(layeredRegistryAccess)
                .readField(0);

        return ReflectStructure.Provider.acquireStructure(MinecraftReflection.getRegistryFriendlyByteBufClass())
                .<Function<ByteBuf, Object>>withWeakType(Function.class).withTarget(null)
                .invokeMethod(null, 0, registryCustomDimension);
    }

    public static Object getStreamCodec1_20_5(PacketBound bound, PacketState state) {
        Object protocolInfo = getProtocolInfo1_20_5(bound, state);

        return ReflectStructure.Provider.acquireStructure(MinecraftReflection.getProtocolInfoClass())
                .withType(MinecraftReflection.getStreamCodecClass()).withTarget(protocolInfo)
                .invokeMethod(null, 0);
    }
}
