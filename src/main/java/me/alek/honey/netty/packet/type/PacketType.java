package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.netty.utility.NetProtocolRange;
import me.alek.honey.netty.utility.Reflection;

import java.util.*;

public enum PacketType implements PacketTypeEnum {
    UNKNOWN;

    @Override
    public Class<?> getNmsClass() {
        return null;
    }

    @Override
    public int getPacketId() {
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

    private static class PacketDetails {
        int packetId;
        PacketTypeEnum packetType;
    }

    private static void addPacket(Class<?> clazz, int id, PacketTypeEnum type) {
        PACKET_STATE_TYPE.get(type.getState()).add(type);

        if (type instanceof RangedPacketTypeEnum) {
            if (!NetProtocol.matchProtocol((RangedPacketTypeEnum) type)) {
                return;
            }
        }
        PACKET_DETAILS.put(clazz, new PacketDetails(){{
            this.packetId = id;
            this.packetType = type;
        }});
    }

    private static final NetProtocol PROTOCOL_VERSION = NetProtocol.getProtocol();
    private static final Map<Class<?>, PacketDetails> PACKET_DETAILS = new IdentityHashMap<>();
    private static final Map<PacketState, List<PacketTypeEnum>> PACKET_STATE_TYPE = new HashMap<>();

    public static void load() {
        for (PacketState state : PacketState.values()) {
            PACKET_STATE_TYPE.put(state, new ArrayList<>());
        }
        for (Handshake.Client handshakeClient : Handshake.Client.values()) {
            addPacket(handshakeClient.getNmsClass(), handshakeClient.getPacketId(), handshakeClient);
        }
        for (Login.Client loginClient : Login.Client.values()) {
            addPacket(loginClient.getNmsClass(), loginClient.getPacketId(), loginClient);
        }
        for (Login.Server loginServer : Login.Server.values()) {
            addPacket(loginServer.getNmsClass(), loginServer.getPacketId(), loginServer);
        }
        for (Status.Client statusClient : Status.Client.values()) {
            addPacket(statusClient.getNmsClass(), statusClient.getPacketId(), statusClient);
        }
        for (Status.Server statusServer : Status.Server.values()) {
            addPacket(statusServer.getNmsClass(), statusServer.getPacketId(), statusServer);
        }
        for (Play.Client playClient : Play.Client.values()) {
            addPacket(playClient.getNmsClass(), playClient.getPacketId(), playClient);
        }
        for (Play.Server playServer : Play.Server.values()) {
            addPacket(playServer.getNmsClass(), playServer.getPacketId(), playServer);
        }
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(NetProtocol protocol, PacketState state, PacketBound packetBound) {
        if (state == PacketState.UNKNOWN) {
            return new ArrayList<>();
        }
        final ArrayList<PacketTypeEnum> packetTypes = new ArrayList<>();
        for (PacketTypeEnum packetType : PACKET_STATE_TYPE.get(state)) {

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
        return getTypesAvailableFor(NetProtocol.getProtocol());
    }

    public static Map<Class<?>, PacketDetails> getPacketDetails() {
        return PACKET_DETAILS;
    }

    public static PacketTypeEnum getPacketType(Class<?> clazz) {
        if (!PACKET_DETAILS.containsKey(clazz)) {
            return UNKNOWN;
        }
        return PACKET_DETAILS.get(clazz).packetType;
    }

    public static class Handshake {

        private static final Class<?> SET_PROTOCOL_CLASS = Reflection.getClass("{nms}.PacketHandshakingInSetProtocol");

        public enum Client implements PacketTypeEnum {

            SET_PROTOCOL(SET_PROTOCOL_CLASS, 0);

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
            public int getPacketId() {
                return packetId;
            }

            @Override
            public PacketBound getBound() {
                return PacketBound.CLIENT;
            }

            @Override
            public PacketState getState() {
                return PacketState.HANDSHAKE;
            }
        }
    }

    public static class Login {


        // client
        private static final Class<?> ENCRYPTION_RESPONSE_CLASS = Reflection.getClass("{nms}.PacketLoginInEncryptionBegin");
        private static final Class<?> LOGIN_START_CLASS = Reflection.getClass("{nms}.PacketLoginInStart");
        private static Class<?> CUSTOM_PAYLOAD_CLASS;


        public enum Client implements RangedPacketTypeEnum {
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, 151, NetProtocolRange.betweenBefore(NetProtocol.v1_13, NetProtocol.v1_20_2)),
            ENCRYPTION_RESPONSE(ENCRYPTION_RESPONSE_CLASS, 152),
            LOGIN_START(LOGIN_START_CLASS, 153);

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
            public int getPacketId() {
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

        private static final Class<?> DISCONNECT_CLASS = Reflection.getClass("{nms}.PacketLoginOutDisconnect");
        private static final Class<?> ENCRYPTION_REQUEST_CLASS = Reflection.getClass("{nms}.PacketLoginOutEncryptionBegin");
        private static final Class<?> LOGIN_SUCCESS_CLASS = Reflection.getClass("{nms}.PacketLoginOutSuccess");
        private static final Class<?> SET_COMPRESSION_CLASS = Reflection.getClass("{nms}.PacketLoginOutSetCompression");
        private static Class<?> SERVERBOUND_CUSTOM_PAYLOAD_CLASS;

        static {
            if (PROTOCOL_VERSION.isBetween(NetProtocol.v1_13, NetProtocol.v1_20_1)) {
                SERVERBOUND_CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{nms}.PacketLoginOutCustomPayload");
                CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{nms}.PacketLoginInCustomPayload");
            }
        }

        public enum Server implements RangedPacketTypeEnum {
            DISCONNECT(DISCONNECT_CLASS, 154),
            ENCRYPTION_REQUEST(ENCRYPTION_REQUEST_CLASS, 155),
            LOGIN_SUCCESS(LOGIN_SUCCESS_CLASS, 156),
            CUSTOM_PAYLOAD(SERVERBOUND_CUSTOM_PAYLOAD_CLASS, 157, NetProtocolRange.betweenBefore(NetProtocol.v1_13, NetProtocol.v1_20_2)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, 158);

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
            public int getPacketId() {
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
    }

    public static class Status {

        private static final Class<?> PING_CLASS = Reflection.getClass("{nms}.PacketStatusInPing");
        private static final Class<?> START_CLASS = Reflection.getClass("{nms}.PacketStatusInStart");

        public enum Client implements PacketTypeEnum {

            PING_REQUEST(PING_CLASS,1),
            STATUS_REQUEST(START_CLASS, 2);

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
            public int getPacketId() {
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

        private static final Class<?> PONG_CLASS = Reflection.getClass("{nms}.PacketStatusOutPong");
        private static final Class<?> SERVER_INFO_CLASS = Reflection.getClass("{nms}.PacketStatusOutServerInfo");

        public enum Server implements PacketTypeEnum {

            PING_RESPONSE(PONG_CLASS, 3),
            STATUS_RESPONSE(SERVER_INFO_CLASS, 4);

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
            public int getPacketId() {
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
    }

    public static class Play {

        // client
        private static final Class<?> ABILITIES_CLASS = Reflection.getClass("{nms}.PacketPlayInAbilities");
        private static final Class<?> ARM_ANIMATION_CLASS = Reflection.getClass("{nms}.PacketPlayInArmAnimation");
        private static final Class<?> BLOCK_DIG_CLASS = Reflection.getClass("{nms}.PacketPlayInBlockDig");
        private static final Class<?> BLOCK_PLACE_CLASS = Reflection.getClass("{nms}.PacketPlayInBlockPlace");
        private static final Class<?> CHAT_CLASS = Reflection.getClass("{nms}.PacketPlayInChat");
        private static final Class<?> CLIENT_COMMAND_CLASS = Reflection.getClass("{nms}.PacketPlayInClientCommand");
        private static final Class<?> CLOSE_WINDOW_CLASS = Reflection.getClass("{nms}.PacketPlayInCloseWindow");
        private static final Class<?> ENCHANT_ITEM_CLASS = Reflection.getClass("{nms}.PacketPlayInEnchantItem");
        private static final Class<?> ENTITY_ACTION_CLASS = Reflection.getClass("{nms}.PacketPlayInEntityAction");
        private static final Class<?> HELD_ITEM_SLOT_CLASS = Reflection.getClass("{nms}.PacketPlayInHeldItemSlot");
        private static final Class<?> SPECTATE_CLASS = Reflection.getClass("{nms}.PacketPlayInSpectate");
        private static final Class<?> STEER_VEHICLE_CLASS = Reflection.getClass("{nms}.PacketPlayInSteerVehicle");
        private static final Class<?> SET_CREATIVE_SLOT_CLASS = Reflection.getClass("{nms}.PacketPlayInSetCreativeSlot");
        private static final Class<?> TAB_COMPLETE_CLASS = Reflection.getClass("{nms}.PacketPlayInTabComplete");
        private static final Class<?> UPDATE_SIGN_CLASS = Reflection.getClass("{nms}.PacketPlayInUpdateSign");
        private static final Class<?> USE_ENTITY_CLASS = Reflection.getClass("{nms}.PacketPlayInUseEntity");
        private static final Class<?> WINDOW_CLICK_CLASS = Reflection.getClass("{nms}.PacketPlayInWindowClick");
        private static Class<?> ADVANCEMENTS_CLASS;
        private static Class<?> AUTO_RECIPE_CLASS;
        private static Class<?> B_EDIT_CLASS;
        private static Class<?> BEACON_CLASS;
        private static Class<?> BOAT_MOVE_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_CLASS;
        private static Class<?> DIFFICULTY_CHANGE_CLASS;
        private static Class<?> DIFFICULTY_LOCK_CLASS;
        private static Class<?> ENTITY_NBT_QUERY_CLASS;
        private static Class<?> FLYING_CLASS;
        private static Class<?> GROUND_CLASS;
        private static Class<?> ITEM_NAME_CLASS;
        private static Class<?> JIGSAW_GENERATE_CLASS;
        private static Class<?> KEEP_ALIVE_CLASS;
        private static Class<?> LOOK_CLASS;
        private static Class<?> POSITION_CLASS;
        private static Class<?> POSITION_LOOK_CLASS;
        private static Class<?> PICK_ITEM_CLASS;
        private static Class<?> RECIPE_DISPLAYED_CLASS;
        private static Class<?> RECIPE_SETTINGS_CLASS;
        private static Class<?> RESOURCE_PACK_STATUS_CLASS;
        private static Class<?> SET_COMMAND_BLOCK_CLASS;
        private static Class<?> SET_COMMAND_MINECART_CLASS;
        private static Class<?> SET_JIGSAW_CLASS;
        private static Class<?> SETTINGS_CLASS;
        private static Class<?> STRUCT_CLASS;
        private static Class<?> TELEPORT_ACCEPT_CLASS;
        private static Class<?> TILE_NBT_QUERY_CLASAS;
        private static Class<?> TR_SEL_CLASS;
        private static Class<?> TRANSACTION_CLASS;
        private static Class<?> USE_ITEM_CLASS;
        private static Class<?> VEHICLE_MOVE_CLASS;

        static {
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                TRANSACTION_CLASS = Reflection.getClass("{nms}.PacketPlayInTransaction");
                FLYING_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying");
                POSITION_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInPosition");
                POSITION_LOOK_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInPositionLook");
                LOOK_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInLook");
            }

            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_9)) {
                VEHICLE_MOVE_CLASS = Reflection.getClass("{nms}.PacketPlayInVehicleMove");
                BOAT_MOVE_CLASS = Reflection.getClass("{nms}.PacketPlayInBoatMove");
                USE_ITEM_CLASS = Reflection.getClass("{nms}.PacketPlayInUseItem");
                TELEPORT_ACCEPT_CLASS = Reflection.getClass("{nms}.PacketPlayInTeleportAccept");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_12)) {
                AUTO_RECIPE_CLASS = Reflection.getClass("{nms}.PacketPlayInAutoRecipe");
                RECIPE_DISPLAYED_CLASS = Reflection.getClass("{nms}.PacketPlayInRecipeDisplayed");
                ADVANCEMENTS_CLASS = Reflection.getClass("{nms}.PacketPlayInAdvancements");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_13)) {
                TILE_NBT_QUERY_CLASAS = Reflection.getClass("{nms}.PacketPlayInTileNBTQuery");
                ENTITY_NBT_QUERY_CLASS = Reflection.getClass("{nms}.PacketPlayInEntityNBTQuery");
                B_EDIT_CLASS = Reflection.getClass("{nms}.PacketPlayInBEdit");
                PICK_ITEM_CLASS = Reflection.getClass("{nms}.PacketPlayInPickItem");
                ITEM_NAME_CLASS = Reflection.getClass("{nms}.PacketPlayInItemName");
                TR_SEL_CLASS = Reflection.getClass("{nms}.PacketPlayInTrSel");
                BEACON_CLASS = Reflection.getClass("{nms}.PacketPlayInBeacon");
                SET_COMMAND_BLOCK_CLASS = Reflection.getClass("{nms}.PacketPlayInSetCommandBlock");
                SET_COMMAND_MINECART_CLASS = Reflection.getClass("{nms}.PacketPlayInSetCommandMinecart");
                STRUCT_CLASS = Reflection.getClass("{nms}.PacketPlayInStruct");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_14)) {
                DIFFICULTY_CHANGE_CLASS = Reflection.getClass("{nms}.PacketPlayInDifficultyChange");
                DIFFICULTY_LOCK_CLASS = Reflection.getClass("{nms}.PacketPlayInDifficultyLock");
                SET_JIGSAW_CLASS = Reflection.getClass("{nms}.PacketPlayInSetJigsaw");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_16_1)) {
                JIGSAW_GENERATE_CLASS = Reflection.getClass("{nms}.PacketPlayInJigsawGenerate");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_16_2)) {
                RECIPE_SETTINGS_CLASS = Reflection.getClass("{nms}.PacketPlayInRecipeSettings");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_17)) {
                POSITION_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$PacketPlayInPosition");
                POSITION_LOOK_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$PacketPlayInPositionLook");
                LOOK_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$PacketPlayInLook");
                GROUND_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$d");
            }
            if (PROTOCOL_VERSION.isOlderThan(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{nms}.PacketPlayInCustomPayload");
                KEEP_ALIVE_CLASS = Reflection.getClass("{nms}.PacketPlayInKeepAlive");
                RESOURCE_PACK_STATUS_CLASS = Reflection.getClass("{nms}.PacketPlayInResourcePackStatus");
                SETTINGS_CLASS = Reflection.getClass("{nms}.PacketPlayInSettings");
            }
        }

        public enum Client implements RangedPacketTypeEnum {

            ABILITIES(ABILITIES_CLASS, 5),
            ADVANCEMENTS(ADVANCEMENTS_CLASS, 6, NetProtocolRange.since(NetProtocol.v1_12)),
            ARM_ANIMATION(ARM_ANIMATION_CLASS, 7),
            AUTO_RECIPE(AUTO_RECIPE_CLASS, 8, NetProtocolRange.since(NetProtocol.v1_12)),
            B_EDIT(B_EDIT_CLASS, 9, NetProtocolRange.since(NetProtocol.v1_13)),
            BEACON(BEACON_CLASS, 10, NetProtocolRange.since(NetProtocol.v1_13)),
            BLOCK_DIG(BLOCK_DIG_CLASS, 11),
            BLOCK_PLACE(BLOCK_PLACE_CLASS, 12),
            BOAT_MOVE(BOAT_MOVE_CLASS, 13, NetProtocolRange.since(NetProtocol.v1_9)),
            CHAT(CHAT_CLASS, 14),
            CLIENT_COMMAND(CLIENT_COMMAND_CLASS, 15),
            CLOSE_WINDOW(CLOSE_WINDOW_CLASS, 16),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, 17, NetProtocolRange.before(NetProtocol.v1_20_2)),
            DIFFICULTY_CHANGE(DIFFICULTY_CHANGE_CLASS, 18, NetProtocolRange.since(NetProtocol.v1_14)),
            DIFFICULTY_LOCK(DIFFICULTY_LOCK_CLASS, 19, NetProtocolRange.since(NetProtocol.v1_14)),
            ENCHANT_ITEM(ENCHANT_ITEM_CLASS, 20),
            ENTITY_ACTION(ENTITY_ACTION_CLASS, 21),
            ENTITY_NBT_QUERY(ENTITY_NBT_QUERY_CLASS, 22, NetProtocolRange.since(NetProtocol.v1_13)),
            FLYING(FLYING_CLASS, 23, NetProtocolRange.until(NetProtocol.v1_16_5)),
            GROUND(GROUND_CLASS, 24, NetProtocolRange.since(NetProtocol.v1_17)),
            HELD_ITEM_SLOT(HELD_ITEM_SLOT_CLASS, 25),
            ITEM_NAME(ITEM_NAME_CLASS, 26, NetProtocolRange.since(NetProtocol.v1_13)),
            JIGSAW_GENERATE(JIGSAW_GENERATE_CLASS, 27, NetProtocolRange.since(NetProtocol.v1_16_1)),
            KEEP_ALIVE(KEEP_ALIVE_CLASS, 28, NetProtocolRange.before(NetProtocol.v1_20_2)),
            LOOK(LOOK_CLASS, 29),
            PICK_ITEM(PICK_ITEM_CLASS, 30, NetProtocolRange.since(NetProtocol.v1_13)),
            POSITION(POSITION_CLASS, 31),
            POSITION_LOOK(POSITION_LOOK_CLASS, 32),
            RECIPE_DISPLAYED(RECIPE_DISPLAYED_CLASS, 33, NetProtocolRange.since(NetProtocol.v1_12)),
            RECIPE_SETTINGS(RECIPE_SETTINGS_CLASS, 34, NetProtocolRange.since(NetProtocol.v1_16_2)),
            RESOURCE_PACK_STATUS(RESOURCE_PACK_STATUS_CLASS, 35, NetProtocolRange.before(NetProtocol.v1_20_2)),
            SET_COMMAND_BLOCK(SET_COMMAND_BLOCK_CLASS, 36, NetProtocolRange.since(NetProtocol.v1_13)),
            SET_COMMAND_MINECART(SET_COMMAND_MINECART_CLASS, 37, NetProtocolRange.since(NetProtocol.v1_13)),
            SET_CREATIVE_SLOT(SET_CREATIVE_SLOT_CLASS, 38),
            SET_JIGSAW(SET_JIGSAW_CLASS, 39, NetProtocolRange.since(NetProtocol.v1_14)),
            SETTINGS(SETTINGS_CLASS, 40, NetProtocolRange.before(NetProtocol.v1_20_2)),
            SPECTATE(SPECTATE_CLASS, 41),
            STEER_VEHICLE(STEER_VEHICLE_CLASS, 42),
            STRUCT(STRUCT_CLASS, 43, NetProtocolRange.since(NetProtocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_CLASS, 44),
            TELEPORT_ACCEPT(TELEPORT_ACCEPT_CLASS, 45, NetProtocolRange.since(NetProtocol.v1_9)),
            TILE_NBT_QUERY(TILE_NBT_QUERY_CLASAS, 46, NetProtocolRange.since(NetProtocol.v1_13)),
            TR_SEL(TR_SEL_CLASS, 47, NetProtocolRange.since(NetProtocol.v1_13)),
            TRANSACTION(TRANSACTION_CLASS, 48, NetProtocolRange.until(NetProtocol.v1_16_5)),
            UPDATE_SIGN(UPDATE_SIGN_CLASS, 49),
            USE_ENTITY(USE_ENTITY_CLASS, 50),
            USE_ITEM(USE_ITEM_CLASS, 51, NetProtocolRange.since(NetProtocol.v1_9)),
            VEHICLE_MOVE(VEHICLE_MOVE_CLASS, 52, NetProtocolRange.since(NetProtocol.v1_9)),
            WINDOW_CLICK(WINDOW_CLICK_CLASS, 53),
            ;

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
            public int getPacketId() {
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

        private static final Class<?> ABILITIES_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutAbilities");
        private static final Class<?> ANIMATION_CLASS = Reflection.getClass("{nms}.PacketPlayOutAnimation");
        private static final Class<?> ATTACH_ENTITY_CLASS = Reflection.getClass("{nms}.PacketPlayOutAttachEntity");
        private static final Class<?> BLOCK_ACTION_CLASS = Reflection.getClass("{nms}.PacketPlayOutBlockAction");
        private static final Class<?> BLOCK_BREAK_ANIMATION_CLASS = Reflection.getClass("{nms}.PacketPlayOutBlockBreakAnimation");
        private static final Class<?> BLOCK_CHANGE_CLASS = Reflection.getClass("{nms}.PacketPlayOutBlockChange");
        private static final Class<?> CAMERA_CLASS = Reflection.getClass("{nms}.PacketPlayOutCamera");
        private static final Class<?> CLOSE_WINDOW_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutCloseWindow");
        private static final Class<?> COLLECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutCollect");
        private static final Class<?> ENTITY_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntity");
        private static final Class<?> ENTITY_DESTROY_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityDestroy");
        private static final Class<?> ENTITY_EFFECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityEffect");
        private static final Class<?> ENTITY_EQUIPMENT_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityEquipment");
        private static final Class<?> ENTITY_HEAD_ROTATION_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityHeadRotation");
        private static final Class<?> ENTITY_METADATA_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityMetadata");
        private static final Class<?> ENTITY_STATUS_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityStatus");
        private static final Class<?> ENTITY_TELEPORT_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityTeleport");
        private static final Class<?> ENTITY_VELOCITY_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntityVelocity");
        private static final Class<?> EXPERIENCE_CLASS = Reflection.getClass("{nms}.PacketPlayOutExperience");
        private static final Class<?> EXPLOSION_CLASS = Reflection.getClass("{nms}.PacketPlayOutExplosion");
        private static final Class<?> GAME_STATE_CHANGE_CLASS = Reflection.getClass("{nms}.PacketPlayOutGameStateChange");
        private static final Class<?> HELD_ITEM_SLOT_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutHeldItemSlot");
        private static final Class<?> LOGIN_CLASS = Reflection.getClass("{nms}.PacketPlayOutLogin");
        private static final Class<?> MAP_CLASS = Reflection.getClass("{nms}.PacketPlayOutMap");
        private static final Class<?> MULTI_BLOCK_CHANGE_CLASS = Reflection.getClass("{nms}.PacketPlayOutMultiBlockChange");
        private static final Class<?> NAMED_SOUND_EFFECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutNamedSoundEffect");
        private static final Class<?> OPEN_SIGN_EDITOR_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenSignEditor");
        private static final Class<?> OPEN_WINDOW_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenWindow");
        private static final Class<?> PLAYER_INFO_CLASS = Reflection.getFuzzyClass("{nms}.PacketPlayOutPlayerInfo",
                "{nms}.ClientboundPlayerInfoUpdatePacket");
        private static final Class<?> PLAYER_LIST_HEADER_FOOTER_CLASS = Reflection.getClass("{nms}.PacketPlayOutPlayerListHeaderFooter");
        private static final Class<?> POSITION_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutPosition");
        private static final Class<?> REMOVE_ENTITY_EFFECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutRemoveEntityEffect");
        private static final Class<?> RESPAWN_CLASS = Reflection.getClass("{nms}.PacketPlayOutRespawn");
        private static final Class<?> SCOREBOARD_DISPLAY_OBJECTIVE_CLASS = Reflection.getClass("{nms}.PacketPlayOutScoreboardDisplayObjective");
        private static final Class<?> SCOREBOARD_OBJECTIVE_CLASS = Reflection.getClass("{nms}.PacketPlayOutScoreboardObjective");
        private static final Class<?> SCOREBOARD_SCORE_CLASS = Reflection.getClass("{nms}.PacketPlayOutScoreboardScore");
        private static final Class<?> SCOREBOARD_TEAM_CLASS = Reflection.getClass("{nms}.PacketPlayOutScoreboardTeam");
        private static final Class<?> SERVER_DIFFICULTY_CLASS = Reflection.getClass("{nms}.PacketPlayOutServerDifficulty");
        private static final Class<?> SET_SLOT_CLASS = Reflection.getClass("{nms}.PacketPlayOutSetSlot");
        private static final Class<?> SPAWN_ENTITY_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntity");
        private static final Class<?> SPAWN_ENTITY_EXPERIENCE_ORB_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityExperienceOrb");
        private static final Class<?> SPAWN_POSITION_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnPosition");
        private static final Class<?> STATISTIC_CLASS = Reflection.getClass("{nms}.PacketPlayOutStatistic");
        private static final Class<?> TAB_COMPLETE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutTabComplete");
        private static final Class<?> TILE_ENTITY_DATA_CLASS = Reflection.getClass("{nms}.PacketPlayOutTileEntityData");
        private static final Class<?> UPDATE_ATTRIBUTES_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateAttributes");
        private static final Class<?> UPDATE_HEALTH_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateHealth");
        private static final Class<?> UPDATE_TIME_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateTime");
        private static final Class<?> WINDOW_DATA_CLASS = Reflection.getClass("{nms}.PacketPlayOutWindowData");
        private static final Class<?> WINDOW_ITEMS_CLASS = Reflection.getClass("{nms}.PacketPlayOutWindowItems");
        private static final Class<?> WORLD_EVENT_CLASS = Reflection.getClass("{nms}.PacketPlayOutWorldEvent");
        private static final Class<?> WORLD_PARTICLES_CLASS = Reflection.getClass("{nms}.PacketPlayOutWorldParticles");
        private static Class<?> ADVANCEMENTS_SERVER_CLASS;
        private static Class<?> AUTO_RECIPE_SERVER_CLASS;
        private static Class<?> BED_CLASS;
        private static Class<?> BLOCK_BREAK_CLASS;
        private static Class<?> BOSS_CLASS;
        private static Class<?> CHAT_SERVER_CLASS;
        private static Class<?> COMBAT_EVENT_CLASS;
        private static Class<?> COMMANDS_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_SERVER_CLASS;
        private static Class<?> ENTITY_LOOK_CLASS;
        private static Class<?> ENTITY_SOUND_CLASS;
        private static Class<?> KEEP_ALIVE_SERVER_CLASS;
        private static Class<?> KICK_DISCONNECT_CLASS;
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
        private static Class<?> RECIPE_UPDATE_CLASS;
        private static Class<?> RECIPES_CLASS;
        private static Class<?> REL_ENTITY_MOVE_LOOK_CLASS;
        private static Class<?> REL_ENTITY_MOVE_CLASS;
        private static Class<?> RESOURCE_PACK_SEND_CLASS;
        private static Class<?> SELECT_ADVANCEMENT_TAB_CLASS;
        private static Class<?> SPAWN_ENTITY_LIVING_CLASS;
        private static Class<?> SPAWN_ENTITY_PAINTING_CLASS;
        private static Class<?> SPAWN_ENTITY_WEATHER_CLASS;
        private static Class<?> SET_COMPRESSION_CLASS;
        private static Class<?> SET_COOLDOWN_CLASS;
        private static Class<?> STOP_SOUND_CLASS;
        private static Class<?> TAGS_CLASS;
        private static Class<?> TRANSACTION_SERVER_CLASS;
        private static Class<?> TITLE_CLASS;
        private static Class<?> UNLOAD_CHUNK_CLASS;
        private static Class<?> UPDATE_ENTITY_NBT_CLASS;
        private static Class<?> UPDATE_SIGN_SERVER_CLASS;
        private static Class<?> VEHICLE_MOVE_SERVER_CLASS;
        private static Class<?> VIEW_CENTRE_CLASS;
        private static Class<?> VIEW_DISTANCE_CLASS;
        private static Class<?> WORLD_BORDER_CLASS;

        static {
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                MAP_CHUNK_BULK_CLASS = Reflection.getClass("{nms}.PacketPlayOutMapChunkBulk");
                UPDATE_ENTITY_NBT_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateEntityNBT");
                SET_COMPRESSION_CLASS = Reflection.getClass("{nms}.PacketPlayOutSetCompression");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_9_2)) {
                UPDATE_SIGN_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateSign");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_13_2)) {
                BED_CLASS = Reflection.getClass("{nms}.PacketPlayOutBed");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_15_2)) {
                SPAWN_ENTITY_WEATHER_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityWeather");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                COMBAT_EVENT_CLASS = Reflection.getClass("{nms}.PacketPlayOutCombatEvent");
                ENTITY_LOOK_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutEntityLook");
                REL_ENTITY_MOVE_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutRelEntityMove");
                REL_ENTITY_MOVE_LOOK_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutRelEntityMoveLook");
                TRANSACTION_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutTransaction");
                TITLE_CLASS = Reflection.getClass("{nms}.PacketPlayOutTitle");
                WORLD_BORDER_CLASS = Reflection.getClass("{nms}.PacketPlayOutWorldBorder");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_17_1)) {
                MAP_CHUNK_CLASS = Reflection.getClass("{nms}.PacketPlayOutMapChunk");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(NetProtocol.v1_18_2)) {
                CHAT_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutChat");
                SPAWN_ENTITY_LIVING_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityLiving");
                SPAWN_ENTITY_PAINTING_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityPainting");
            }

            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_9)) {
                BOSS_CLASS = Reflection.getClass("{nms}.PacketPlayOutBoss");
                MOUNT_CLASS = Reflection.getClass("{nms}.PacketPlayOutMount");
                UNLOAD_CHUNK_CLASS = Reflection.getClass("{nms}.PacketPlayOutUnloadChunk");
                VEHICLE_MOVE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutVehicleMove");
                SET_COOLDOWN_CLASS = Reflection.getClass("{nms}.PacketPlayOutSetCooldown");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_12)) {
                ADVANCEMENTS_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutAdvancements");
                RECIPES_CLASS = Reflection.getClass("{nms}.PacketPlayOutRecipes");
                SELECT_ADVANCEMENT_TAB_CLASS = Reflection.getClass("{nms}.PacketPlayOutSelectAdvancementTab");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_12_1)) {
                AUTO_RECIPE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutAutoRecipe");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_13)) {
                COMMANDS_CLASS = Reflection.getClass("{nms}.PacketPlayOutCommands");
                NBT_QUERY_CLASS = Reflection.getClass("{nms}.PacketPlayOutNBTQuery");
                RECIPE_UPDATE_CLASS = Reflection.getClass("{nms}.PacketPlayOutRecipeUpdate");
                STOP_SOUND_CLASS = Reflection.getClass("{nms}.PacketPlayOutStopSound");
                LOOK_AT_CLASS = Reflection.getClass("{nms}.PacketPlayOutLookAt");
                if (PROTOCOL_VERSION.isOlderThan(NetProtocol.v1_20_2))
                    TAGS_CLASS = Reflection.getClass("{nms}.PacketPlayOutTags");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_14)) {
                ENTITY_SOUND_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntitySound");
                LIGHT_UPDATE_CLASS = Reflection.getClass("{nms}.PacketPlayOutLightUpdate");
                OPEN_BOOK_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenBook");
                OPEN_WINDOW_HORSE_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenWindowHorse");
                OPEN_WINDOW_MERCHANT_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenWindowMerchant");
                VIEW_CENTRE_CLASS = Reflection.getClass("{nms}.PacketPlayOutViewCentre");
                VIEW_DISTANCE_CLASS = Reflection.getClass("{nms}.PacketPlayOutViewDistance");
            }
            if (PROTOCOL_VERSION.isBetween(NetProtocol.v1_14_4, NetProtocol.v1_18_2)) {
                BLOCK_BREAK_CLASS = Reflection.getClass("{nms}.PacketPlayOutBlockBreak");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(NetProtocol.v1_17)) {
                ENTITY_LOOK_CLASS = Reflection.getClass( "{nms}.PacketPlayOutEntity$PacketPlayOutEntityLook");
                REL_ENTITY_MOVE_CLASS = Reflection.getClass( "{nms}.PacketPlayOutEntity$PacketPlayOutRelEntityMove");
                REL_ENTITY_MOVE_LOOK_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook");
                WORLD_BORDER_CLASS = Reflection.getClass("{nms}.ClientboundInitializeBorderPacket");
            }
            if (PROTOCOL_VERSION.isOlderThan(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutCustomPayload");
                KEEP_ALIVE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutKeepAlive");
                KICK_DISCONNECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutKickDisconnect");
                RESOURCE_PACK_SEND_CLASS = Reflection.getClass("{nms}.PacketPlayOutResourcePackSend");
                NAMED_ENTITY_SPAWN_CLASS = Reflection.getClass("{nms}.PacketPlayOutNamedEntitySpawn");
            }
        }

        public enum Server implements RangedPacketTypeEnum {

            ABILITIES(ABILITIES_SERVER_CLASS, 54),
            ADVANCEMENTS(ADVANCEMENTS_SERVER_CLASS, 55, NetProtocolRange.since(NetProtocol.v1_12)),
            ANIMATION(ANIMATION_CLASS, 56),
            ATTACH_ENTITY(ATTACH_ENTITY_CLASS, 57),
            AUTO_RECIPE(AUTO_RECIPE_SERVER_CLASS, 58, NetProtocolRange.since(NetProtocol.v1_12_1)),
            BED(BED_CLASS, 59, NetProtocolRange.until(NetProtocol.v1_13_2)),
            BLOCK_ACTION(BLOCK_ACTION_CLASS, 60),
            BLOCK_BREAK(BLOCK_BREAK_CLASS, 61, NetProtocolRange.between(NetProtocol.v1_14, NetProtocol.v1_18_2)),
            BLOCK_BREAK_ANIMATION(BLOCK_BREAK_ANIMATION_CLASS, 62),
            BLOCK_CHANGE(BLOCK_CHANGE_CLASS, 63),
            BOSS(BOSS_CLASS, 64, NetProtocolRange.since(NetProtocol.v1_9)),
            CAMERA(CAMERA_CLASS, 65),
            CHAT(CHAT_SERVER_CLASS, 66, NetProtocolRange.until(NetProtocol.v1_18_2)),
            CLOSE_WINDOW(CLOSE_WINDOW_SERVER_CLASS, 67),
            COLLECT(COLLECT_CLASS, 68),
            COMBAT_EVENT(COMBAT_EVENT_CLASS, 69, NetProtocolRange.until(NetProtocol.v1_16_5)),
            COMMANDS(COMMANDS_CLASS, 70, NetProtocolRange.since(NetProtocol.v1_13)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, 71, NetProtocolRange.before(NetProtocol.v1_20_2)),
            ENTITY(ENTITY_CLASS, 72),
            ENTITY_DESTROY(ENTITY_DESTROY_CLASS, 73),
            ENTITY_EFFECT(ENTITY_EFFECT_CLASS, 74),
            ENTITY_EQUIPMENT(ENTITY_EQUIPMENT_CLASS, 75),
            ENTITY_HEAD_ROTATION(ENTITY_HEAD_ROTATION_CLASS, 76),
            ENTITY_LOOK(ENTITY_LOOK_CLASS, 77),
            ENTITY_METADATA(ENTITY_METADATA_CLASS, 78),
            ENTITY_MOVE(REL_ENTITY_MOVE_CLASS, 79),
            ENTITY_MOVE_LOOK(REL_ENTITY_MOVE_LOOK_CLASS, 80),
            ENTITY_SOUND(ENTITY_SOUND_CLASS, 81, NetProtocolRange.since(NetProtocol.v1_14)),
            ENTITY_STATUS(ENTITY_STATUS_CLASS, 82),
            ENTITY_TELEPORT(ENTITY_TELEPORT_CLASS, 83),
            ENTITY_VELOCITY(ENTITY_VELOCITY_CLASS, 84),
            EXPERIENCE(EXPERIENCE_CLASS, 85),
            EXPLOSION(EXPLOSION_CLASS, 86),
            GAME_STATE_CHANGE(GAME_STATE_CHANGE_CLASS, 87),
            HELD_ITEM_SLOT(HELD_ITEM_SLOT_SERVER_CLASS, 88),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, 89, NetProtocolRange.before(NetProtocol.v1_20_2)),
            KICK_DISCONNECT(KICK_DISCONNECT_CLASS, 90, NetProtocolRange.before(NetProtocol.v1_20_2)),
            LIGHT_UPDATE(LIGHT_UPDATE_CLASS, 91, NetProtocolRange.since(NetProtocol.v1_14)),
            LOGIN(LOGIN_CLASS, 92),
            LOOK_AT(LOOK_AT_CLASS, 93, NetProtocolRange.since(NetProtocol.v1_13)),
            MAP(MAP_CLASS, 94),
            MAP_CHUNK(MAP_CHUNK_CLASS, 95, NetProtocolRange.until(NetProtocol.v1_17_1)),
            MAP_CHUNK_BULK(MAP_CHUNK_BULK_CLASS, 96, NetProtocolRange.until(NetProtocol.v1_8_8)),
            MOUNT(MOUNT_CLASS, 97, NetProtocolRange.since(NetProtocol.v1_9)),
            MULTI_BLOCK_CHANGE(MULTI_BLOCK_CHANGE_CLASS, 98),
            NBT_QUERY(NBT_QUERY_CLASS, 99, NetProtocolRange.since(NetProtocol.v1_13)),
            NAMED_ENTITY_SPAWN(NAMED_ENTITY_SPAWN_CLASS, 100, NetProtocolRange.before(NetProtocol.v1_20_2)),
            NAMED_SOUND_EFFECT(NAMED_SOUND_EFFECT_CLASS, 101),
            OPEN_BOOK(OPEN_BOOK_CLASS, 102, NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_SIGN_EDITOR(OPEN_SIGN_EDITOR_CLASS, 103),
            OPEN_WINDOW(OPEN_WINDOW_CLASS, 104),
            OPEN_WINDOW_HORSE(OPEN_WINDOW_HORSE_CLASS, 105, NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_WINDOW_MERCHANT(OPEN_WINDOW_MERCHANT_CLASS, 106, NetProtocolRange.since(NetProtocol.v1_14)),
            PLAYER_INFO(PLAYER_INFO_CLASS, 107),
            PLAYERLIST_HEADER_FOOTER(PLAYER_LIST_HEADER_FOOTER_CLASS, 108),
            POSITION(POSITION_SERVER_CLASS, 109),
            RECIPE_UPDATE(RECIPE_UPDATE_CLASS, 110, NetProtocolRange.since(NetProtocol.v1_13)),
            RECIPES(RECIPES_CLASS, 111, NetProtocolRange.since(NetProtocol.v1_12)),
            REMOVE_ENTITY_EFFECT(REMOVE_ENTITY_EFFECT_CLASS, 112),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_CLASS, 113, NetProtocolRange.before(NetProtocol.v1_20_2)),
            RESPAWN(RESPAWN_CLASS, 114),
            SCOREBOARD_DISPLAY_OBJECTIVE(SCOREBOARD_DISPLAY_OBJECTIVE_CLASS, 115),
            SCOREBOARD_OBJECTIVE(SCOREBOARD_OBJECTIVE_CLASS, 116),
            SCOREBOARD_SCORE(SCOREBOARD_SCORE_CLASS, 117),
            SCOREBOARD_TEAM(SCOREBOARD_TEAM_CLASS, 118),
            SELECT_ADVANCEMENT_TAB(SELECT_ADVANCEMENT_TAB_CLASS, 119, NetProtocolRange.since(NetProtocol.v1_12)),
            SERVER_DIFFICULTY(SERVER_DIFFICULTY_CLASS, 120),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, 121, NetProtocolRange.until(NetProtocol.v1_8_8)),
            SET_COOLDOWN(SET_COOLDOWN_CLASS, 122, NetProtocolRange.since(NetProtocol.v1_9)),
            SET_SLOT(SET_SLOT_CLASS, 123),
            SPAWN_ENTITY(SPAWN_ENTITY_CLASS, 124),
            SPAWN_ENTITY_EXPERIENCE_ORB(SPAWN_ENTITY_EXPERIENCE_ORB_CLASS, 125),
            SPAWN_ENTITY_LIVING(SPAWN_ENTITY_LIVING_CLASS, 126, NetProtocolRange.until(NetProtocol.v1_18_2)),
            SPAWN_ENTITY_PAINTING(SPAWN_ENTITY_PAINTING_CLASS, 127, NetProtocolRange.until(NetProtocol.v1_18_2)),
            SPAWN_ENTITY_WEATHER(SPAWN_ENTITY_WEATHER_CLASS, 128, NetProtocolRange.until(NetProtocol.v1_15_2)),
            SPAWN_POSITION(SPAWN_POSITION_CLASS, 129),
            STATISTIC(STATISTIC_CLASS, 130),
            STOP_SOUND(STOP_SOUND_CLASS, 131, NetProtocolRange.since(NetProtocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_SERVER_CLASS, 132),
            TAGS(TAGS_CLASS, 133, NetProtocolRange.betweenBefore(NetProtocol.v1_13, NetProtocol.v1_20_2)),
            TILE_ENTITY_DATA(TILE_ENTITY_DATA_CLASS, 134),
            TITLE(TITLE_CLASS, 135, NetProtocolRange.until(NetProtocol.v1_16_5)),
            TRANSACTION(TRANSACTION_SERVER_CLASS, 136, NetProtocolRange.until(NetProtocol.v1_16_5)),
            UNLOAD_CHUNK(UNLOAD_CHUNK_CLASS, 137, NetProtocolRange.since(NetProtocol.v1_9)),
            UPDATE_ATTRIBUTES(UPDATE_ATTRIBUTES_CLASS, 138),
            UPDATE_ENTITY_NBT(UPDATE_ENTITY_NBT_CLASS, 139, NetProtocolRange.until(NetProtocol.v1_8_8)),
            UPDATE_HEALTH(UPDATE_HEALTH_CLASS, 140),
            UPDATE_SIGN(UPDATE_SIGN_SERVER_CLASS, 141, NetProtocolRange.until(NetProtocol.v1_9_2)),
            UPDATE_TIME(UPDATE_TIME_CLASS, 142),
            VEHICLE_MOVE(VEHICLE_MOVE_SERVER_CLASS, 143, NetProtocolRange.since(NetProtocol.v1_9)),
            VIEW_CENTRE(VIEW_CENTRE_CLASS, 144, NetProtocolRange.since(NetProtocol.v1_14)),
            VIEW_DISTANCE(VIEW_DISTANCE_CLASS, 145, NetProtocolRange.since(NetProtocol.v1_14)),
            WINDOW_DATA(WINDOW_DATA_CLASS, 146),
            WINDOW_ITEMS(WINDOW_ITEMS_CLASS, 147),
            WORLD_BORDER(WORLD_BORDER_CLASS, 148),
            WORLD_EVENT(WORLD_EVENT_CLASS, 149),
            WORLD_PARTICLES(WORLD_PARTICLES_CLASS, 150)
            ;

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
            public int getPacketId() {
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
    }
}
