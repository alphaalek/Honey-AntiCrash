package me.alek.honey.netty.codec;

import me.alek.honey.netty.util.NMS;

public class Protocols {

    public static Class<Object> GAME_CLASS = NMS.getGameProtocolsClass();
    public static Class<Object> LOGIN_CLASS = NMS.getLoginProtocolsClass();
    public static Class<Object> HANDSHAKE_CLASS = NMS.getHandshakeProtocolsClass();
    public static Class<Object> STATUS_CLASS = NMS.getStatusProtocolsClass();
    public static Class<Object> CONFIGURATION_CLASS = NMS.getConfigurationProtocolsClass();

    public static Class<Object> GAME_TYPES_CLASS = NMS.getGamePacketTypesClass();
    public static Class<Object> LOGIN_TYPES_CLASS = NMS.getLoginPacketTypesClass();
    public static Class<Object> HANDSHAKE_TYPES_CLASS = NMS.getHandshakePacketTypesClass();
    public static Class<Object> STATUS_TYPES_CLASS = NMS.getStatusPacketTypesClass();
    public static Class<Object> CONFIGURATION_TYPES_CLASS = NMS.getConfigurationPacketTypesClass();
}
