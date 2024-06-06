package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.NetProtocolRange;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LazyInit;

import java.util.*;

public enum PacketType implements PacketTypeEnum {
    UNKNOWN;

    @Override
    public Class<?> getNmsClass() {
        return null;
    }

    @Override
    public LazyInit<Integer> getMinecraftID() {
        return null;
    }

    @Override
    public int getHoneyID() {
        return -1;
    }

    @Override
    public PacketState getState() {
        return PacketState.UNKNOWN;
    }

    @Override
    public PacketBound getBound() {
        return PacketBound.SERVER;
    }

    public static boolean isUnknown(PacketTypeEnum packetType) {
        return packetType == null || packetType == UNKNOWN;
    }

    public static Array<PacketState> getCommonStates(PacketTypeEnum type) {
        if (!type.isCommonState()) {
            return Array.from(type.getState());
        }
        return ((CommonPacketStateTypeEnum)type).getCommonStates();
    }

    private static void registerPacketDetails(PacketTypeEnum type) {
        if (type.isCommonState()) {
            Array<PacketState> commonStates = ((CommonPacketStateTypeEnum)type).getCommonStates();
            for (PacketState state : commonStates) {
                packetFromStateLookup.get(state).add(type);
            }
        } else {
            packetFromStateLookup.get(type.getState()).add(type);
        }

        if (type instanceof RangedPacketTypeEnum
                && !NetProtocol.matchProtocol((RangedPacketTypeEnum) type)) {
            return;
        }
        registeredPackets.put(type.getNmsClass(), type);
    }

    private static final NetProtocol protocolVersion = NetProtocol.getProtocol();
    private static final Map<Class<?>, PacketTypeEnum> registeredPackets = new IdentityHashMap<>();
    private static final Map<PacketState, List<PacketTypeEnum>> packetFromStateLookup = new HashMap<>();
    private static final IDRegistry idRegistry = new IDRegistry();

    public static void load() {
        for (PacketState state : PacketState.values()) {
            packetFromStateLookup.put(state, new ArrayList<>());
        }

        for (Common.Client commonClient : Common.Client.values()) {
            registerPacketDetails(commonClient); }
        for (Common.Server commonServer : Common.Server.values()) {
            registerPacketDetails(commonServer); }
        for (Handshake.Client handshakeClient : Handshake.Client.values()) {
            registerPacketDetails(handshakeClient); }
        for (Login.Client loginClient : Login.Client.values()) {
            registerPacketDetails(loginClient); }
        for (Login.Server loginServer : Login.Server.values()) {
            registerPacketDetails(loginServer); }
        for (Status.Client statusClient : Status.Client.values()) {
            registerPacketDetails(statusClient); }
        for (Status.Server statusServer : Status.Server.values()) {
            registerPacketDetails(statusServer); }
        for (Play.Client playClient : Play.Client.values()) {
            registerPacketDetails(playClient); }
        for (Play.Server playServer : Play.Server.values()) {
            registerPacketDetails(playServer); }
        for (Configuration.Client configurationClient : Configuration.Client.values()) {
            registerPacketDetails(configurationClient); }
        for (Configuration.Server configurationServer : Configuration.Server.values()) {
            registerPacketDetails(configurationServer); }
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(NetProtocol protocol, PacketState state, PacketBound packetBound) {
        if (state == PacketState.UNKNOWN) {
            return new ArrayList<>();
        }
        final ArrayList<PacketTypeEnum> packetTypes = new ArrayList<>();
        for (PacketTypeEnum packetType : packetFromStateLookup.get(state)) {

            if (packetBound != null && packetType.getBound() != packetBound) {
                continue;
            }
            if (packetType instanceof RangedPacketTypeEnum) {
                RangedPacketTypeEnum rangedPacketType = (RangedPacketTypeEnum) packetType;
                if (!NetProtocol.matchProtocol(rangedPacketType, protocol)) {
                    continue;
                }
            }
            packetTypes.add(packetType);
        }
        return packetTypes;
    }


    public static List<PacketTypeEnum> getTypesAvailableFor(NetProtocol protocol, PacketState state) {
        return getTypesAvailableFor(protocol, state, null);
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(NetProtocol protocol, PacketBound packetBound) {
        final List<PacketTypeEnum> totalList = new ArrayList<>();
        for (PacketState state : PacketState.values()) {
            totalList.addAll(getTypesAvailableFor(protocol, state, packetBound));
        }
        return totalList;
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(NetProtocol protocol) {
        return getTypesAvailableFor(protocol, (PacketBound) null);
    }

    public static List<PacketTypeEnum> getTypesAvailable() {
        return getTypesAvailableFor(protocolVersion);
    }

    public static Map<Class<?>, PacketTypeEnum> getRegisteredPackets() {
        return registeredPackets;
    }

    public static PacketTypeEnum getPacketType(Class<?> clazz) {
        return registeredPackets.getOrDefault(clazz, UNKNOWN);
    }

    public static class Common {

        private static Class<?> CLIENT_INFORMATION_CLASS;
        private static Class<?> COOKIE_RESPONSE_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_CLIENT_CLASS;
        private static Class<?> KEEP_ALIVE_CLIENT_CLASS;
        private static Class<?> PONG_CLASS;
        private static Class<?> RESOURCE_PACK_CLIENT_CLASS;

        static {
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CLIENT_INFORMATION_CLASS = Reflection.getClass("{common}.ServerboundClientInformationPacket");
                CUSTOM_PAYLOAD_CLIENT_CLASS = Reflection.getClass("{common}.ServerboundCustomPayloadPacket");
                KEEP_ALIVE_CLIENT_CLASS = Reflection.getClass("{common}.ServerboundKeepAlivePacket");
                PONG_CLASS = Reflection.getClass("{common}.ServerboundPongPacket");
                RESOURCE_PACK_CLIENT_CLASS = Reflection.getClass("{common}.ServerboundResourcePackPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                COOKIE_RESPONSE_CLASS = Reflection.getClass("{cookie}.ServerboundCookieResponsePacket");
            }
        }

        public enum Client implements CommonPacketStateTypeEnum, RangedPacketTypeEnum {
            CLIENT_INFORMATION(CLIENT_INFORMATION_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            COOKIE_RESPONSE(COOKIE_RESPONSE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_5), Array.fromData(PacketState.PLAY, PacketState.LOGIN, PacketState.CONFIGURATION)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            KEEP_ALIVE(KEEP_ALIVE_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            PONG(PONG_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            RESOURCE_PACK(RESOURCE_PACK_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsClass;
            private final int packetId;
            private final NetProtocolRange range;
            private final Array<PacketState> states;

            Client(Class<?> nmsClass, int packetId, NetProtocolRange range, Array<PacketState> states) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.range = range;
                this.states = states;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsClass;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.SERVER;
            }

            @Override
            public NetProtocolRange getRange() {
                return range;
            }

            @Override
            public Array<PacketState> getCommonStates() {
                return states;
            }
        }

        private static Class<?> COOKIE_REQUEST_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_SERVER_CLASS;
        private static Class<?> DISCONNECT_CLASS;
        private static Class<?> KEEP_ALIVE_SERVER_CLASS;
        private static Class<?> PING_CLASS;
        private static Class<?> RESOURCE_PACK_SERVER_CLASS;
        private static Class<?> RESOURCE_PACK_POP_CLASS;
        private static Class<?> RESOURCE_PACK_PUSH_CLASS;
        private static Class<?> UPDATE_TAGS_CLASS;

        static {
            if (protocolVersion.isEqual(NetProtocol.v1_20_2)) {
                RESOURCE_PACK_SERVER_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{common}.ClientboundCustomPayloadPacket");
                DISCONNECT_CLASS = Reflection.getClass("{common}.ClientboundDisconnectPacket");
                KEEP_ALIVE_SERVER_CLASS = Reflection.getClass("{common}.ClientboundKeepAlivePacket");
                PING_CLASS = Reflection.getClass("{common}.ClientboundPingPacket");
                UPDATE_TAGS_CLASS = Reflection.getClass("{common}.ClientboundUpdateTagsPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_4)) {
                RESOURCE_PACK_POP_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPopPacket");
                RESOURCE_PACK_PUSH_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPushPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                COOKIE_REQUEST_CLASS = Reflection.getClass("{cookie}.ClientboundCookieRequestPacket");
            }
        }

        public enum Server implements CommonPacketStateTypeEnum, RangedPacketTypeEnum {
            COOKIE_REQUEST(COOKIE_REQUEST_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_5), Array.fromData(PacketState.PLAY, PacketState.LOGIN, PacketState.CONFIGURATION)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            DISCONNECT(DISCONNECT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            PING(PING_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            RESOURCE_PACK(RESOURCE_PACK_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_20_2, NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            RESOURCE_PACK_POP(RESOURCE_PACK_POP_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_4), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            RESOURCE_PACK_PUSH(RESOURCE_PACK_PUSH_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_4), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION)),
            UPDATE_TAGS(UPDATE_TAGS_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2), Array.fromData(PacketState.PLAY, PacketState.CONFIGURATION))
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsClass;
            private final int packetId;
            private final NetProtocolRange range;
            private final Array<PacketState> states;

            Server(Class<?> nmsClass, int packetId, NetProtocolRange range, Array<PacketState> states) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.range = range;
                this.states = states;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsClass;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.CLIENT;
            }

            @Override
            public NetProtocolRange getRange() {
                return range;
            }

            @Override
            public Array<PacketState> getCommonStates() {
                return states;
            }
        }
    }

    public static class Handshake {

        private static final Class<?> SET_PROTOCOL_CLASS = Reflection.getClass("{handshake}.PacketHandshakingInSetProtocol");

        public enum Client implements PacketTypeEnum {

            SET_PROTOCOL(SET_PROTOCOL_CLASS, idRegistry.acquire());

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;

            Client(Class<?> nmsPacket, int packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.SERVER;
            }

            @Override
            public PacketState getState() {
                return PacketState.HANDSHAKE;
            }
        }
    }

    public static class Login {

        private static final Class<?> ENCRYPTION_BEGIN_CLASS = Reflection.getClass("{login}.PacketLoginInEncryptionBegin");
        private static final Class<?> LOGIN_START_CLASS = Reflection.getClass("{login}.PacketLoginInStart");
        private static Class<?> CUSTOM_PAYLOAD_CLIENT_CLASS;
        private static Class<?> CUSTOM_QUERY_ANSWER_CLASS;
        private static Class<?> LOGIN_ACKNOWLEDGED_CLASS;

        static {
            if (protocolVersion.isBetween(NetProtocol.v1_13, NetProtocol.v1_20_1)) {
                CUSTOM_PAYLOAD_CLIENT_CLASS = Reflection.getClass("{login}.PacketLoginInCustomPayload");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CUSTOM_QUERY_ANSWER_CLASS = Reflection.getClass("{login}.ServerboundCustomQueryAnswerPacket");
                LOGIN_ACKNOWLEDGED_CLASS = Reflection.getClass("{login}.ServerboundLoginAcknowledgedPacket");
            }
        }

        public enum Client implements RangedPacketTypeEnum {
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_13, NetProtocol.v1_20_1)),
            CUSTOM_QUERY_ANSWER(CUSTOM_QUERY_ANSWER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            ENCRYPTION_BEGIN(ENCRYPTION_BEGIN_CLASS, idRegistry.acquire()),
            LOGIN_ACKNOWLEDGED(LOGIN_ACKNOWLEDGED_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            LOGIN_START(LOGIN_START_CLASS, idRegistry.acquire());

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;
            private final NetProtocolRange protocolRange;

            Client(Class<?> nmsPacket, int packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = NetProtocolRange.ALL;
            }

            Client(Class<?> nmsPacket, int packetId, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.SERVER;
            }

            @Override
            public PacketState getState() {
                return PacketState.LOGIN;
            }
        }

        private static final Class<?> DISCONNECT_CLASS = Reflection.getClass("{login}.PacketLoginOutDisconnect");
        private static final Class<?> ENCRYPTION_REQUEST_CLASS = Reflection.getClass("{login}.PacketLoginOutEncryptionBegin");
        private static final Class<?> LOGIN_SUCCESS_CLASS = Reflection.getClass("{login}.PacketLoginOutSuccess");
        private static final Class<?> SET_COMPRESSION_CLASS = Reflection.getClass("{login}.PacketLoginOutSetCompression");
        private static Class<?> CUSTOM_PAYLOAD_SERVER_CLASS;

        static {
            if (protocolVersion.isBetween(NetProtocol.v1_13, NetProtocol.v1_20_1)) {
                CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{login}.PacketLoginOutCustomPayload");
            }
        }

        public enum Server implements RangedPacketTypeEnum {
            DISCONNECT(DISCONNECT_CLASS, idRegistry.acquire()),
            ENCRYPTION_REQUEST(ENCRYPTION_REQUEST_CLASS, idRegistry.acquire()),
            LOGIN_SUCCESS(LOGIN_SUCCESS_CLASS, idRegistry.acquire()),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_13, NetProtocol.v1_20_1)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, idRegistry.acquire());

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;
            private final NetProtocolRange protocolRange;

            Server(Class<?> nmsPacket, int packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                protocolRange = NetProtocolRange.ALL;
            }

            Server(Class<?> nmsPacket, int packetId, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.CLIENT;
            }

            @Override
            public PacketState getState() {
                return PacketState.LOGIN;
            }
        }
    }

    public static class Status {

        private static final Class<?> PING_CLASS = Reflection.getClass("{status}.PacketStatusInPing");
        private static final Class<?> START_CLASS = Reflection.getClass("{status}.PacketStatusInStart");

        public enum Client implements PacketTypeEnum {

            PING_REQUEST(PING_CLASS, idRegistry.acquire()),
            STATUS_REQUEST(START_CLASS, idRegistry.acquire());

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;

            Client(Class<?> nmsPacket, int packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.SERVER;
            }

            @Override
            public PacketState getState() {
                return PacketState.STATUS;
            }
        }

        private static final Class<?> PONG_CLASS = Reflection.getClass("{status}.PacketStatusOutPong");
        private static final Class<?> SERVER_INFO_CLASS = Reflection.getClass("{status}.PacketStatusOutServerInfo");

        public enum Server implements PacketTypeEnum {

            PING_RESPONSE(PONG_CLASS, idRegistry.acquire()),
            STATUS_RESPONSE(SERVER_INFO_CLASS, idRegistry.acquire());

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;

            Server(Class<?> nmsPacket, int packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.CLIENT;
            }

            @Override
            public PacketState getState() {
                return PacketState.STATUS;
            }
        }
    }

    public static class Play {

        private static final Class<?> ABILITIES_CLASS = Reflection.getClass("{play}.PacketPlayInAbilities");
        private static final Class<?> ARM_ANIMATION_CLASS = Reflection.getClass("{play}.PacketPlayInArmAnimation");
        private static final Class<?> BLOCK_DIG_CLASS = Reflection.getClass("{play}.PacketPlayInBlockDig");
        private static final Class<?> BLOCK_PLACE_CLASS = Reflection.getClass("{play}.PacketPlayInBlockPlace");
        private static final Class<?> CHAT_CLASS = Reflection.getClass("{play}.PacketPlayInChat");
        private static final Class<?> CLIENT_COMMAND_CLASS = Reflection.getClass("{play}.PacketPlayInClientCommand");
        private static final Class<?> CLOSE_WINDOW_CLASS = Reflection.getClass("{play}.PacketPlayInCloseWindow");
        private static final Class<?> ENCHANT_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayInEnchantItem");
        private static final Class<?> ENTITY_ACTION_CLASS = Reflection.getClass("{play}.PacketPlayInEntityAction");
        private static final Class<?> HELD_ITEM_SLOT_CLASS = Reflection.getClass("{play}.PacketPlayInHeldItemSlot");
        private static final Class<?> SPECTATE_CLASS = Reflection.getClass("{play}.PacketPlayInSpectate");
        private static final Class<?> STEER_VEHICLE_CLASS = Reflection.getClass("{play}.PacketPlayInSteerVehicle");
        private static final Class<?> SET_CREATIVE_SLOT_CLASS = Reflection.getClass("{play}.PacketPlayInSetCreativeSlot");
        private static final Class<?> TAB_COMPLETE_CLASS = Reflection.getClass("{play}.PacketPlayInTabComplete");
        private static final Class<?> UPDATE_SIGN_CLASS = Reflection.getClass("{play}.PacketPlayInUpdateSign");
        private static final Class<?> USE_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayInUseEntity");
        private static final Class<?> WINDOW_CLICK_CLASS = Reflection.getClass("{play}.PacketPlayInWindowClick");
        private static Class<?> ADVANCEMENTS_CLASS;
        private static Class<?> AUTO_RECIPE_CLASS;
        private static Class<?> B_EDIT_CLASS;
        private static Class<?> BEACON_CLASS;
        private static Class<?> BOAT_MOVE_CLASS;
        private static Class<?> CHAT_ACK_CLASS;
        private static Class<?> CHAT_COMMAND_CLASS;
        private static Class<?> CHAT_PREVIEW_CLIENT_CLASS;
        private static Class<?> CHUNK_BATCH_RECEIVED_CLASS;
        private static Class<?> CONFIGURATION_ACKNOWLEDGED_CLASS;
        private static Class<?> CONTAINER_SLOT_STATE_CHANGED_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_CLASS;
        private static Class<?> DIFFICULTY_CHANGE_CLASS;
        private static Class<?> DIFFICULTY_LOCK_CLASS;
        private static Class<?> ENTITY_NBT_QUERY_CLASS;
        private static Class<?> FLYING_CLASS;
        private static Class<?> GROUND_CLASS;
        private static Class<?> ITEM_NAME_CLASS;
        private static Class<?> JIGSAW_GENERATE_CLASS;
        private static Class<?> KEEP_ALIVE_CLIENT_CLASS;
        private static Class<?> LOOK_CLASS;
        private static Class<?> POSITION_CLASS;
        private static Class<?> POSITION_LOOK_CLASS;
        private static Class<?> PICK_ITEM_CLASS;
        private static Class<?> PONG_CLASS;
        private static Class<?> RECIPE_DISPLAYED_CLASS;
        private static Class<?> RECIPE_SETTINGS_CLASS;
        private static Class<?> RESOURCE_PACK_STATUS_CLASS;
        private static Class<?> CHAT_SESSION_UPDATE_CLASS;
        private static Class<?> SET_COMMAND_BLOCK_CLASS;
        private static Class<?> SET_COMMAND_MINECART_CLASS;
        private static Class<?> SET_JIGSAW_CLASS;
        private static Class<?> SETTINGS_CLASS;
        private static Class<?> STRUCT_CLASS;
        private static Class<?> TELEPORT_ACCEPT_CLASS;
        private static Class<?> TILE_NBT_QUERY_CLASS;
        private static Class<?> TR_SEL_CLASS;
        private static Class<?> TRANSACTION_CLASS;
        private static Class<?> USE_ITEM_CLASS;
        private static Class<?> VEHICLE_MOVE_CLASS;

        static {
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                TRANSACTION_CLASS = Reflection.getClass("{play}.PacketPlayInTransaction");
                FLYING_CLASS = Reflection.getClass("{play}.PacketPlayInFlying");
                POSITION_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInPosition");
                POSITION_LOOK_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInPositionLook");
                LOOK_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInLook");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_9)) {
                VEHICLE_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayInVehicleMove");
                BOAT_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayInBoatMove");
                USE_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayInUseItem");
                TELEPORT_ACCEPT_CLASS = Reflection.getClass("{play}.PacketPlayInTeleportAccept");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_12)) {
                AUTO_RECIPE_CLASS = Reflection.getClass("{play}.PacketPlayInAutoRecipe");
                RECIPE_DISPLAYED_CLASS = Reflection.getClass("{play}.PacketPlayInRecipeDisplayed");
                ADVANCEMENTS_CLASS = Reflection.getClass("{play}.PacketPlayInAdvancements");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_13)) {
                TILE_NBT_QUERY_CLASS = Reflection.getClass("{play}.PacketPlayInTileNBTQuery");
                ENTITY_NBT_QUERY_CLASS = Reflection.getClass("{play}.PacketPlayInEntityNBTQuery");
                B_EDIT_CLASS = Reflection.getClass("{play}.PacketPlayInBEdit");
                PICK_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayInPickItem");
                ITEM_NAME_CLASS = Reflection.getClass("{play}.PacketPlayInItemName");
                TR_SEL_CLASS = Reflection.getClass("{play}.PacketPlayInTrSel");
                BEACON_CLASS = Reflection.getClass("{play}.PacketPlayInBeacon");
                SET_COMMAND_BLOCK_CLASS = Reflection.getClass("{play}.PacketPlayInSetCommandBlock");
                SET_COMMAND_MINECART_CLASS = Reflection.getClass("{play}.PacketPlayInSetCommandMinecart");
                STRUCT_CLASS = Reflection.getClass("{play}.PacketPlayInStruct");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_14)) {
                DIFFICULTY_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayInDifficultyChange");
                DIFFICULTY_LOCK_CLASS = Reflection.getClass("{play}.PacketPlayInDifficultyLock");
                SET_JIGSAW_CLASS = Reflection.getClass("{play}.PacketPlayInSetJigsaw");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_16_1)) {
                JIGSAW_GENERATE_CLASS = Reflection.getClass("{play}.PacketPlayInJigsawGenerate");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_16_2)) {
                RECIPE_SETTINGS_CLASS = Reflection.getClass("{play}.PacketPlayInRecipeSettings");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_17)) {
                POSITION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPosition");
                POSITION_LOOK_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPositionLook");
                LOOK_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInLook");
                GROUND_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$d");
                if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                    PONG_CLASS = Reflection.getClass("{play}.ServerboundPongPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19)) {
                CHAT_COMMAND_CLASS = Reflection.getClass("{play}.ServerboundChatCommandPacket");
                if (protocolVersion.isOlderThan(NetProtocol.v1_19_3)) {
                    CHAT_PREVIEW_CLIENT_CLASS = Reflection.getClass("{play}.ServerboundChatPreviewPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_1)) {
                CHAT_ACK_CLASS = Reflection.getClass("{play}.ServerboundChatAckPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_3)) {
                CHAT_SESSION_UPDATE_CLASS = Reflection.getClass("{play}.ServerboundChatSessionUpdatePacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CHUNK_BATCH_RECEIVED_CLASS = Reflection.getClass("{play}.ServerboundChunkBatchReceivedPacket");
                CONFIGURATION_ACKNOWLEDGED_CLASS = Reflection.getClass("{play}.ServerboundConfigurationAcknowledgedPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_4)) {
                CONTAINER_SLOT_STATE_CHANGED_CLASS = Reflection.getClass("{play}.ServerboundContainerSlotStateChangedPacket");
            }
            if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{play}.PacketPlayInCustomPayload");
                KEEP_ALIVE_CLIENT_CLASS = Reflection.getClass("{play}.PacketPlayInKeepAlive");
                RESOURCE_PACK_STATUS_CLASS = Reflection.getClass("{play}.PacketPlayInResourcePackStatus");
                SETTINGS_CLASS = Reflection.getClass("{play}.PacketPlayInSettings");
            }
        }

        public enum Client implements RangedPacketTypeEnum {

            ABILITIES(ABILITIES_CLASS, idRegistry.acquire()),
            ADVANCEMENTS(ADVANCEMENTS_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12)),
            ARM_ANIMATION(ARM_ANIMATION_CLASS, idRegistry.acquire()),
            AUTO_RECIPE(AUTO_RECIPE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12)),
            B_EDIT(B_EDIT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            BEACON(BEACON_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            BLOCK_DIG(BLOCK_DIG_CLASS, idRegistry.acquire()),
            BLOCK_PLACE(BLOCK_PLACE_CLASS, idRegistry.acquire()),
            BOAT_MOVE(BOAT_MOVE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            CHAT(CHAT_CLASS, idRegistry.acquire()),
            CHAT_ACK(CHAT_ACK_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_1)),
            CHAT_PREVIEW(CHAT_PREVIEW_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            CHAT_COMMAND(CHAT_COMMAND_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19)),
            CHUNK_BATCH_RECEIVED(CHUNK_BATCH_RECEIVED_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            CLIENT_COMMAND(CLIENT_COMMAND_CLASS, idRegistry.acquire()),
            CLOSE_WINDOW(CLOSE_WINDOW_CLASS, idRegistry.acquire()),
            CONFIGURATION_ACKNOWLEDGED(CONFIGURATION_ACKNOWLEDGED_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            CONTAINER_SLOT_STATE_CHANGED(CONTAINER_SLOT_STATE_CHANGED_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_4)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            DIFFICULTY_CHANGE(DIFFICULTY_CHANGE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            DIFFICULTY_LOCK(DIFFICULTY_LOCK_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            ENCHANT_ITEM(ENCHANT_ITEM_CLASS, idRegistry.acquire()),
            ENTITY_ACTION(ENTITY_ACTION_CLASS, idRegistry.acquire()),
            ENTITY_NBT_QUERY(ENTITY_NBT_QUERY_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            FLYING(FLYING_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_16_5)),
            GROUND(GROUND_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            HELD_ITEM_SLOT(HELD_ITEM_SLOT_CLASS, idRegistry.acquire()),
            ITEM_NAME(ITEM_NAME_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            JIGSAW_GENERATE(JIGSAW_GENERATE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_16_1)),
            KEEP_ALIVE(KEEP_ALIVE_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            LOOK(LOOK_CLASS, idRegistry.acquire()),
            PICK_ITEM(PICK_ITEM_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            PONG(PONG_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_17, NetProtocol.v1_20_1)),
            POSITION(POSITION_CLASS, idRegistry.acquire()),
            POSITION_LOOK(POSITION_LOOK_CLASS, idRegistry.acquire()),
            RECIPE_DISPLAYED(RECIPE_DISPLAYED_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12)),
            RECIPE_SETTINGS(RECIPE_SETTINGS_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_16_2)),
            RESOURCE_PACK_STATUS(RESOURCE_PACK_STATUS_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            CHAT_SESSION_UPDATE(CHAT_SESSION_UPDATE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_3)),
            SET_COMMAND_BLOCK(SET_COMMAND_BLOCK_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            SET_COMMAND_MINECART(SET_COMMAND_MINECART_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            SET_CREATIVE_SLOT(SET_CREATIVE_SLOT_CLASS, idRegistry.acquire()),
            SET_JIGSAW(SET_JIGSAW_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            SETTINGS(SETTINGS_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            SPECTATE(SPECTATE_CLASS, idRegistry.acquire()),
            STEER_VEHICLE(STEER_VEHICLE_CLASS, idRegistry.acquire()),
            STRUCT(STRUCT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_CLASS, idRegistry.acquire()),
            TELEPORT_ACCEPT(TELEPORT_ACCEPT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            TILE_NBT_QUERY(TILE_NBT_QUERY_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            TR_SEL(TR_SEL_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            TRANSACTION(TRANSACTION_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_16_5)),
            UPDATE_SIGN(UPDATE_SIGN_CLASS, idRegistry.acquire()),
            USE_ENTITY(USE_ENTITY_CLASS, idRegistry.acquire()),
            USE_ITEM(USE_ITEM_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            VEHICLE_MOVE(VEHICLE_MOVE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            WINDOW_CLICK(WINDOW_CLICK_CLASS, idRegistry.acquire()),
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsClass;
            private final int packetId;
            private final NetProtocolRange protocolRange;

            Client(Class<?> nmsClass, int packetId) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = NetProtocolRange.ALL;
            }

            Client(Class<?> nmsClass, int packetId, NetProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsClass;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketState getState() {
                return PacketState.PLAY;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.SERVER;
            }

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }
        }

        private static final Class<?> ABILITIES_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutAbilities");
        private static final Class<?> ANIMATION_CLASS = Reflection.getClass("{play}.PacketPlayOutAnimation");
        private static final Class<?> ATTACH_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutAttachEntity");
        private static final Class<?> BLOCK_ACTION_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockAction");
        private static final Class<?> BLOCK_BREAK_ANIMATION_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockBreakAnimation");
        private static final Class<?> BLOCK_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockChange");
        private static final Class<?> CAMERA_CLASS = Reflection.getClass("{play}.PacketPlayOutCamera");
        private static final Class<?> CLOSE_WINDOW_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutCloseWindow");
        private static final Class<?> COLLECT_CLASS = Reflection.getClass("{play}.PacketPlayOutCollect");
        private static final Class<?> ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutEntity");
        private static final Class<?> ENTITY_DESTROY_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityDestroy");
        private static final Class<?> ENTITY_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityEffect");
        private static final Class<?> ENTITY_EQUIPMENT_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityEquipment");
        private static final Class<?> ENTITY_HEAD_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityHeadRotation");
        private static final Class<?> ENTITY_METADATA_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityMetadata");
        private static final Class<?> ENTITY_STATUS_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityStatus");
        private static final Class<?> ENTITY_TELEPORT_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityTeleport");
        private static final Class<?> ENTITY_VELOCITY_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityVelocity");
        private static final Class<?> EXPERIENCE_CLASS = Reflection.getClass("{play}.PacketPlayOutExperience");
        private static final Class<?> EXPLOSION_CLASS = Reflection.getClass("{play}.PacketPlayOutExplosion");
        private static final Class<?> GAME_STATE_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayOutGameStateChange");
        private static final Class<?> HELD_ITEM_SLOT_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutHeldItemSlot");
        private static final Class<?> LOGIN_CLASS = Reflection.getClass("{play}.PacketPlayOutLogin");
        private static final Class<?> MAP_CLASS = Reflection.getClass("{play}.PacketPlayOutMap");
        private static final Class<?> MULTI_BLOCK_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayOutMultiBlockChange");
        private static final Class<?> NAMED_SOUND_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayOutNamedSoundEffect");
        private static final Class<?> OPEN_SIGN_EDITOR_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenSignEditor");
        private static final Class<?> OPEN_WINDOW_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenWindow");
        private static final Class<?> PLAYER_INFO_CLASS = Reflection.getFuzzyClass("{play}.PacketPlayOutPlayerInfo", "{play}.ClientboundPlayerInfoUpdatePacket");
        private static final Class<?> PLAYER_LIST_HEADER_FOOTER_CLASS = Reflection.getClass("{play}.PacketPlayOutPlayerListHeaderFooter");
        private static final Class<?> POSITION_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutPosition");
        private static final Class<?> REMOVE_ENTITY_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayOutRemoveEntityEffect");
        private static final Class<?> RESPAWN_CLASS = Reflection.getClass("{play}.PacketPlayOutRespawn");
        private static final Class<?> SCOREBOARD_DISPLAY_OBJECTIVE_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardDisplayObjective");
        private static final Class<?> SCOREBOARD_OBJECTIVE_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardObjective");
        private static final Class<?> SCOREBOARD_SCORE_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardScore");
        private static final Class<?> SCOREBOARD_TEAM_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardTeam");
        private static final Class<?> SERVER_DIFFICULTY_CLASS = Reflection.getClass("{play}.PacketPlayOutServerDifficulty");
        private static final Class<?> SET_SLOT_CLASS = Reflection.getClass("{play}.PacketPlayOutSetSlot");
        private static final Class<?> SPAWN_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntity");
        private static final Class<?> SPAWN_ENTITY_EXPERIENCE_ORB_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntityExperienceOrb");
        private static final Class<?> SPAWN_POSITION_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnPosition");
        private static final Class<?> STATISTIC_CLASS = Reflection.getClass("{play}.PacketPlayOutStatistic");
        private static final Class<?> TAB_COMPLETE_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutTabComplete");
        private static final Class<?> TILE_ENTITY_DATA_CLASS = Reflection.getClass("{play}.PacketPlayOutTileEntityData");
        private static final Class<?> UPDATE_ATTRIBUTES_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateAttributes");
        private static final Class<?> UPDATE_HEALTH_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateHealth");
        private static final Class<?> UPDATE_TIME_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateTime");
        private static final Class<?> WINDOW_DATA_CLASS = Reflection.getClass("{play}.PacketPlayOutWindowData");
        private static final Class<?> WINDOW_ITEMS_CLASS = Reflection.getClass("{play}.PacketPlayOutWindowItems");
        private static final Class<?> WORLD_EVENT_CLASS = Reflection.getClass("{play}.PacketPlayOutWorldEvent");
        private static final Class<?> WORLD_PARTICLES_CLASS = Reflection.getClass("{play}.PacketPlayOutWorldParticles");
        private static Class<?> ADD_VIBRATION_SIGNAL_CLASS;
        private static Class<?> ADVANCEMENTS_SERVER_CLASS;
        private static Class<?> AUTO_RECIPE_SERVER_CLASS;
        private static Class<?> BED_CLASS;
        private static Class<?> BLOCK_BREAK_CLASS;
        private static Class<?> BLOCK_CHANGED_ACK_CLASS;
        private static Class<?> BOSS_CLASS;
        private static Class<?> BUNDLE_CLASS;
        private static Class<?> CHAT_PREVIEW_SERVER_CLASS;
        private static Class<?> CHUNK_BATCH_FINISHED_CLASS;
        private static Class<?> CHUNK_BATCH_START_CLASS;
        private static Class<?> CHUNKS_BIOMES_CLASS;
        private static Class<?> CLEAR_TITLES_CLASS;
        private static Class<?> COMBAT_EVENT_CLASS;
        private static Class<?> COMMANDS_CLASS;
        private static Class<?> CUSTOM_CHAT_COMPLETIONS_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_SERVER_CLASS;
        private static Class<?> DAMAGE_EVENT_CLASS;
        private static Class<?> DELETE_CHAT_CLASS;
        private static Class<?> DISGUISED_CHAT_CLASS;
        private static Class<?> ENTITY_LOOK_CLASS;
        private static Class<?> ENTITY_SOUND_CLASS;
        private static Class<?> HURT_ANIMATION_CLASS;
        private static Class<?> INITIALIZE_BORDER_CLASS;
        private static Class<?> KEEP_ALIVE_SERVER_CLASS;
        private static Class<?> KICK_DISCONNECT_CLASS;
        private static Class<?> LEVEL_CHUNK_DATA_CLASS;
        private static Class<?> LEVEL_CHUNK_WITH_LIGHT_CLASS;
        private static Class<?> LIGHT_UPDATE_CLASS;
        private static Class<?> LOOK_AT_CLASS;
        private static Class<?> MAP_CHUNK_CLASS;
        private static Class<?> MAP_CHUNK_BULK_CLASS;
        private static Class<?> MOUNT_CLASS;
        private static Class<?> NAMED_ENTITY_SPAWN_CLASS;
        private static Class<?> NBT_QUERY_CLASS;
        private static Class<?> OPEN_BOOK_CLASS;
        private static Class<?> OPEN_WINDOW_HORSE_CLASS;
        private static Class<?> OPEN_WINDOW_MERCHANT_CLASS;
        private static Class<?> PING_CLASS;
        private static Class<?> PLAYER_CHAT_CLASS;
        private static Class<?> PLAYER_CHAT_HEADER_CLASS;
        private static Class<?> PLAYER_COMBAT_END_CLASS;
        private static Class<?> PLAYER_COMBAT_ENTER_CLASS;
        private static Class<?> PLAYER_COMBAT_KILL_CLASS;
        private static Class<?> PLAYER_INFO_REMOVE_CLASS;
        private static Class<?> PLAYER_INFO_UPDATE_CLASS;
        private static Class<?> RECIPE_UPDATE_CLASS;
        private static Class<?> RECIPES_CLASS;
        private static Class<?> RESET_SCORE_CLASS;
        private static Class<?> REL_ENTITY_MOVE_LOOK_CLASS;
        private static Class<?> REL_ENTITY_MOVE_CLASS;
        private static Class<?> RESOURCE_PACK_SEND_CLASS;
        private static Class<?> SELECT_ADVANCEMENT_TAB_CLASS;
        private static Class<?> SERVER_DATA_CLASS;
        private static Class<?> SET_ACTION_BAR_TEXT_CLASS;
        private static Class<?> SET_BORDER_CENTER_CLASS;
        private static Class<?> SET_BORDER_LERP_SIZE_CLASS;
        private static Class<?> SET_BORDER_WARNING_DELAY_CLASS;
        private static Class<?> SET_BORDER_WARNING_DISTANCE_CLASS;
        private static Class<?> SET_BORDER_SIZE_CLASS;
        private static Class<?> SET_COMPRESSION_CLASS;
        private static Class<?> SET_COOLDOWN_CLASS;
        private static Class<?> SET_DISPLAY_CHAT_PREVIEW_CLASS;
        private static Class<?> SET_SUBTITLE_TEXT_CLASS;
        private static Class<?> SET_SIMULATION_DISTANCE_CLASS;
        private static Class<?> SET_TITLE_TEXT_CLASS;
        private static Class<?> SET_TITLES_ANIMATION_CLASS;
        private static Class<?> SPAWN_ENTITY_LIVING_CLASS;
        private static Class<?> SPAWN_ENTITY_PAINTING_CLASS;
        private static Class<?> SPAWN_ENTITY_WEATHER_CLASS;
        private static Class<?> SYSTEM_CHAT_CLASS;
        private static Class<?> START_CONFIGURATION_CLASS;
        private static Class<?> STOP_SOUND_CLASS;
        private static Class<?> TAGS_CLASS;
        private static Class<?> TRANSACTION_SERVER_CLASS;
        private static Class<?> TICKING_STATE_CLASS;
        private static Class<?> TICKING_STEP_CLASS;
        private static Class<?> TITLE_CLASS;
        private static Class<?> UNLOAD_CHUNK_CLASS;
        private static Class<?> UPDATE_ENABLED_FEATURES_CLASS;
        private static Class<?> UPDATE_ENTITY_NBT_CLASS;
        private static Class<?> UPDATE_SIGN_SERVER_CLASS;
        private static Class<?> VEHICLE_MOVE_SERVER_CLASS;
        private static Class<?> VIEW_CENTRE_CLASS;
        private static Class<?> VIEW_DISTANCE_CLASS;
        private static Class<?> WORLD_BORDER_CLASS;

        static {
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                MAP_CHUNK_BULK_CLASS = Reflection.getClass("{play}.PacketPlayOutMapChunkBulk");
                UPDATE_ENTITY_NBT_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateEntityNBT");
                SET_COMPRESSION_CLASS = Reflection.getClass("{play}.PacketPlayOutSetCompression");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_9_2)) {
                UPDATE_SIGN_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateSign");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_13_2)) {
                BED_CLASS = Reflection.getClass("{play}.PacketPlayOutBed");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_15_2)) {
                SPAWN_ENTITY_WEATHER_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntityWeather");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                COMBAT_EVENT_CLASS = Reflection.getClass("{play}.PacketPlayOutCombatEvent");
                ENTITY_LOOK_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutEntityLook");
                REL_ENTITY_MOVE_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutRelEntityMove");
                REL_ENTITY_MOVE_LOOK_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutRelEntityMoveLook");
                TRANSACTION_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutTransaction");
                TITLE_CLASS = Reflection.getClass("{play}.PacketPlayOutTitle");
                WORLD_BORDER_CLASS = Reflection.getClass("{play}.PacketPlayOutWorldBorder");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_17_1)) {
                MAP_CHUNK_CLASS = Reflection.getClass("{play}.PacketPlayOutMapChunk");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_18_2)) {
                PLAYER_CHAT_CLASS = Reflection.getClass("{play}.PacketPlayOutChat");
                SPAWN_ENTITY_LIVING_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntityLiving");
                SPAWN_ENTITY_PAINTING_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntityPainting");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_9)) {
                BOSS_CLASS = Reflection.getClass("{play}.PacketPlayOutBoss");
                MOUNT_CLASS = Reflection.getClass("{play}.PacketPlayOutMount");
                UNLOAD_CHUNK_CLASS = Reflection.getClass("{play}.PacketPlayOutUnloadChunk");
                VEHICLE_MOVE_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutVehicleMove");
                SET_COOLDOWN_CLASS = Reflection.getClass("{play}.PacketPlayOutSetCooldown");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_12)) {
                ADVANCEMENTS_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutAdvancements");
                RECIPES_CLASS = Reflection.getClass("{play}.PacketPlayOutRecipes");
                SELECT_ADVANCEMENT_TAB_CLASS = Reflection.getClass("{play}.PacketPlayOutSelectAdvancementTab");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_12_1)) {
                AUTO_RECIPE_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutAutoRecipe");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_13)) {
                COMMANDS_CLASS = Reflection.getClass("{play}.PacketPlayOutCommands");
                NBT_QUERY_CLASS = Reflection.getClass("{play}.PacketPlayOutNBTQuery");
                RECIPE_UPDATE_CLASS = Reflection.getClass("{play}.PacketPlayOutRecipeUpdate");
                STOP_SOUND_CLASS = Reflection.getClass("{play}.PacketPlayOutStopSound");
                LOOK_AT_CLASS = Reflection.getClass("{play}.PacketPlayOutLookAt");
                if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                    TAGS_CLASS = Reflection.getClass("{play}.PacketPlayOutTags");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_14)) {
                ENTITY_SOUND_CLASS = Reflection.getClass("{play}.PacketPlayOutEntitySound");
                LIGHT_UPDATE_CLASS = Reflection.getClass("{play}.PacketPlayOutLightUpdate");
                OPEN_BOOK_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenBook");
                OPEN_WINDOW_HORSE_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenWindowHorse");
                OPEN_WINDOW_MERCHANT_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenWindowMerchant");
                VIEW_CENTRE_CLASS = Reflection.getClass("{play}.PacketPlayOutViewCentre");
                VIEW_DISTANCE_CLASS = Reflection.getClass("{play}.PacketPlayOutViewDistance");
            }
            if (protocolVersion.isBetween(NetProtocol.v1_14_4, NetProtocol.v1_18_2)) {
                BLOCK_BREAK_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockBreak");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_17)) {
                CLEAR_TITLES_CLASS = Reflection.getClass("{play}.ClientboundClearTitlesPacket");
                ENTITY_LOOK_CLASS = Reflection.getClass( "{play}.PacketPlayOutEntity$PacketPlayOutEntityLook");
                INITIALIZE_BORDER_CLASS = Reflection.getClass("{play}.ClientboundInitializeBorderPacket");
                PLAYER_COMBAT_END_CLASS = Reflection.getClass("{play}.ClientboundPlayerCombatEndPacket");
                PLAYER_COMBAT_ENTER_CLASS = Reflection.getClass("{play}.ClientboundPlayerCombatEnterPacket");
                PLAYER_COMBAT_KILL_CLASS = Reflection.getClass("{play}.ClientboundPlayerCombatKillPacket");
                REL_ENTITY_MOVE_CLASS = Reflection.getClass( "{play}.PacketPlayOutEntity$PacketPlayOutRelEntityMove");
                REL_ENTITY_MOVE_LOOK_CLASS = Reflection.getClass("{play}.PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook");
                SET_ACTION_BAR_TEXT_CLASS = Reflection.getClass("{play}.ClientboundSetActionBarTextPacket");
                SET_BORDER_CENTER_CLASS = Reflection.getClass("{play}.ClientboundSetBorderCenterPacket");
                SET_BORDER_LERP_SIZE_CLASS = Reflection.getClass("{play}.ClientboundSetBorderLerpSizePacket");
                SET_BORDER_SIZE_CLASS = Reflection.getClass("{play}.ClientboundSetBorderSizePacket");
                SET_BORDER_WARNING_DELAY_CLASS = Reflection.getClass("{play}.ClientboundSetBorderWarningDelayPacket");
                SET_BORDER_WARNING_DISTANCE_CLASS = Reflection.getClass("{play}.ClientboundSetBorderWarningDistancePacket");
                SET_SUBTITLE_TEXT_CLASS = Reflection.getClass("{play}.ClientboundSetSubtitleTextPacket");
                SET_TITLE_TEXT_CLASS = Reflection.getClass("{play}.ClientboundSetTitleTextPacket");
                SET_TITLES_ANIMATION_CLASS = Reflection.getClass("{play}.ClientboundSetTitlesAnimationPacket");
                WORLD_BORDER_CLASS = Reflection.getClass("{play}.ClientboundInitializeBorderPacket");
                if (protocolVersion.isOlderThan(NetProtocol.v1_19)) {
                    ADD_VIBRATION_SIGNAL_CLASS = Reflection.getClass("{play}.ClientboundAddVibrationSignalPacket");
                }
                if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                    PING_CLASS = Reflection.getClass("{play}.ClientboundPingPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_18)) {
                LEVEL_CHUNK_DATA_CLASS = Reflection.getClass("{play}.ClientboundLevelChunkPacketData");
                LEVEL_CHUNK_WITH_LIGHT_CLASS = Reflection.getClass("{play}.ClientboundLevelChunkWithLightPacket");
                SET_SIMULATION_DISTANCE_CLASS = Reflection.getClass("{play}.ClientboundSetSimulationDistancePacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19)) {
                BLOCK_CHANGED_ACK_CLASS = Reflection.getClass("{play}.ClientboundBlockChangedAckPacket");
                PLAYER_CHAT_CLASS = Reflection.getClass("{play}.ClientboundPlayerChatPacket");
                SERVER_DATA_CLASS = Reflection.getClass("{play}.ClientboundServerDataPacket");
                SYSTEM_CHAT_CLASS = Reflection.getClass("{play}.ClientboundSystemChatPacket");
                if (protocolVersion.isOlderThan(NetProtocol.v1_19_3)) {
                    CHAT_PREVIEW_SERVER_CLASS = Reflection.getClass("{play}.ClientboundChatPreviewPacket");
                    SET_DISPLAY_CHAT_PREVIEW_CLASS = Reflection.getClass("{play}.ClientboundSetDisplayChatPreviewPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_1)) {
                CUSTOM_CHAT_COMPLETIONS_CLASS = Reflection.getClass("{play}.ClientboundCustomChatCompletionsPacket");
                DELETE_CHAT_CLASS = Reflection.getClass("{play}.ClientboundDeleteChatPacket");
                if (protocolVersion.isOlderThan(NetProtocol.v1_19_3)) {
                    PLAYER_CHAT_HEADER_CLASS = Reflection.getClass("{play}.ClientboundPlayerChatHeaderPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_3)) {
                DISGUISED_CHAT_CLASS = Reflection.getClass("{play}.ClientboundDisguisedChatPacket");
                PLAYER_INFO_REMOVE_CLASS = Reflection.getClass("{play}.ClientboundPlayerInfoRemovePacket");
                PLAYER_INFO_UPDATE_CLASS = Reflection.getClass("{play}.ClientboundPlayerInfoUpdatePacket");
                if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                    UPDATE_ENABLED_FEATURES_CLASS = Reflection.getClass("{play}.ClientboundUpdateEnabledFeaturesPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_4)) {
                BUNDLE_CLASS = Reflection.getClass("{play}.ClientboundBundlePacket");
                CHUNKS_BIOMES_CLASS = Reflection.getClass("{play}.ClientboundChunksBiomesPacket");
                DAMAGE_EVENT_CLASS = Reflection.getClass("{play}.ClientboundDamageEventPacket");
                HURT_ANIMATION_CLASS = Reflection.getClass("{play}.ClientboundHurtAnimationPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CHUNK_BATCH_FINISHED_CLASS = Reflection.getClass("{play}.ClientboundChunkBatchFinishedPacket");
                CHUNK_BATCH_START_CLASS = Reflection.getClass("{play}.ClientboundChunkBatchStartPacket");
                START_CONFIGURATION_CLASS = Reflection.getClass("{play}.ClientboundStartConfigurationPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_4)) {
                RESET_SCORE_CLASS = Reflection.getClass("{play}.ClientboundResetScorePacket");
                TICKING_STATE_CLASS = Reflection.getClass("{play}.ClientboundTickingStatePacket");
                TICKING_STEP_CLASS = Reflection.getClass("{play}.ClientboundTickingStepPacket");
            }
            if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutCustomPayload");
                KEEP_ALIVE_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutKeepAlive");
                KICK_DISCONNECT_CLASS = Reflection.getClass("{play}.PacketPlayOutKickDisconnect");
                RESOURCE_PACK_SEND_CLASS = Reflection.getClass("{play}.PacketPlayOutResourcePackSend");
                NAMED_ENTITY_SPAWN_CLASS = Reflection.getClass("{play}.PacketPlayOutNamedEntitySpawn");
            }
        }

        public enum Server implements RangedPacketTypeEnum {

            ABILITIES(ABILITIES_SERVER_CLASS, idRegistry.acquire()),
            ADD_VIBRATION_SIGNAL(ADD_VIBRATION_SIGNAL_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_17, NetProtocol.v1_18_2)),
            ADVANCEMENTS(ADVANCEMENTS_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12)),
            ANIMATION(ANIMATION_CLASS, idRegistry.acquire()),
            ATTACH_ENTITY(ATTACH_ENTITY_CLASS, idRegistry.acquire()),
            AUTO_RECIPE(AUTO_RECIPE_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12_1)),
            BED(BED_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_13_2)),
            BLOCK_ACTION(BLOCK_ACTION_CLASS, idRegistry.acquire()),
            BLOCK_BREAK(BLOCK_BREAK_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_14, NetProtocol.v1_18_2)),
            BLOCK_BREAK_ANIMATION(BLOCK_BREAK_ANIMATION_CLASS, idRegistry.acquire()),
            BLOCK_CHANGE(BLOCK_CHANGE_CLASS, idRegistry.acquire()),
            BLOCK_CHANGED_ACK(BLOCK_CHANGED_ACK_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19)),
            BOSS(BOSS_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            BUNDLE(BUNDLE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_4)),
            CAMERA(CAMERA_CLASS, idRegistry.acquire()),
            CHAT_PREVIEW(CHAT_PREVIEW_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            CHUNK_BATCH_FINISHED(CHUNK_BATCH_FINISHED_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            CHUNK_BATCH_START(CHUNK_BATCH_START_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            CHUNKS_BIOMES(CHUNKS_BIOMES_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_4)),
            CLEAR_TITLES(CLEAR_TITLES_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            CLOSE_WINDOW(CLOSE_WINDOW_SERVER_CLASS, idRegistry.acquire()),
            COLLECT(COLLECT_CLASS, idRegistry.acquire()),
            COMBAT_EVENT(COMBAT_EVENT_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_16_5)),
            COMMANDS(COMMANDS_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            CUSTOM_CHAT_COMPLETIONS(CUSTOM_CHAT_COMPLETIONS_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_1)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            DAMAGE_EVENT(DAMAGE_EVENT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_4)),
            DELETE_CHAT(DELETE_CHAT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_1)),
            DISGUISED_CHAT(DISGUISED_CHAT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_3)),
            ENTITY(ENTITY_CLASS, idRegistry.acquire()),
            ENTITY_DESTROY(ENTITY_DESTROY_CLASS, idRegistry.acquire()),
            ENTITY_EFFECT(ENTITY_EFFECT_CLASS, idRegistry.acquire()),
            ENTITY_EQUIPMENT(ENTITY_EQUIPMENT_CLASS, idRegistry.acquire()),
            ENTITY_HEAD_ROTATION(ENTITY_HEAD_ROTATION_CLASS, idRegistry.acquire()),
            ENTITY_LOOK(ENTITY_LOOK_CLASS, idRegistry.acquire()),
            ENTITY_METADATA(ENTITY_METADATA_CLASS, idRegistry.acquire()),
            ENTITY_MOVE(REL_ENTITY_MOVE_CLASS, idRegistry.acquire()),
            ENTITY_MOVE_LOOK(REL_ENTITY_MOVE_LOOK_CLASS, idRegistry.acquire()),
            ENTITY_SOUND(ENTITY_SOUND_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            ENTITY_STATUS(ENTITY_STATUS_CLASS, idRegistry.acquire()),
            ENTITY_TELEPORT(ENTITY_TELEPORT_CLASS, idRegistry.acquire()),
            ENTITY_VELOCITY(ENTITY_VELOCITY_CLASS, idRegistry.acquire()),
            EXPERIENCE(EXPERIENCE_CLASS, idRegistry.acquire()),
            EXPLOSION(EXPLOSION_CLASS, idRegistry.acquire()),
            GAME_STATE_CHANGE(GAME_STATE_CHANGE_CLASS, idRegistry.acquire()),
            HELD_ITEM_SLOT(HELD_ITEM_SLOT_SERVER_CLASS, idRegistry.acquire()),
            HURT_ANIMATION(HURT_ANIMATION_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_4)),
            INITIALIZE_BORDER(INITIALIZE_BORDER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            KICK_DISCONNECT(KICK_DISCONNECT_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            LEVEL_CHUNK_DATA(LEVEL_CHUNK_DATA_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_18)),
            LEVEL_CHUNK_WITH_LIGHT(LEVEL_CHUNK_WITH_LIGHT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_18)),
            LIGHT_UPDATE(LIGHT_UPDATE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            LOGIN(LOGIN_CLASS, idRegistry.acquire()),
            LOOK_AT(LOOK_AT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            MAP(MAP_CLASS, idRegistry.acquire()),
            MAP_CHUNK(MAP_CHUNK_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_17_1)),
            MAP_CHUNK_BULK(MAP_CHUNK_BULK_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_8_8)),
            MOUNT(MOUNT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            MULTI_BLOCK_CHANGE(MULTI_BLOCK_CHANGE_CLASS, idRegistry.acquire()),
            NBT_QUERY(NBT_QUERY_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            NAMED_ENTITY_SPAWN(NAMED_ENTITY_SPAWN_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            NAMED_SOUND_EFFECT(NAMED_SOUND_EFFECT_CLASS, idRegistry.acquire()),
            OPEN_BOOK(OPEN_BOOK_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_SIGN_EDITOR(OPEN_SIGN_EDITOR_CLASS, idRegistry.acquire()),
            OPEN_WINDOW(OPEN_WINDOW_CLASS, idRegistry.acquire()),
            OPEN_WINDOW_HORSE(OPEN_WINDOW_HORSE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_WINDOW_MERCHANT(OPEN_WINDOW_MERCHANT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            PING(PING_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_17, NetProtocol.v1_20_1)),
            PLAYER_CHAT(PLAYER_CHAT_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_18_2)),
            PLAYER_CHAT_HEADER(PLAYER_CHAT_HEADER_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_19_1, NetProtocol.v1_19_2)),
            PLAYER_COMBAT_END(PLAYER_COMBAT_END_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_COMBAT_ENTER(PLAYER_COMBAT_ENTER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_COMBAT_KILL(PLAYER_COMBAT_KILL_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_INFO(PLAYER_INFO_CLASS, idRegistry.acquire()),
            PLAYER_INFO_REMOVE(PLAYER_INFO_REMOVE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_3)),
            PLAYER_INFO_UPDATE(PLAYER_INFO_UPDATE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19_3)),
            PLAYERLIST_HEADER_FOOTER(PLAYER_LIST_HEADER_FOOTER_CLASS, idRegistry.acquire()),
            POSITION(POSITION_SERVER_CLASS, idRegistry.acquire()),
            RECIPE_UPDATE(RECIPE_UPDATE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            RECIPES(RECIPES_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12)),
            REMOVE_ENTITY_EFFECT(REMOVE_ENTITY_EFFECT_CLASS, idRegistry.acquire()),
            RESET_SCORE(RESET_SCORE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_CLASS, idRegistry.acquire(), NetProtocolRange.before(NetProtocol.v1_20_2)),
            RESPAWN(RESPAWN_CLASS, idRegistry.acquire()),
            SCOREBOARD_DISPLAY_OBJECTIVE(SCOREBOARD_DISPLAY_OBJECTIVE_CLASS, idRegistry.acquire()),
            SCOREBOARD_OBJECTIVE(SCOREBOARD_OBJECTIVE_CLASS, idRegistry.acquire()),
            SCOREBOARD_SCORE(SCOREBOARD_SCORE_CLASS, idRegistry.acquire()),
            SCOREBOARD_TEAM(SCOREBOARD_TEAM_CLASS, idRegistry.acquire()),
            SELECT_ADVANCEMENT_TAB(SELECT_ADVANCEMENT_TAB_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_12)),
            SERVER_DATA(SERVER_DATA_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19)),
            SERVER_DIFFICULTY(SERVER_DIFFICULTY_CLASS, idRegistry.acquire()),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_8_8)),
            SET_ACTIONBAR_TEXT(SET_ACTION_BAR_TEXT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_CENTER(SET_BORDER_CENTER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_LERP_SIZE(SET_BORDER_LERP_SIZE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_SIZE(SET_BORDER_SIZE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_WARNING_DELAY(SET_BORDER_WARNING_DELAY_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_WARNING_DISTANCE(SET_BORDER_WARNING_DISTANCE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_SIMULATION_DISTANCE(SET_SIMULATION_DISTANCE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_18)),
            SET_SUBTITLE_TEXT(SET_SUBTITLE_TEXT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_COOLDOWN(SET_COOLDOWN_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            SET_DISPLAY_CHAT_PREVIEW(SET_DISPLAY_CHAT_PREVIEW_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            SET_TITLE_TEXT(SET_TITLE_TEXT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_TITLES_ANIMATION(SET_TITLES_ANIMATION_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_17)),
            SET_SLOT(SET_SLOT_CLASS, idRegistry.acquire()),
            SPAWN_ENTITY(SPAWN_ENTITY_CLASS, idRegistry.acquire()),
            SPAWN_ENTITY_EXPERIENCE_ORB(SPAWN_ENTITY_EXPERIENCE_ORB_CLASS, idRegistry.acquire()),
            SPAWN_ENTITY_LIVING(SPAWN_ENTITY_LIVING_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_18_2)),
            SPAWN_ENTITY_PAINTING(SPAWN_ENTITY_PAINTING_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_18_2)),
            SPAWN_ENTITY_WEATHER(SPAWN_ENTITY_WEATHER_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_15_2)),
            SPAWN_POSITION(SPAWN_POSITION_CLASS, idRegistry.acquire()),
            SYSTEM_CHAT(SYSTEM_CHAT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_19)),
            START_CONFIGURATION(START_CONFIGURATION_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            STATISTIC(STATISTIC_CLASS, idRegistry.acquire()),
            STOP_SOUND(STOP_SOUND_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_SERVER_CLASS, idRegistry.acquire()),
            TAGS(TAGS_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_13, NetProtocol.v1_20_1)),
            TICKING_STATE(TICKING_STATE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_4)),
            TICKING_STEP(TICKING_STEP_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_4)),
            TILE_ENTITY_DATA(TILE_ENTITY_DATA_CLASS, idRegistry.acquire()),
            TITLE(TITLE_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_16_5)),
            TRANSACTION(TRANSACTION_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_16_5)),
            UNLOAD_CHUNK(UNLOAD_CHUNK_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            UPDATE_ATTRIBUTES(UPDATE_ATTRIBUTES_CLASS, idRegistry.acquire()),
            UPDATE_ENABLED_FEATURES(UPDATE_ENABLED_FEATURES_CLASS, idRegistry.acquire(), NetProtocolRange.between(NetProtocol.v1_19_3, NetProtocol.v1_20_1)),
            UPDATE_ENTITY_NBT(UPDATE_ENTITY_NBT_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_8_8)),
            UPDATE_HEALTH(UPDATE_HEALTH_CLASS, idRegistry.acquire()),
            UPDATE_SIGN(UPDATE_SIGN_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.until(NetProtocol.v1_9_2)),
            UPDATE_TIME(UPDATE_TIME_CLASS, idRegistry.acquire()),
            VEHICLE_MOVE(VEHICLE_MOVE_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_9)),
            VIEW_CENTRE(VIEW_CENTRE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            VIEW_DISTANCE(VIEW_DISTANCE_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_14)),
            WINDOW_DATA(WINDOW_DATA_CLASS, idRegistry.acquire()),
            WINDOW_ITEMS(WINDOW_ITEMS_CLASS, idRegistry.acquire()),
            WORLD_BORDER(WORLD_BORDER_CLASS, idRegistry.acquire()),
            WORLD_EVENT(WORLD_EVENT_CLASS, idRegistry.acquire()),
            WORLD_PARTICLES(WORLD_PARTICLES_CLASS, idRegistry.acquire())
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsClass;
            private final int packetId;
            private final NetProtocolRange protocolRange;

            Server(Class<?> nmsClass, int packetId) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = NetProtocolRange.ALL;
            }

            Server(Class<?> nmsClass, int packetId, NetProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsClass;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public PacketState getState() {
                return PacketState.PLAY;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.CLIENT;
            }

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }
        }
    }

    public static class Configuration {

        private static Class<?> FINISH_CONFIGURATION_CLIENT_CLASS;

        static {
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                FINISH_CONFIGURATION_CLIENT_CLASS = Reflection.getClass("{configuration}.ServerboundFinishConfigurationPacket");
            }
        }

        public enum Client implements RangedPacketTypeEnum {
            FINISH_CONFIGURATION(FINISH_CONFIGURATION_CLIENT_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2))
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;
            private final NetProtocolRange protocolRange;

            Client(Class<?> nmsPacket, int packetId, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.SERVER;
            }

            @Override
            public PacketState getState() {
                return PacketState.CONFIGURATION;
            }
        }

        private static Class<?> FINISH_CONFIGURATION_SERVER_CLASS;
        private static Class<?> REGISTRY_DATA_CLASS;
        private static Class<?> UPDATE_ENABLED_FEATURES_CLASS;

        static {
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                FINISH_CONFIGURATION_SERVER_CLASS = Reflection.getClass("{configuration}.ClientboundFinishConfigurationPacket");
                REGISTRY_DATA_CLASS = Reflection.getClass("{configuration}.ClientboundRegistryDataPacket");
                UPDATE_ENABLED_FEATURES_CLASS = Reflection.getClass("{configuration}.ClientboundUpdateEnabledFeaturesPacket");
            }
        }

        public enum Server implements RangedPacketTypeEnum {
            FINISH_CONFIGURATION(FINISH_CONFIGURATION_SERVER_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            REGISTRY_DATA(REGISTRY_DATA_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            UPDATE_ENABLED_FEATURES(UPDATE_ENABLED_FEATURES_CLASS, idRegistry.acquire(), NetProtocolRange.since(NetProtocol.v1_20_2)),
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final Class<?> nmsPacket;
            private final int packetId;
            private final NetProtocolRange protocolRange;

            Server(Class<?> nmsPacket, int packetId, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            @Override
            public int getHoneyID() {
                return packetId;
            }

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.CLIENT;
            }

            @Override
            public PacketState getState() {
                return PacketState.CONFIGURATION;
            }
        }
    }
}
