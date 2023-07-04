package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.AsyncFuture;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.protocol.ProtocolRange;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Bukkit;

import java.util.*;

public class PacketType {

    private static class PacketDetails {
        byte packetId;
        PacketTypeEnum packetType;
    }

    private static void addPacket(Class<?> clazz, byte id, PacketTypeEnum type) {
        PACKET_STATE_TYPE.get(type.getState()).add(type);

        if (type instanceof RangedPacketTypeEnum) {
            if (!Protocol.protocolMatch((RangedPacketTypeEnum) type)) {
                return;
            }
        }
        PACKET_DETAILS.put(clazz, new PacketDetails(){{
            this.packetId = id;
            this.packetType = type;
        }});
    }

    private static final Protocol PROTOCOL_VERSION = Protocol.getProtocol();
    private static final Map<Class<?>, PacketDetails> PACKET_DETAILS = new IdentityHashMap<>();
    private static final Map<PacketState, List<PacketTypeEnum>> PACKET_STATE_TYPE = new HashMap<>();

    public static AsyncFuture load() {
        AsyncFuture future = new AsyncFuture();
        new Thread(() -> {
            loadAsync(future);
        }).start();
        return future;
    }

    private static void loadAsync(AsyncFuture future) {
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
        future.done();
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(PacketState state, PacketBound packetBound, Protocol protocol) {
        if (state == PacketState.UNKNOWN) {
            return new ArrayList<>();
        }
        final ArrayList<PacketTypeEnum> packetTypes = new ArrayList<>();
        for (PacketTypeEnum packetType : PACKET_STATE_TYPE.get(state)) {

            if (packetBound != null) {
                if (packetType.getBound() != packetBound) {
                    continue;
                }
            }
            if (packetType instanceof RangedPacketTypeEnum) {
                RangedPacketTypeEnum rangedPacketType = (RangedPacketTypeEnum) packetType;
                if (!Protocol.protocolMatch(rangedPacketType, protocol)) {
                    continue;
                }
            }
            packetTypes.add(packetType);
        }
        return packetTypes;
    }


    public static List<PacketTypeEnum> getTypesAvailableFor(PacketState state, Protocol protocol) {
        return getTypesAvailableFor(state, null, protocol);
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(Protocol protocol, PacketBound packetBound) {
        final List<PacketTypeEnum> totalList = new ArrayList<>();
        for (PacketState state : PacketState.values()) {
            totalList.addAll(getTypesAvailableFor(state, packetBound, protocol));
        }
        return totalList;
    }

    public static List<PacketTypeEnum> getTypesAvailableFor(Protocol protocol) {
        return getTypesAvailableFor(protocol, null);
    }

    public static Map<Class<?>, PacketDetails> getPacketDetails() {
        return PACKET_DETAILS;
    }

    public static PacketTypeEnum getPacketType(Class<?> clazz) {
        if (!PACKET_DETAILS.containsKey(clazz)) {
            throw new RuntimeException("Ingen packet details for " + clazz);
        }
        return PACKET_DETAILS.get(clazz).packetType;
    }

    public static class Handshake {

        private static final Class<?> SET_PROTOCOL_CLASS = Reflection.getClass("{nms}.PacketHandshakingInSetProtocol");

        private static final byte SET_PROTOCOL = 0x00;

        public enum Client implements PacketTypeEnum {

            SET_PROTOCOL(SET_PROTOCOL_CLASS, Handshake.SET_PROTOCOL);

            private final Class<?> nmsPacket;
            private final byte packetId;

            Client(Class<?> nmsPacket, byte packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public byte getPacketId() {
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

        private static final byte LOGIN_START = 0x00, ENCRYPTION_RESPONSE = 0x01, CUSTOM_PAYLOAD = 0x02;


        public enum Client implements RangedPacketTypeEnum {
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, Login.CUSTOM_PAYLOAD, ProtocolRange.since(Protocol.v1_13)),
            ENCRYPTION_RESPONSE(ENCRYPTION_RESPONSE_CLASS, Login.ENCRYPTION_RESPONSE),
            LOGIN_START(LOGIN_START_CLASS, Login.LOGIN_START);

            private final Class<?> nmsPacket;
            private final byte packetId;
            private final ProtocolRange protocolRange;

            Client(Class<?> nmsPacket, byte packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = ProtocolRange.ALL;
            }

            Client(Class<?> nmsPacket, byte packetId, ProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public byte getPacketId() {
                return packetId;
            }

            @Override
            public ProtocolRange getRange() {
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
        private static final byte DISCONNECT = 0x00, ENCRYPTION_REQUEST = 0x01, LOGIN_SUCCESS = 0x02, SET_COMPRESSION = 0x03, SERVERBOUND_CUSTOM_PAYLOAD = 0x04;

        static {
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_13)) {
                CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{nms}.PacketLoginInCustomPayload");
                SERVERBOUND_CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{nms}.PacketLoginOutCustomPayload");
            }
        }

        public enum Server implements RangedPacketTypeEnum {
            DISCONNECT(DISCONNECT_CLASS, Login.DISCONNECT),
            ENCRYPTION_REQUEST(ENCRYPTION_REQUEST_CLASS, Login.ENCRYPTION_REQUEST),
            LOGIN_SUCCESS(LOGIN_SUCCESS_CLASS, Login.LOGIN_SUCCESS),
            CUSTOM_PAYLOAD(SERVERBOUND_CUSTOM_PAYLOAD_CLASS, Login.SERVERBOUND_CUSTOM_PAYLOAD, ProtocolRange.since(Protocol.v1_13)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, Login.SET_COMPRESSION);

            private final Class<?> nmsPacket;
            private final byte packetId;
            private final ProtocolRange protocolRange;

            Server(Class<?> nmsPacket, byte packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                protocolRange = ProtocolRange.ALL;
            }

            Server(Class<?> nmsPacket, byte packetId, ProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public byte getPacketId() {
                return packetId;
            }

            @Override
            public ProtocolRange getRange() {
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

        private static final byte START = 0x00, PING = 0x01;


        public enum Client implements PacketTypeEnum {

            PING_REQUEST(PING_CLASS, PING),
            STATUS_REQUEST(START_CLASS, START);

            private final Class<?> nmsPacket;
            private final byte packetId;

            Client(Class<?> nmsPacket, byte packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public byte getPacketId() {
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

        private static final byte SERVER_INFO = 0x00, PONG = 0x01;

        public enum Server implements PacketTypeEnum {

            PING_RESPONSE(PONG_CLASS, PONG),
            STATUS_RESPONSE(SERVER_INFO_CLASS, SERVER_INFO);

            private final Class<?> nmsPacket;
            private final byte packetId;

            Server(Class<?> nmsPacket, byte packetId) {
                this.nmsPacket = nmsPacket;
                this.packetId = packetId;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public byte getPacketId() {
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
        private static final Class<?> CUSTOM_PAYLOAD_CLASS = Reflection.getClass("{nms}.PacketPlayInCustomPayload");
        private static final Class<?> ENCHANT_ITEM_CLASS = Reflection.getClass("{nms}.PacketPlayInEnchantItem");
        private static final Class<?> ENTITY_ACTION_CLASS = Reflection.getClass("{nms}.PacketPlayInEntityAction");
        private static final Class<?> HELD_ITEM_SLOT_CLASS = Reflection.getClass("{nms}.PacketPlayInHeldItemSlot");
        private static final Class<?> KEEP_ALIVE_CLASS = Reflection.getClass("{nms}.PacketPlayInKeepAlive");
        private static final Class<?> RESOURCE_PACK_STATUS_CLASS = Reflection.getClass("{nms}.PacketPlayInResourcePackStatus");
        private static final Class<?> SETTINGS_CLASS = Reflection.getClass("{nms}.PacketPlayInSettings");
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
        private static Class<?> DIFFICULTY_CHANGE_CLASS;
        private static Class<?> DIFFICULTY_LOCK_CLASS;
        private static Class<?> ENTITY_NBT_QUERY_CLASS;
        private static Class<?> FLYING_CLASS;
        private static Class<?> GROUND_CLASS;
        private static Class<?> ITEM_NAME_CLASS;
        private static Class<?> JIGSAW_GENERATE_CLASS;
        private static Class<?> LOOK_CLASS;
        private static Class<?> POSITION_CLASS;
        private static Class<?> POSITION_LOOK_CLASS;
        private static Class<?> PICK_ITEM_CLASS;
        private static Class<?> RECIPE_DISPLAYED_CLASS;
        private static Class<?> RECIPE_SETTINGS_CLASS;
        private static Class<?> SET_COMMAND_BLOCK_CLASS;
        private static Class<?> SET_COMMAND_MINECART_CLASS;
        private static Class<?> SET_JIGSAW_CLASS;
        private static Class<?> STRUCT_CLASS;
        private static Class<?> TELEPORT_ACCEPT_CLASS;
        private static Class<?> TILE_NBT_QUERY_CLASAS;
        private static Class<?> TR_SEL_CLASS;
        private static Class<?> TRANSACTION_CLASS;
        private static Class<?> USE_ITEM_CLASS;
        private static Class<?> VEHICLE_MOVE_CLASS;

        static {
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_16_5)) {
                TRANSACTION_CLASS = Reflection.getClass("{nms}.PacketPlayInTransaction");
                FLYING_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying");
                POSITION_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInPosition");
                POSITION_LOOK_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInPositionLook");
                LOOK_CLASS = Reflection.getSubClass(FLYING_CLASS, "PacketPlayInLook");
            }

            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_9)) {
                VEHICLE_MOVE_CLASS = Reflection.getClass("{nms}.PacketPlayInVehicleMove");
                BOAT_MOVE_CLASS = Reflection.getClass("{nms}.PacketPlayInBoatMove");
                USE_ITEM_CLASS = Reflection.getClass("{nms}.PacketPlayInUseItem");
                TELEPORT_ACCEPT_CLASS = Reflection.getClass("{nms}.PacketPlayInTeleportAccept");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_12)) {
                AUTO_RECIPE_CLASS = Reflection.getClass("{nms}.PacketPlayInAutoRecipe");
                RECIPE_DISPLAYED_CLASS = Reflection.getClass("{nms}.PacketPlayInRecipeDisplayed");
                ADVANCEMENTS_CLASS = Reflection.getClass("{nms}.PacketPlayInAdvancements");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_13)) {
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
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_14)) {
                DIFFICULTY_CHANGE_CLASS = Reflection.getClass("{nms}.PacketPlayInDifficultyChange");
                DIFFICULTY_LOCK_CLASS = Reflection.getClass("{nms}.PacketPlayInDifficultyLock");
                SET_JIGSAW_CLASS = Reflection.getClass("{nms}.PacketPlayInSetJigsaw");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_16_1)) {
                JIGSAW_GENERATE_CLASS = Reflection.getClass("{nms}.PacketPlayInJigsawGenerate");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_16_2)) {
                RECIPE_SETTINGS_CLASS = Reflection.getClass("{nms}.PacketPlayInRecipeSettings");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_17)) {
                POSITION_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$PacketPlayInPosition");
                POSITION_LOOK_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$PacketPlayInPositionLook");
                LOOK_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$PacketPlayInLook");
                GROUND_CLASS = Reflection.getClass("{nms}.PacketPlayInFlying$d");
            }
        }

        private static final byte
                TELEPORT_ACCEPT = 0x00, TILE_NBT_QUERY = 0x01, DIFFICULTY_CHANGE = 0x01,
                CLIENT_COMMAND = 0x07, SETTINGS = 0x08, TAB_COMPLETE = 0x09, ENCHANT_ITEM = 0x0A,
                WINDOW_CLICK = 0x0B, CLOSE_WINDOW = 0x0C, CUSTOM_PAYLOAD = 0x0D, B_EDIT = 0x0E,
                ENTITY_NBT_QUERY = 0x0F, USE_ENTITY = 0x10, JIGSAW_GENERATE = 0x11, KEEP_ALIVE = 0x12,
                DIFFICULTY_LOCK = 0x12, POSITION = 0x14, POSITION_LOOK = 0x15, LOOK = 0x16,
                GROUND = 0x17, VEHICLE_MOVE = 0x18, BOAT_MOVE = 0x19, PICK_ITEM = 0x1A, AUTO_RECIPE = 0x1B,
                ABILITIES = 0x1C, BLOCK_DIG = 0x1D, ENTITY_ACTION = 0x1E, STEER_VEHICLE = 0x1F,
                PONG = 0x20, RECIPE_SETTINGS = 0x21, RECIPE_DISPLAYED = 0x22, ITEM_NAME = 0x23,
                RESOURCE_PACK_STATUS = 0x24, ADVANCEMENTS = 0x25, TR_SEL = 0x026, BEACON = 0x27,
                HELD_ITEM_SLOT = 0x28, SET_COMMAND_BLOCK = 0x29, SET_COMMAND_MINECART = 0x2A,
                SET_CREATIVE_SLOT = 0x2B, SET_JIGSAW = 0x2C, STRUCT = 0x2D, UPDATE_SIGN = 0x2E,
                ARM_ANIMATION = 0x2F, SPECTATE = 0x30, USE_ITEM = 0x31, BLOCK_PLACE = 0x32,
                TRANSACTION = 0x33, FLYING = 0x34, CHAT = 0x35;

        public enum Client implements RangedPacketTypeEnum {

            ABILITIES(ABILITIES_CLASS, Play.ABILITIES),
            ADVANCEMENTS(ADVANCEMENTS_CLASS, Play.ADVANCEMENTS, ProtocolRange.since(Protocol.v1_12)),
            ARM_ANIMATION(ARM_ANIMATION_CLASS, Play.ARM_ANIMATION),
            AUTO_RECIPE(AUTO_RECIPE_CLASS, Play.AUTO_RECIPE, ProtocolRange.since(Protocol.v1_12)),
            B_EDIT(B_EDIT_CLASS, Play.B_EDIT, ProtocolRange.since(Protocol.v1_13)),
            BEACON(BEACON_CLASS, Play.BEACON, ProtocolRange.since(Protocol.v1_13)),
            BLOCK_DIG(BLOCK_DIG_CLASS, Play.BLOCK_DIG),
            BLOCK_PLACE(BLOCK_PLACE_CLASS, Play.BLOCK_PLACE),
            BOAT_MOVE(BOAT_MOVE_CLASS, Play.BOAT_MOVE, ProtocolRange.since(Protocol.v1_9)),
            CHAT(CHAT_CLASS, Play.CHAT),
            CLIENT_COMMAND(CLIENT_COMMAND_CLASS, Play.CLIENT_COMMAND),
            CLOSE_WINDOW(CLOSE_WINDOW_CLASS, Play.CLOSE_WINDOW),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, Play.CUSTOM_PAYLOAD),
            DIFFICULTY_CHANGE(DIFFICULTY_CHANGE_CLASS, Play.DIFFICULTY_CHANGE, ProtocolRange.since(Protocol.v1_14)),
            DIFFICULTY_LOCK(DIFFICULTY_LOCK_CLASS, Play.DIFFICULTY_LOCK, ProtocolRange.since(Protocol.v1_14)),
            ENCHANT_ITEM(ENCHANT_ITEM_CLASS, Play.ENCHANT_ITEM),
            ENTITY_ACTION(ENTITY_ACTION_CLASS, Play.ENTITY_ACTION),
            ENTITY_NBT_QUERY(ENTITY_NBT_QUERY_CLASS, Play.ENTITY_NBT_QUERY, ProtocolRange.since(Protocol.v1_13)),
            FLYING(FLYING_CLASS, Play.FLYING, ProtocolRange.until(Protocol.v1_16_5)),
            GROUND(GROUND_CLASS, Play.GROUND, ProtocolRange.since(Protocol.v1_17)),
            HELD_ITEM_SLOT(HELD_ITEM_SLOT_CLASS, Play.HELD_ITEM_SLOT),
            ITEM_NAME(ITEM_NAME_CLASS, Play.ITEM_NAME, ProtocolRange.since(Protocol.v1_13)),
            JIGSAW_GENERATE(JIGSAW_GENERATE_CLASS, Play.JIGSAW_GENERATE, ProtocolRange.since(Protocol.v1_16_1)),
            KEEP_ALIVE(KEEP_ALIVE_CLASS, Play.KEEP_ALIVE),
            LOOK(LOOK_CLASS, Play.LOOK),
            PICK_ITEM(PICK_ITEM_CLASS, Play.PICK_ITEM, ProtocolRange.since(Protocol.v1_13)),
            POSITION(POSITION_CLASS, Play.POSITION),
            POSITION_LOOK(POSITION_LOOK_CLASS, Play.POSITION_LOOK),
            RECIPE_DISPLAYED(RECIPE_DISPLAYED_CLASS, Play.RECIPE_DISPLAYED, ProtocolRange.since(Protocol.v1_12)),
            RECIPE_SETTINGS(RECIPE_SETTINGS_CLASS, Play.RECIPE_SETTINGS, ProtocolRange.since(Protocol.v1_16_2)),
            RESOURCE_PACK_STATUS(RESOURCE_PACK_STATUS_CLASS, Play.RESOURCE_PACK_STATUS),
            SET_COMMAND_BLOCK(SET_COMMAND_BLOCK_CLASS, Play.SET_COMMAND_BLOCK, ProtocolRange.since(Protocol.v1_13)),
            SET_COMMAND_MINECART(SET_COMMAND_MINECART_CLASS, Play.SET_COMMAND_MINECART, ProtocolRange.since(Protocol.v1_13)),
            SET_CREATIVE_SLOT(SET_CREATIVE_SLOT_CLASS, Play.SET_CREATIVE_SLOT),
            SET_JIGSAW(SET_JIGSAW_CLASS, Play.SET_JIGSAW, ProtocolRange.since(Protocol.v1_14)),
            SETTINGS(SETTINGS_CLASS, Play.SETTINGS),
            SPECTATE(SPECTATE_CLASS, Play.SPECTATE),
            STEER_VEHICLE(STEER_VEHICLE_CLASS, Play.STEER_VEHICLE),
            STRUCT(STRUCT_CLASS, Play.STRUCT, ProtocolRange.since(Protocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_CLASS, Play.TAB_COMPLETE),
            TELEPORT_ACCEPT(TELEPORT_ACCEPT_CLASS, Play.TELEPORT_ACCEPT, ProtocolRange.since(Protocol.v1_9)),
            TILE_NBT_QUERY(TILE_NBT_QUERY_CLASAS, Play.TILE_NBT_QUERY, ProtocolRange.since(Protocol.v1_13)),
            TR_SEL(TR_SEL_CLASS, Play.TR_SEL, ProtocolRange.since(Protocol.v1_13)),
            TRANSACTION(TRANSACTION_CLASS, Play.TRANSACTION, ProtocolRange.until(Protocol.v1_16_5)),
            UPDATE_SIGN(UPDATE_SIGN_CLASS, Play.UPDATE_SIGN),
            USE_ENTITY(USE_ENTITY_CLASS, Play.USE_ENTITY),
            USE_ITEM(USE_ITEM_CLASS, Play.USE_ITEM, ProtocolRange.since(Protocol.v1_9)),
            VEHICLE_MOVE(VEHICLE_MOVE_CLASS, Play.VEHICLE_MOVE, ProtocolRange.since(Protocol.v1_9)),
            WINDOW_CLICK(WINDOW_CLICK_CLASS, Play.WINDOW_CLICK),
            ;

            private final Class<?> nmsClass;
            private final byte packetId;
            private final ProtocolRange protocolRange;

            Client(Class<?> nmsClass, byte packetId) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = ProtocolRange.ALL;
            }

            Client(Class<?> nmsClass, byte packetId, ProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsClass;
            }

            @Override
            public byte getPacketId() {
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
            public ProtocolRange getRange() {
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
        private static final Class<?> CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutCustomPayload");
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
        private static final Class<?> KEEP_ALIVE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutKeepAlive");
        private static final Class<?> KICK_DISCONNECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutKickDisconnect");
        private static final Class<?> LOGIN_CLASS = Reflection.getClass("{nms}.PacketPlayOutLogin");
        private static final Class<?> MAP_CLASS = Reflection.getClass("{nms}.PacketPlayOutMap");
        private static final Class<?> MULTI_BLOCK_CHANGE_CLASS = Reflection.getClass("{nms}.PacketPlayOutMultiBlockChange");
        private static final Class<?> NAMED_ENTITY_SPAWN_CLASS = Reflection.getClass("{nms}.PacketPlayOutNamedEntitySpawn");
        private static final Class<?> NAMED_SOUND_EFFECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutNamedSoundEffect");
        private static final Class<?> OPEN_SIGN_EDITOR_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenSignEditor");
        private static final Class<?> OPEN_WINDOW_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenWindow");
        private static final Class<?> PLAYER_INFO_CLASS = Reflection.getClass("{nms}.PacketPlayOutPlayerInfo");
        private static final Class<?> PLAYER_LIST_HEADER_FOOTER_CLASS = Reflection.getClass("{nms}.PacketPlayOutPlayerListHeaderFooter");
        private static final Class<?> POSITION_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutPosition");
        private static final Class<?> REMOVE_ENTITY_EFFECT_CLASS = Reflection.getClass("{nms}.PacketPlayOutRemoveEntityEffect");
        private static final Class<?> RESOURCE_PACK_SEND_CLASS = Reflection.getClass("{nms}.PacketPlayOutResourcePackSend");
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
        private static Class<?> ENTITY_LOOK_CLASS;
        private static Class<?> ENTITY_SOUND_CLASS;
        private static Class<?> LIGHT_UPDATE_CLASS;
        private static Class<?> LOOK_AT_CLASS;
        private static Class<?> MAP_CHUNK_CLASS;
        private static Class<?> MAP_CHUNK_BULK_CLASS;
        private static Class<?> MOUNT_CLASS;
        private static Class<?> NBT_QUERY_CLASS;
        private static Class<?> OPEN_BOOK_CLASS;
        private static Class<?> OPEN_WINDOW_HORSE_CLASS;
        private static Class<?> OPEN_WINDOW_MERCHANT_CLASS;
        private static Class<?> RECIPE_UPDATE_CLASS;
        private static Class<?> RECIPES_CLASS;
        private static Class<?> REL_ENTITY_MOVE_LOOK_CLASS;
        private static Class<?> REL_ENTITY_MOVE_CLASS;
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
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_8_8)) {
                MAP_CHUNK_BULK_CLASS = Reflection.getClass("{nms}.PacketPlayOutMapChunkBulk");
                UPDATE_ENTITY_NBT_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateEntityNBT");
                SET_COMPRESSION_CLASS = Reflection.getClass("{nms}.PacketPlayOutSetCompression");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_9_2)) {
                UPDATE_SIGN_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutUpdateSign");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_13_2)) {
                BED_CLASS = Reflection.getClass("{nms}.PacketPlayOutBed");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_15_2)) {
                SPAWN_ENTITY_WEATHER_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityWeather");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_16_5)) {
                COMBAT_EVENT_CLASS = Reflection.getClass("{nms}.PacketPlayOutCombatEvent");
                ENTITY_LOOK_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutEntityLook");
                REL_ENTITY_MOVE_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutRelEntityMove");
                REL_ENTITY_MOVE_LOOK_CLASS = Reflection.getSubClass(ENTITY_CLASS, "PacketPlayOutRelEntityMoveLook");
                TRANSACTION_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutTransaction");
                TITLE_CLASS = Reflection.getClass("{nms}.PacketPlayOutTitle");
                WORLD_BORDER_CLASS = Reflection.getClass("{nms}.PacketPlayOutWorldBorder");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_17_1)) {
                MAP_CHUNK_CLASS = Reflection.getClass("{nms}.PacketPlayOutMapChunk");
            }
            if (PROTOCOL_VERSION.isOlderThanOrEqual(Protocol.v1_18_2)) {
                CHAT_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutChat");
                SPAWN_ENTITY_LIVING_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityLiving");
                SPAWN_ENTITY_PAINTING_CLASS = Reflection.getClass("{nms}.PacketPlayOutSpawnEntityPainting");
            }

            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_9)) {
                BOSS_CLASS = Reflection.getClass("{nms}.PacketPlayOutBoss");
                MOUNT_CLASS = Reflection.getClass("{nms}.PacketPlayOutMount");
                UNLOAD_CHUNK_CLASS = Reflection.getClass("{nms}.PacketPlayOutUnloadChunk");
                VEHICLE_MOVE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutVehicleMove");
                SET_COOLDOWN_CLASS = Reflection.getClass("{nms}.PacketPlayOutSetCooldown");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_12)) {
                ADVANCEMENTS_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutAdvancements");
                RECIPES_CLASS = Reflection.getClass("{nms}.PacketPlayOutRecipes");
                SELECT_ADVANCEMENT_TAB_CLASS = Reflection.getClass("{nms}.PacketPlayOutSelectAdvancementTab");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_12_1)) {
                AUTO_RECIPE_SERVER_CLASS = Reflection.getClass("{nms}.PacketPlayOutAutoRecipe");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_13)) {
                COMMANDS_CLASS = Reflection.getClass("{nms}.PacketPlayOutCommands");
                NBT_QUERY_CLASS = Reflection.getClass("{nms}.PacketPlayOutNBTQuery");
                RECIPE_UPDATE_CLASS = Reflection.getClass("{nms}.PacketPlayOutRecipeUpdate");
                STOP_SOUND_CLASS = Reflection.getClass("{nms}.PacketPlayOutStopSound");
                TAGS_CLASS = Reflection.getClass("{nms}.PacketPlayOutTags");
                LOOK_AT_CLASS = Reflection.getClass("{nms}.PacketPlayOutLookAt");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_14)) {
                ENTITY_SOUND_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntitySound");
                LIGHT_UPDATE_CLASS = Reflection.getClass("{nms}.PacketPlayOutLightUpdate");
                OPEN_BOOK_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenBook");
                OPEN_WINDOW_HORSE_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenWindowHorse");
                OPEN_WINDOW_MERCHANT_CLASS = Reflection.getClass("{nms}.PacketPlayOutOpenWindowMerchant");
                VIEW_CENTRE_CLASS = Reflection.getClass("{nms}.PacketPlayOutViewCentre");
                VIEW_DISTANCE_CLASS = Reflection.getClass("{nms}.PacketPlayOutViewDistance");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_14_4)) {
                BLOCK_BREAK_CLASS = Reflection.getClass("{nms}.PacketPlayOutBlockBreak");
            }
            if (PROTOCOL_VERSION.isNewerThanOrEqual(Protocol.v1_17)) {
                ENTITY_LOOK_CLASS = Reflection.getClass( "{nms}.PacketPlayOutEntity$PacketPlayOutEntityLook");
                REL_ENTITY_MOVE_CLASS = Reflection.getClass( "{nms}.PacketPlayOutEntity$PacketPlayOutRelEntityMove");
                REL_ENTITY_MOVE_LOOK_CLASS = Reflection.getClass("{nms}.PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook");
                WORLD_BORDER_CLASS = Reflection.getClass("{nms}.ClientboundInitializeBorderPacket");
            }
        }

        private static final byte
                BUNDLE = 0x00, SPAWN_ENTITY = 0x01, SPAWN_ENTITY_EXPERIENCE_ORB = 0x02,
                NAMED_ENTITY_SPAWN_SERVER = 0x03, ANIMATION = 0x04, STATISTIC = 0x05, BLOCK_CHANGED_ACK = 0x06,
                BLOCK_BREAK_ANIMATION = 0x07, TILE_ENTITY_DATA = 0x08, BLOCK_ACTION = 0x09, BLOCK_CHANGE = 0x0A,
                BOSS = 0x0B, SERVER_DIFFICULTY = 0x0C, ATTACH_ENTITY = 0x0D, CLEAR_TITLES = 0x0E,
                TAB_COMPLETE_SERVER = 0x0F, COMMANDS = 0x10, CLOSE_WINDOW_SERVER = 0x11, WINDOW_ITEMS = 0x012,
                WINDOW_DATA = 0x13, SET_SLOT = 0x14, SET_COOLDOWN = 0x15, CUSTOM_CHAT_COMPLETIONS = 0x16,
                CUSTOM_PAYLOAD_SERVER = 0x17, DAMAGE_EVENT = 0x18, DELETE_CHAT_MESSAGE = 0x19, KICK_DISCONNECT = 0x1A,
                DISGUISED_CHAT = 0x1B, ENTITY_STATUS = 0x1C, EXPLOSION = 0x1D, UNLOAD_CHUNK = 0x1E,
                GAME_STATE_CHANGE = 0x1F, OPEN_WINDOW_HORSE = 0x20, HURT_ANIMATION = 0x21, INITIALIZE_BORDER = 0x22,
                KEEP_ALIVE_SERVER = 0x23, MAP_CHUNK = 0x24, WORLD_EVENT = 0x25, WORLD_PARTICLES = 0x26,
                LIGHT_UPDATE = 0x27, LOGIN = 0x28, MAP = 0x29, OPEN_WINDOW_MERCHANT = 0x2A, REL_ENTITY_MOVE = 0x2B,
                REL_ENTITY_MOVE_LOOK = 0x2C, ENTITY_LOOK = 0x2D, VEHICLE_MOVE_SERVER = 0x2E, OPEN_BOOK = 0x2F,
                OPEN_WINDOW = 0x30, OPEN_SIGN_EDITOR = 0x31, PING = 0x32, AUTO_RECIPE_SERVER = 0x33,
                ABILITIES_SERVER = 0x34, CHAT_SERVER = 0x35, PLAY_COMBAT_END = 0x36, PLAYER_COMBAT_ENTER = 0x37,
                PLAYER_COMBAT_KILL = 0x38, PLAYER_INFO_REMOVE = 0x39, PLAYER_INFO = 0x3A, LOOK_AT = 0x3B,
                POSITION_SERVER = 0x3C, RECIPES = 0x3D, ENTITY_DESTROY = 0x3E, REMOVE_ENTITY_EFFECT = 0x3F,
                RESOURCE_PACK_SEND = 0x40, RESPAWN = 0x41, ENTITY_HEAD_ROTATION = 0x42, MULTI_BLOCK_CHANGE = 0x43,
                SELECT_ADVANCEMENT_TAB = 0x44, SERVER_DATA = 0x45, SET_ACTION_BAR_TEXT = 0x46, SET_BORDER_CENTER = 0x47,
                ANIMATION_SERVER = 0x48, SET_BORDER_SIZE = 0x49, SET_BORDER_WARNING_DELAY = 0x4A,
                SET_BORDER_WARNING_DISTANCE = 0x4B, CAMERA = 0x4C, HELD_ITEM_SLOT_SERVER = 0x4D, VIEW_CENTRE = 0x4E,
                VIEW_DISTANCE = 0x4F, SPAWN_POSITION = 0x50, SCOREBOARD_DISPLAY_OBJECTIVE = 0x51,
                ENTITY_METADATA = 0x52, ENTITY_VELOCITY = 0x54, ENTITY_EQUIPMENT = 0x55,
                EXPERIENCE = 0x56, UPDATE_HEALTH = 0x57, SCOREBOARD_OBJECTIVE = 0x58, MOUNT = 0x59,
                SCOREBOARD_TEAM = 0x5A, SCOREBOARD_SCORE = 0x5B, UPDATE_SIMULATION_DISTANCE = 0x5C,
                SET_SUBTITLE_TEXT = 0x5D, UPDATE_TIME = 0x5E, SET_TITLE_TEXT = 0x5F, SET_TITLES_ANIMATION = 0x60,
                ENTITY_SOUND = 0x61, NAMED_SOUND_EFFECT = 0x62, STOP_SOUND = 0x63, SYSTEM_CHAT = 0x64,
                PLAYER_LIST_HEADER_FOOTER = 0x65, NBT_QUERY = 0x66, COLLECT = 0x67, ENTITY_TELEPORT = 0x68,
                ADVANCEMENTS_SERVER = 0x69, UPDATE_ATTRIBUTES = 0x6A, UPDATE_ENABLED_FEATURES = 0x6B,
                ENTITY_EFFECT = 0x6C, RECIPE_UPDATE = 0x6D, TAGS = 0x6E, MAP_CHUNK_BULK = (byte) 255,
                SET_COMPRESSION = (byte) 254, UPDATE_ENTITY_NBT = (byte) 253, UPDATE_SIGN_SERVER = (byte) 252,
                BED = (byte) 251, SPAWN_ENTITY_WEATHER = (byte) 250, TITLE = (byte) 249, WORLD_BORDER = (byte) 248,
                COMBAT_EVENT = (byte) 249, TRANSACTION_SERVER = (byte) 248, ENTITY = (byte) 245,
                SPAWN_ENTITY_LIVING = (byte) 244, SPAWN_ENTITY_PAINTING = (byte) 243, BLOCK_BREAK = (byte) 242;


        public enum Server implements RangedPacketTypeEnum {

            ABILITIES(ABILITIES_SERVER_CLASS, Play.ABILITIES_SERVER),
            ADVANCEMENTS(ADVANCEMENTS_SERVER_CLASS, Play.ADVANCEMENTS_SERVER, ProtocolRange.since(Protocol.v1_12)),
            ANIMATION(ANIMATION_CLASS, Play.ANIMATION_SERVER),
            ATTACH_ENTITY(ATTACH_ENTITY_CLASS, Play.ATTACH_ENTITY),
            AUTO_RECIPE(AUTO_RECIPE_SERVER_CLASS, Play.AUTO_RECIPE_SERVER, ProtocolRange.since(Protocol.v1_12_1)),
            BED(BED_CLASS, Play.BED, ProtocolRange.until(Protocol.v1_13_2)),
            BLOCK_ACTION(BLOCK_ACTION_CLASS, Play.BLOCK_ACTION),
            BLOCK_BREAK(BLOCK_BREAK_CLASS, Play.BLOCK_BREAK, ProtocolRange.since(Protocol.v1_14)),
            BLOCK_BREAK_ANIMATION(BLOCK_BREAK_ANIMATION_CLASS, Play.BLOCK_BREAK_ANIMATION),
            BLOCK_CHANGE(BLOCK_CHANGE_CLASS, Play.BLOCK_CHANGE),
            BOSS(BOSS_CLASS, Play.BOSS, ProtocolRange.since(Protocol.v1_9)),
            CAMERA(CAMERA_CLASS, Play.CAMERA),
            CHAT(CHAT_SERVER_CLASS, Play.CHAT_SERVER, ProtocolRange.until(Protocol.v1_18_2)),
            CLOSE_WINDOW(CLOSE_WINDOW_SERVER_CLASS, CLOSE_WINDOW_SERVER),
            COLLECT(COLLECT_CLASS, Play.COLLECT),
            COMBAT_EVENT(COMBAT_EVENT_CLASS, Play.COMBAT_EVENT, ProtocolRange.until(Protocol.v1_16_5)),
            COMMANDS(COMMANDS_CLASS, Play.COMMANDS, ProtocolRange.since(Protocol.v1_13)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, Play.CUSTOM_PAYLOAD_SERVER),
            ENTITY(ENTITY_CLASS, Play.ENTITY),
            ENTITY_DESTROY(ENTITY_DESTROY_CLASS, Play.ENTITY_DESTROY),
            ENTITY_EFFECT(ENTITY_EFFECT_CLASS, Play.ENTITY_EFFECT),
            ENTITY_EQUIPMENT(ENTITY_EQUIPMENT_CLASS, Play.ENTITY_EQUIPMENT),
            ENTITY_HEAD_ROTATION(ENTITY_HEAD_ROTATION_CLASS, Play.ENTITY_HEAD_ROTATION),
            ENTITY_LOOK(ENTITY_LOOK_CLASS, Play.ENTITY_LOOK),
            ENTITY_METADATA(ENTITY_METADATA_CLASS, Play.ENTITY_METADATA),
            ENTITY_MOVE(REL_ENTITY_MOVE_CLASS, Play.REL_ENTITY_MOVE),
            ENTITY_MOVE_LOOK(REL_ENTITY_MOVE_LOOK_CLASS, Play.REL_ENTITY_MOVE_LOOK),
            ENTITY_SOUND(ENTITY_SOUND_CLASS, Play.ENTITY_SOUND, ProtocolRange.since(Protocol.v1_14)),
            ENTITY_STATUS(ENTITY_STATUS_CLASS, Play.ENTITY_STATUS),
            ENTITY_TELEPORT(ENTITY_TELEPORT_CLASS, Play.ENTITY_TELEPORT),
            ENTITY_VELOCITY(ENTITY_VELOCITY_CLASS, Play.ENTITY_VELOCITY),
            EXPERIENCE(EXPERIENCE_CLASS, Play.EXPERIENCE),
            EXPLOSION(EXPLOSION_CLASS, Play.EXPLOSION),
            GAME_STATE_CHANGE(GAME_STATE_CHANGE_CLASS, Play.GAME_STATE_CHANGE),
            HELD_ITEM_SLOT(HELD_ITEM_SLOT_SERVER_CLASS, Play.HELD_ITEM_SLOT_SERVER),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, Play.KEEP_ALIVE_SERVER),
            KICK_DISCONNECT(KICK_DISCONNECT_CLASS, Play.KICK_DISCONNECT),
            LIGHT_UPDATE(LIGHT_UPDATE_CLASS, Play.LIGHT_UPDATE, ProtocolRange.since(Protocol.v1_14)),
            LOGIN(LOGIN_CLASS, Play.LOGIN),
            LOOK_AT(LOOK_AT_CLASS, Play.LOOK_AT, ProtocolRange.since(Protocol.v1_13)),
            MAP(MAP_CLASS, Play.MAP),
            MAP_CHUNK(MAP_CHUNK_CLASS, Play.MAP_CHUNK, ProtocolRange.until(Protocol.v1_17_1)),
            MAP_CHUNK_BULK(MAP_CHUNK_BULK_CLASS, Play.MAP_CHUNK_BULK, ProtocolRange.until(Protocol.v1_8_8)),
            MOUNT(MOUNT_CLASS, Play.MOUNT, ProtocolRange.since(Protocol.v1_9)),
            MULTI_BLOCK_CHANGE(MULTI_BLOCK_CHANGE_CLASS, Play.MULTI_BLOCK_CHANGE),
            NBT_QUERY(NBT_QUERY_CLASS, Play.NBT_QUERY, ProtocolRange.since(Protocol.v1_13)),
            NAMED_ENTITY_SPAWN(NAMED_ENTITY_SPAWN_CLASS, Play.NAMED_ENTITY_SPAWN_SERVER),
            NAMED_SOUND_EFFECT(NAMED_SOUND_EFFECT_CLASS, Play.NAMED_SOUND_EFFECT),
            OPEN_BOOK(OPEN_BOOK_CLASS, Play.OPEN_BOOK, ProtocolRange.since(Protocol.v1_14)),
            OPEN_SIGN_EDITOR(OPEN_SIGN_EDITOR_CLASS, Play.OPEN_SIGN_EDITOR),
            OPEN_WINDOW(OPEN_WINDOW_CLASS, Play.OPEN_WINDOW),
            OPEN_WINDOW_HORSE(OPEN_WINDOW_HORSE_CLASS, Play.OPEN_WINDOW_HORSE, ProtocolRange.since(Protocol.v1_14)),
            OPEN_WINDOW_MERCHANT(OPEN_WINDOW_MERCHANT_CLASS, Play.OPEN_WINDOW_MERCHANT, ProtocolRange.since(Protocol.v1_14)),
            PLAYER_INFO(PLAYER_INFO_CLASS, Play.PLAYER_INFO),
            PLAYERLIST_HEADER_FOOTER(PLAYER_LIST_HEADER_FOOTER_CLASS, Play.PLAYER_LIST_HEADER_FOOTER),
            POSITION(POSITION_SERVER_CLASS, Play.POSITION_SERVER),
            RECIPE_UPDATE(RECIPE_UPDATE_CLASS, Play.RECIPE_UPDATE, ProtocolRange.since(Protocol.v1_13)),
            RECIPES(RECIPES_CLASS, Play.RECIPES, ProtocolRange.since(Protocol.v1_12)),
            REMOVE_ENTITY_EFFECT(REMOVE_ENTITY_EFFECT_CLASS, Play.REMOVE_ENTITY_EFFECT),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_CLASS, Play.RESOURCE_PACK_SEND),
            RESPAWN(RESPAWN_CLASS, Play.RESPAWN),
            SCOREBOARD_DISPLAY_OBJECTIVE(SCOREBOARD_DISPLAY_OBJECTIVE_CLASS, Play.SCOREBOARD_DISPLAY_OBJECTIVE),
            SCOREBOARD_OBJECTIVE(SCOREBOARD_OBJECTIVE_CLASS, Play.SCOREBOARD_OBJECTIVE),
            SCOREBOARD_SCORE(SCOREBOARD_SCORE_CLASS, Play.SCOREBOARD_SCORE),
            SCOREBOARD_TEAM(SCOREBOARD_TEAM_CLASS, Play.SCOREBOARD_TEAM),
            SELECT_ADVANCEMENT_TAB(SELECT_ADVANCEMENT_TAB_CLASS, Play.SELECT_ADVANCEMENT_TAB, ProtocolRange.since(Protocol.v1_12)),
            SERVER_DIFFICULTY(SERVER_DIFFICULTY_CLASS, Play.SERVER_DIFFICULTY),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, Play.SET_COMPRESSION, ProtocolRange.until(Protocol.v1_8_8)),
            SET_COOLDOWN(SET_COOLDOWN_CLASS, Play.SET_COOLDOWN, ProtocolRange.since(Protocol.v1_9)),
            SET_SLOT(SET_SLOT_CLASS, Play.SET_SLOT),
            SPAWN_ENTITY(SPAWN_ENTITY_CLASS, Play.SPAWN_ENTITY),
            SPAWN_ENTITY_EXPERIENCE_ORB(SPAWN_ENTITY_EXPERIENCE_ORB_CLASS, Play.SPAWN_ENTITY_EXPERIENCE_ORB),
            SPAWN_ENTITY_LIVING(SPAWN_ENTITY_LIVING_CLASS, Play.SPAWN_ENTITY_LIVING, ProtocolRange.until(Protocol.v1_18_2)),
            SPAWN_ENTITY_PAINTING(SPAWN_ENTITY_PAINTING_CLASS, Play.SPAWN_ENTITY_PAINTING, ProtocolRange.until(Protocol.v1_18_2)),
            SPAWN_ENTITY_WEATHER(SPAWN_ENTITY_WEATHER_CLASS, Play.SPAWN_ENTITY_WEATHER, ProtocolRange.until(Protocol.v1_15_2)),
            SPAWN_POSITION(SPAWN_POSITION_CLASS, Play.SPAWN_POSITION),
            STATISTIC(STATISTIC_CLASS, Play.STATISTIC),
            STOP_SOUND(STOP_SOUND_CLASS, Play.STOP_SOUND, ProtocolRange.since(Protocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_SERVER_CLASS, Play.TAB_COMPLETE_SERVER),
            TAGS(TAGS_CLASS, Play.TAGS, ProtocolRange.since(Protocol.v1_13)),
            TILE_ENTITY_DATA(TILE_ENTITY_DATA_CLASS, Play.TILE_ENTITY_DATA),
            TITLE(TITLE_CLASS, Play.TITLE, ProtocolRange.until(Protocol.v1_16_5)),
            TRANSACTION(TRANSACTION_SERVER_CLASS, Play.TRANSACTION_SERVER, ProtocolRange.until(Protocol.v1_16_5)),
            UNLOAD_CHUNK(UNLOAD_CHUNK_CLASS, Play.UNLOAD_CHUNK, ProtocolRange.since(Protocol.v1_9)),
            UPDATE_ATTRIBUTES(UPDATE_ATTRIBUTES_CLASS, Play.UPDATE_ATTRIBUTES),
            UPDATE_ENTITY_NBT(UPDATE_ENTITY_NBT_CLASS, Play.UPDATE_ENTITY_NBT, ProtocolRange.until(Protocol.v1_8_8)),
            UPDATE_HEALTH(UPDATE_HEALTH_CLASS, Play.UPDATE_HEALTH),
            UPDATE_SIGN(UPDATE_SIGN_SERVER_CLASS, Play.UPDATE_SIGN, ProtocolRange.until(Protocol.v1_9_2)),
            UPDATE_TIME(UPDATE_TIME_CLASS, Play.UPDATE_TIME),
            VEHICLE_MOVE(VEHICLE_MOVE_SERVER_CLASS, Play.VEHICLE_MOVE_SERVER, ProtocolRange.since(Protocol.v1_9)),
            VIEW_CENTRE(VIEW_CENTRE_CLASS, Play.VIEW_CENTRE, ProtocolRange.since(Protocol.v1_14)),
            VIEW_DISTANCE(VIEW_DISTANCE_CLASS, Play.VIEW_DISTANCE, ProtocolRange.since(Protocol.v1_14)),
            WINDOW_DATA(WINDOW_DATA_CLASS, Play.WINDOW_DATA),
            WINDOW_ITEMS(WINDOW_ITEMS_CLASS, Play.WINDOW_ITEMS),
            WORLD_BORDER(WORLD_BORDER_CLASS, Play.WORLD_BORDER),
            WORLD_EVENT(WORLD_EVENT_CLASS, Play.WORLD_EVENT),
            WORLD_PARTICLES(WORLD_PARTICLES_CLASS, Play.WORLD_PARTICLES)
            ;

            private final Class<?> nmsClass;
            private final byte packetId;
            private final ProtocolRange protocolRange;

            Server(Class<?> nmsClass, byte packetId) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = ProtocolRange.ALL;
            }

            Server(Class<?> nmsClass, byte packetId, ProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.packetId = packetId;
                this.protocolRange = protocolRange;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsClass;
            }

            @Override
            public byte getPacketId() {
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
            public ProtocolRange getRange() {
                return protocolRange;
            }
        }

    }
}
