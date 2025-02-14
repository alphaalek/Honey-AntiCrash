package me.alek.honey.netty.codec;

import me.alek.honey.util.reflection.MinecraftReflection;

public class Protocols {
    public static Class<Object> GAME_CLASS = MinecraftReflection.getGameProtocolsClass();
    public static Class<Object> LOGIN_CLASS = MinecraftReflection.getLoginProtocolsClass();
    public static Class<Object> HANDSHAKE_CLASS = MinecraftReflection.getHandshakeProtocolsClass();
    public static Class<Object> STATUS_CLASS = MinecraftReflection.getStatusProtocolsClass();
    public static Class<Object> CONFIGURATION_CLASS = MinecraftReflection.getConfigurationProtocolsClass();

    public static Class<Object> GAME_TYPES_CLASS = MinecraftReflection.getGamePacketTypesClass();
    public static Class<Object> LOGIN_TYPES_CLASS = MinecraftReflection.getLoginPacketTypesClass();
    public static Class<Object> HANDSHAKE_TYPES_CLASS = MinecraftReflection.getHandshakePacketTypesClass();
    public static Class<Object> STATUS_TYPES_CLASS = MinecraftReflection.getStatusPacketTypesClass();
    public static Class<Object> CONFIGURATION_TYPES_CLASS = MinecraftReflection.getConfigurationPacketTypesClass();
}
