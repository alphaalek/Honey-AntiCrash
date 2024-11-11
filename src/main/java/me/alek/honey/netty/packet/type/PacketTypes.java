package me.alek.honey.netty.packet.type;

import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LazyInit;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum PacketTypes implements PacketType {
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
    public String getKeyName() {
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

    private static final Array<PacketType> UNKNOWN_ARRAY = Array.from(UNKNOWN);

    public static boolean isUnknown(PacketType type) {
        return type == null || type == UNKNOWN;
    }

    private static final NetProtocol protocolVersion = NetProtocol.getProtocol();
    private static final List<PacketType> allPacketTypes = new LinkedList<>();
    private static final Map<Class<?>, Array<PacketType>> registeredPackets = new HashMap<>();
    private static final Map<String, List<PacketType>> registeredPacketsByClassName = new HashMap<>();
    private static final Map<String, List<PacketType>> registeredPacketsByName = new HashMap<>();
    private static final Map<PacketState, List<PacketType>> packetFromStateLookup = new HashMap<>();
    private static final IDRegistry idRegistry = new IDRegistry();

    public static boolean isServerbound(PacketType type) {
        return type.getBound().equals(PacketBound.SERVER);
    }

    public static boolean isClientbound(PacketType type) {
        return type.getBound().equals(PacketBound.CLIENT);
    }

    public static void load() {
        for (PacketState state : PacketState.values()) {
            packetFromStateLookup.put(state, new ArrayList<>());
        }

        register(PacketState.HANDSHAKE, PacketBound.SERVER, Handshake.Client.values());
        register(PacketState.LOGIN, PacketBound.CLIENT, Login.Server.values());
        register(PacketState.LOGIN, PacketBound.SERVER, Login.Client.values());
        register(PacketState.STATUS, PacketBound.CLIENT, Status.Server.values());
        register(PacketState.STATUS, PacketBound.SERVER, Status.Client.values());
        register(PacketState.PLAY, PacketBound.CLIENT, Play.Server.values());
        register(PacketState.PLAY, PacketBound.SERVER, Play.Client.values());
        register(PacketState.CONFIGURATION, PacketBound.CLIENT, Configuration.Server.values());
        register(PacketState.CONFIGURATION, PacketBound.SERVER, Configuration.Client.values());
    }

    private static void registerPacketDetails(PacketType type) {
        allPacketTypes.add(type);

        if (type instanceof RangedPacketType
                && !NetProtocol.matchProtocol((RangedPacketType) type)) {
            return;
        }

        type.getState().getTypes()[type.getBound().ordinal()].add(type);

        packetFromStateLookup.computeIfAbsent(type.getState(), d -> new ArrayList<>())
                .add(type);
        registeredPackets.computeIfAbsent(type.getNmsClass(), d -> new Array<>())
                .set(type.getState().ordinal(), type);
        registeredPacketsByName.computeIfAbsent(type.toString().toLowerCase(), d -> new ArrayList<>())
                .add(type);
        registeredPacketsByClassName.computeIfAbsent(type.getNmsClass().getSimpleName(), d -> new ArrayList<>())
                .add(type);
    }

    private static void register(PacketState state, PacketBound bound, PacketType[] types) {
        for (PacketType type : types) {
            registerPacketDetails(type);
        }
        sortPacketDetails(state, bound, types);
    }

    private static void sortPacketDetails(PacketState state, PacketBound bound, PacketType[] types) {
        Array<PacketType> typesArray = new Array<>();
        for (PacketType type : types) {
            if (type instanceof RangedPacketType
                    && !NetProtocol.matchProtocol((RangedPacketType) type)) {
                continue;
            }

            // skip bundle packet
            if (type == Play.Server.BUNDLE) {
                continue;
            }

            int id = type.getMinecraftID().get();
            if (id == -1) {
                Bukkit.getLogger().severe("Unknown ID: " + id + ", " + type);
            }
            typesArray.set(id, type);
        }

        state.getTypes()[bound.ordinal()].clear();
        state.getTypes()[bound.ordinal()].addAll(typesArray);

        if (typesArray.compact().size() != typesArray.size()) {
            List<Integer> indices = typesArray.streamNodes()
                    .filter(Array.ImmutableArrayNode::isNull)
                    .map(Array.ImmutableArrayNode::getIndex)
                    .collect(Collectors.toList());
            Bukkit.getLogger().severe("Done registering (state: " + state + ", bound: " + bound + " protocol) but found missing packet-type indices: " + indices);
            Bukkit.getLogger().severe(typesArray.toString());
        }
    }

    public static Array<PacketType> getTypesAvailableFor(NetProtocol protocol, PacketState state, PacketBound bound) {
        if (protocol.equals(NetProtocol.getProtocol())) {
            return getTypesAvailableFor(state, bound);
        }

        return Array.from(getAllPacketTypes().stream()
                .filter(t -> t.getBound().equals(bound) && t.getState().equals(state)).toArray());
    }

    public static Array<PacketType> getTypesAvailableFor(PacketState state, PacketBound bound) {
        return state.getTypes()[bound.ordinal()];
    }

    public static Array<PacketType> getTypesAvailableForBound(PacketBound bound) {
        Array<PacketType> types = new Array<>();
        for (PacketState state : PacketState.values()) {
            types.addAll(getTypesAvailableFor(state, bound));
        }
        return types;
    }

    public static Array<PacketType> getTypesAvailable() {
        Array<PacketType> types = new Array<>();
        for (PacketBound bound : PacketBound.values()) {
            types.addAll(getTypesAvailableForBound(bound));
        }
        return types;
    }

    public static PacketType getTypeFromKey(NetProtocol protocol, PacketState state, PacketBound bound, String key) {
        for (PacketType type : getTypesAvailableFor(protocol, state, bound)) {
            if (type.getKeyName() != null
                    && type.getKeyName().equals(key)) {
                return type;
            }
        }

        return PacketTypes.UNKNOWN;
    }

    public static PacketType getTypeFromKey(PacketState state, PacketBound bound, String key) {
        return getTypeFromKey(NetProtocol.getProtocol(), state, bound, key);
    }

    public enum NameGetStrategy {
        BY_NAME,

        BY_CLASS_NAME,

        EITHER
    }

    public static Map<Class<?>, Array<PacketType>> getRegisteredPackets() {
        return registeredPackets;
    }

    public static List<PacketType> getAllPacketTypes() {
        return allPacketTypes;
    }

    public static Optional<PacketType> getPacketType(Class<?> clazz, PacketState state) {
        return Optional.ofNullable(getPacketTypes(clazz).get(state.ordinal()));
    }

    public static Array<PacketType> getPacketTypes(Class<?> clazz) {
        return registeredPackets.getOrDefault(clazz, UNKNOWN_ARRAY);
    }

    public static Optional<PacketType> getPacketType(String name, Predicate<PacketType> filter, NameGetStrategy strategy) {
        Optional<PacketType> byName = Optional.ofNullable(registeredPacketsByName.get(name.toLowerCase()))
                .flatMap(l -> l.stream().filter(filter).findFirst());
        Optional<PacketType> byClassName = Optional.ofNullable(registeredPacketsByClassName.get(name.toLowerCase()))
                .flatMap(l -> l.stream().filter(filter).findFirst());

        switch (strategy) {
            case EITHER:
                return byName.isPresent() ? byName : byClassName;
            case BY_CLASS_NAME:
                return byClassName;
            case BY_NAME:
                return byName;
        }

        throw new IllegalStateException();
    }

    public static class Handshake {

        private static final Class<?> SET_PROTOCOL_CLASS = Reflection.getClass("{handshake}.PacketHandshakingInSetProtocol");

        public enum Client implements PacketType {

            SET_PROTOCOL(SET_PROTOCOL_CLASS, "intention");

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsPacket;
            private final int packetId = idRegistry.acquire();

            Client(Class<?> nmsPacket, String keyName) {
                this.nmsPacket = nmsPacket;
                this.keyName = keyName;
            }

            @Override
            public Class<?> getNmsClass() {
                return nmsPacket;
            }

            @Override
            public LazyInit<Integer> getMinecraftID() {
                return minecraftIdInit;
            }

            public String getKeyName() {
                return keyName;
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
        private static Class<?> COOKIE_RESPONSE_CLASS;
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
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                COOKIE_RESPONSE_CLASS = Reflection.getClass("{cookie}.ServerboundCookieResponsePacket");
            }
        }

        public enum Client implements RangedPacketType {
            COOKIE_RESPONSE(COOKIE_RESPONSE_CLASS, "cookie_response", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLIENT_CLASS, "custom_query", NetProtocolRange.between(NetProtocol.v1_13, NetProtocol.v1_20_1)),
            CUSTOM_QUERY_ANSWER(CUSTOM_QUERY_ANSWER_CLASS, "custom_query_answer", NetProtocolRange.since(NetProtocol.v1_20_2)),
            ENCRYPTION_BEGIN(ENCRYPTION_BEGIN_CLASS, "key"),
            LOGIN_ACKNOWLEDGED(LOGIN_ACKNOWLEDGED_CLASS, "login_acknowledged", NetProtocolRange.since(NetProtocol.v1_20_2)),
            LOGIN_START(LOGIN_START_CLASS, "hello");

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsPacket;
            private final int packetId = idRegistry.acquire();
            private final NetProtocolRange protocolRange;

            Client(Class<?> nmsPacket, String keyName) {
                this(nmsPacket, keyName, NetProtocolRange.ALL);
            }

            Client(Class<?> nmsPacket, String keyName, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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
        private static Class<?> COOKIE_REQUEST_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_SERVER_CLASS;

        static {
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_13)) {
                CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{login}.PacketLoginOutCustomPayload");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                COOKIE_REQUEST_CLASS = Reflection.getClass("{cookie}.ClientboundCookieRequestPacket");
            }
        }

        public enum Server implements RangedPacketType {
            DISCONNECT(DISCONNECT_CLASS, "login_disconnect"),
            ENCRYPTION_REQUEST(ENCRYPTION_REQUEST_CLASS, "hello"),
            LOGIN_SUCCESS(LOGIN_SUCCESS_CLASS, "game_profile"),
            COOKIE_REQUEST(COOKIE_REQUEST_CLASS, "cookie_request", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, "custom_query", NetProtocolRange.since(NetProtocol.v1_13)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, "login_compression");

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsPacket;
            private final int packetId = idRegistry.acquire();
            private final NetProtocolRange protocolRange;

            Server(Class<?> nmsPacket, String keyName) {
                this(nmsPacket, keyName, NetProtocolRange.ALL);
            }

            Server(Class<?> nmsPacket, String keyName, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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

        private static final Class<?> PING_CLASS = Reflection.getUnsureClass("{status}.PacketStatusInPing", "{ping}.ServerboundPingRequestPacket");
        private static final Class<?> START_CLASS = Reflection.getClass("{status}.PacketStatusInStart");

        public enum Client implements RangedPacketType {

            PING(PING_CLASS, "ping_request"),
            START(START_CLASS, "status_request");

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsClass;
            private final NetProtocolRange protocolRange;
            private final int packetId = idRegistry.acquire();

            Client(Class<?> nmsClass, String keyName) {
                this(nmsClass, keyName, NetProtocolRange.ALL);
            }

            Client(Class<?> nmsClass, String keyName, NetProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }
        }

        private static final Class<?> PONG_CLASS = Reflection.getUnsureClass("{status}.PacketStatusOutPong", "{ping}.ClientboundPongResponsePacket");
        private static final Class<?> SERVER_INFO_CLASS = Reflection.getClass("{status}.PacketStatusOutServerInfo");

        public enum Server implements RangedPacketType {

            PONG(PONG_CLASS, "pong_response"),
            SERVER_INFO(SERVER_INFO_CLASS, "status_response");

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsClass;
            private final NetProtocolRange protocolRange;
            private final int packetId = idRegistry.acquire();

            Server(Class<?> nmsPacket, String keyName) {
                this(nmsPacket, keyName, NetProtocolRange.ALL);
            }

            Server(Class<?> nmsClass, String keyName, NetProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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

            @Override
            public NetProtocolRange getRange() {
                return protocolRange;
            }
        }
    }

    public static class Play {

        private static final Class<?> ANIMATION_CLASS = Reflection.getClass("{play}.PacketPlayInArmAnimation");
        private static final Class<?> CHAT_MESSAGE_CLASS = Reflection.getClass("{play}.PacketPlayInChat");
        private static final Class<?> CLICK_WINDOW_BUTTON_CLASS = Reflection.getClass("{play}.PacketPlayInEnchantItem");
        private static final Class<?> CLICK_WINDOW_CLASS = Reflection.getClass("{play}.PacketPlayInWindowClick");
        private static final Class<?> CLIENT_COMMAND_CLASS = Reflection.getClass("{play}.PacketPlayInClientCommand");
        private static final Class<?> CLOSE_WINDOW_CLASS = Reflection.getClass("{play}.PacketPlayInCloseWindow");
        private static final Class<?> CREATIVE_INVENTORY_ACTION_CLASS = Reflection.getClass("{play}.PacketPlayInSetCreativeSlot");
        private static final Class<?> CUSTOM_PAYLOAD_CLASS = Reflection.getUnsureClass("{play}.PacketPlayInCustomPayload", "{common}.ServerboundCustomPayloadPacket");
        private static final Class<?> ENTITY_ACTION_CLASS = Reflection.getClass("{play}.PacketPlayInEntityAction");
        private static final Class<?> HELD_ITEM_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayInHeldItemSlot");
        private static final Class<?> INTERACT_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayInUseEntity");
        private static final Class<?> KEEP_ALIVE_CLIENT_CLASS = Reflection.getUnsureClass("{play}.PacketPlayInKeepAlive", "{common}.ServerboundKeepAlivePacket");
        private static final Class<?> PLAYER_ABILITIES_CLASS = Reflection.getClass("{play}.PacketPlayInAbilities");
        private static final Class<?> PLAYER_DIGGING_CLASS = Reflection.getClass("{play}.PacketPlayInBlockDig");
        private static final Class<?> RESOURCE_PACK_STATUS_CLASS = Reflection.getUnsureClass("{play}.PacketPlayInResourcePackStatus", "{common}.ServerboundResourcePackPacket");
        private static final Class<?> SPECTATE_CLASS = Reflection.getClass("{play}.PacketPlayInSpectate");
        private static final Class<?> STEER_VEHICLE_CLASS = Reflection.getClass("{play}.PacketPlayInSteerVehicle");
        private static final Class<?> TAB_COMPLETE_CLASS = Reflection.getClass("{play}.PacketPlayInTabComplete");
        private static final Class<?> UPDATE_SIGN_CLASS = Reflection.getClass("{play}.PacketPlayInUpdateSign");
        private static final Class<?> BLOCK_PLACE_CLASS = Reflection.getClass("{play}.PacketPlayInBlockPlace");
        private static Class<?> ADVANCEMENTS_CLASS;
        private static Class<?> AUTO_RECIPE_CLASS;
        private static Class<?> BOAT_MOVE_CLASS;
        private static Class<?> BOOK_EDIT_CLASS;
        private static Class<?> CHAT_ACKNOWLEDGED_CLASS;
        private static Class<?> CHAT_COMMAND_CLASS;
        private static Class<?> CHAT_COMMAND_SIGNED_CLASS;
        private static Class<?> CHAT_PREVIEW_CLIENT_CLASS;
        private static Class<?> CHAT_SESSION_UPDATE_CLASS;
        private static Class<?> CHUNK_BATCH_RECEIVED_CLASS;
        private static Class<?> CLIENT_INFORMATION_CLASS;
        private static Class<?> CONFIGURATION_START_ACKNOWLEDGED_CLASS;
        private static Class<?> CONTAINER_SLOT_STATE_CHANGED_CLASS;
        private static Class<?> COOKIE_RESPONSE_CLASS;
        private static Class<?> DEBUG_SAMPLE_SUBSCRIPTION_CLASS;
        private static Class<?> DIFFICULTY_CHANGE_CLASS;
        private static Class<?> DIFFICULTY_LOCK_CLASS;
        private static Class<?> ENTITY_NBT_QUERY_CLASS;
        private static Class<?> GROUND_CLASS;
        private static Class<?> ITEM_NAME_CLASS;
        private static Class<?> JIGSAW_GENERATE_CLASS;
        private static Class<?> PICK_ITEM_CLASS;
        private static Class<?> PING_REQUEST_CLASS;
        private static Class<?> PLAYER_MOVEMENT_CLASS;
        private static Class<?> PLAYER_POSITION_AND_ROTATION_CLASS;
        private static Class<?> PLAYER_POSITION_CLASS;
        private static Class<?> PLAYER_ROTATION_CLASS;
        private static Class<?> PONG_RESPONSE_CLASS;
        private static Class<?> RECIPE_DISPLAYED_CLASS;
        private static Class<?> RECIPE_SETTINGS_CLASS;
        private static Class<?> SELECT_TRADE_CLASS;
        private static Class<?> SET_BEACON_EFFECT_CLASS;
        private static Class<?> SET_COMMAND_MINECART_CLASS;
        private static Class<?> SET_JIGSAW_CLASS;
        private static Class<?> STRUCT_CLASS;
        private static Class<?> TELEPORT_CONFIRM_CLASS;
        private static Class<?> TILE_NBT_QUERY_CLASS;
        private static Class<?> TRANSACTION_CLASS;
        private static Class<?> UPDATE_COMMAND_BLOCK_CLASS;
        private static Class<?> USE_ITEM_CLASS;
        private static Class<?> VEHICLE_MOVE_CLASS;

        static {
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                TRANSACTION_CLASS = Reflection.getClass("{play}.PacketPlayInTransaction");
                PLAYER_MOVEMENT_CLASS = Reflection.getClass("{play}.PacketPlayInFlying");
                PLAYER_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInLook");
                PLAYER_POSITION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPosition");
                PLAYER_POSITION_AND_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPositionLook");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_9)) {
                VEHICLE_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayInVehicleMove");
                BOAT_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayInBoatMove");
                USE_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayInUseItem");
                TELEPORT_CONFIRM_CLASS = Reflection.getClass("{play}.PacketPlayInTeleportAccept");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_12)) {
                AUTO_RECIPE_CLASS = Reflection.getClass("{play}.PacketPlayInAutoRecipe");
                RECIPE_DISPLAYED_CLASS = Reflection.getClass("{play}.PacketPlayInRecipeDisplayed");
                ADVANCEMENTS_CLASS = Reflection.getClass("{play}.PacketPlayInAdvancements");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_13)) {
                TILE_NBT_QUERY_CLASS = Reflection.getClass("{play}.PacketPlayInTileNBTQuery");
                ENTITY_NBT_QUERY_CLASS = Reflection.getClass("{play}.PacketPlayInEntityNBTQuery");
                BOOK_EDIT_CLASS = Reflection.getClass("{play}.PacketPlayInBEdit");
                PICK_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayInPickItem");
                ITEM_NAME_CLASS = Reflection.getClass("{play}.PacketPlayInItemName");
                SELECT_TRADE_CLASS = Reflection.getClass("{play}.PacketPlayInTrSel");
                SET_BEACON_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayInBeacon");
                UPDATE_COMMAND_BLOCK_CLASS = Reflection.getClass("{play}.PacketPlayInSetCommandBlock");
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
                PLAYER_POSITION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPosition");
                PLAYER_POSITION_AND_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPositionLook");
                PLAYER_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInLook");
                GROUND_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$d");
                PONG_RESPONSE_CLASS = Reflection.getUnsureClass("{play}.ServerboundPongPacket", "{common}.ServerboundPongPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19)) {
                CHAT_COMMAND_CLASS = Reflection.getClass("{play}.ServerboundChatCommandPacket");
                if (protocolVersion.isOlderThan(NetProtocol.v1_19_3)) {
                    CHAT_PREVIEW_CLIENT_CLASS = Reflection.getClass("{play}.ServerboundChatPreviewPacket");
                }
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_1)) {
                CHAT_ACKNOWLEDGED_CLASS = Reflection.getClass("{play}.ServerboundChatAckPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_3)) {
                CHAT_SESSION_UPDATE_CLASS = Reflection.getClass("{play}.ServerboundChatSessionUpdatePacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CHUNK_BATCH_RECEIVED_CLASS = Reflection.getClass("{play}.ServerboundChunkBatchReceivedPacket");
                CLIENT_INFORMATION_CLASS = Reflection.getClass("{common}.ServerboundClientInformationPacket");
                CONFIGURATION_START_ACKNOWLEDGED_CLASS = Reflection.getClass("{play}.ServerboundConfigurationAcknowledgedPacket");
                PING_REQUEST_CLASS = Reflection.getUnsureClass("{status}.PacketStatusInPing", "{ping}.ServerboundPingRequestPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_4)) {
                CONTAINER_SLOT_STATE_CHANGED_CLASS = Reflection.getClass("{play}.ServerboundContainerSlotStateChangedPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                CHAT_COMMAND_SIGNED_CLASS = Reflection.getClass("{play}.ServerboundChatCommandSignedPacket");
                COOKIE_RESPONSE_CLASS = Reflection.getClass("{cookie}.ServerboundCookieResponsePacket");
                DEBUG_SAMPLE_SUBSCRIPTION_CLASS = Reflection.getClass("{play}.ServerboundDebugSampleSubscriptionPacket");
            }
            if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                CLIENT_INFORMATION_CLASS = Reflection.getClass("{play}.PacketPlayInSettings");
            }
        }

        public enum Client implements RangedPacketType {

            ADVANCEMENTS(ADVANCEMENTS_CLASS, "seen_advancements", NetProtocolRange.since(NetProtocol.v1_12)),
            ANIMATION(ANIMATION_CLASS, "swing"),
            AUTO_RECIPE(AUTO_RECIPE_CLASS, "place_recipe", NetProtocolRange.since(NetProtocol.v1_12)),
            BOAT_MOVE(BOAT_MOVE_CLASS, "paddle_boat", NetProtocolRange.since(NetProtocol.v1_9)),
            BOOK_EDIT(BOOK_EDIT_CLASS, "edit_book", NetProtocolRange.since(NetProtocol.v1_13)),
            CHAT_ACKNOWLEDGED(CHAT_ACKNOWLEDGED_CLASS, "chat_ack", NetProtocolRange.since(NetProtocol.v1_19_1)),
            CHAT_COMMAND(CHAT_COMMAND_CLASS, "chat_command", NetProtocolRange.since(NetProtocol.v1_19)),
            CHAT_COMMAND_SIGNED(CHAT_COMMAND_SIGNED_CLASS, "chat_command_signed", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CHAT_MESSAGE(CHAT_MESSAGE_CLASS, "chat"),
            CHAT_PREVIEW(CHAT_PREVIEW_CLIENT_CLASS, "chat_preview", NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            CHAT_SESSION_UPDATE(CHAT_SESSION_UPDATE_CLASS, "chat_session_update", NetProtocolRange.since(NetProtocol.v1_19_3)),
            CHUNK_BATCH_RECEIVED(CHUNK_BATCH_RECEIVED_CLASS, "chunk_batch_received", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CLICK_WINDOW(CLICK_WINDOW_CLASS, "container_click"),
            CLICK_WINDOW_BUTTON(CLICK_WINDOW_BUTTON_CLASS, "container_button_click"),
            CLIENT_COMMAND(CLIENT_COMMAND_CLASS, "client_command"),
            CLIENT_INFORMATION(CLIENT_INFORMATION_CLASS, "client_information"),
            CLOSE_WINDOW(CLOSE_WINDOW_CLASS, "container_close"),
            CONFIGURATION_START_ACKNOWLEDGED(CONFIGURATION_START_ACKNOWLEDGED_CLASS, "configuration_acknowledged", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CONTAINER_SLOT_STATE_CHANGED(CONTAINER_SLOT_STATE_CHANGED_CLASS, "container_slot_state_changed", NetProtocolRange.since(NetProtocol.v1_20_4)),
            COOKIE_RESPONSE(COOKIE_RESPONSE_CLASS, "cookie_response", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CREATIVE_INVENTORY_ACTION(CREATIVE_INVENTORY_ACTION_CLASS, "set_creative_mode_slot"),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, "custom_payload"),
            DEBUG_SAMPLE_SUBSCRIPTION(DEBUG_SAMPLE_SUBSCRIPTION_CLASS, "debug_sample_subscription", NetProtocolRange.since(NetProtocol.v1_20_6)),
            DIFFICULTY_CHANGE(DIFFICULTY_CHANGE_CLASS, "change_difficulty", NetProtocolRange.since(NetProtocol.v1_14)),
            DIFFICULTY_LOCK(DIFFICULTY_LOCK_CLASS, "lock_difficulty", NetProtocolRange.since(NetProtocol.v1_14)),
            ENTITY_ACTION(ENTITY_ACTION_CLASS, "player_command"),
            ENTITY_NBT_QUERY(ENTITY_NBT_QUERY_CLASS, "entity_tag_query", NetProtocolRange.since(NetProtocol.v1_13)),
            GROUND(GROUND_CLASS, "move_player_status_only", NetProtocolRange.since(NetProtocol.v1_17)),
            HELD_ITEM_CHANGE(HELD_ITEM_CHANGE_CLASS, "set_carried_item"),
            INTERACT_ENTITY(INTERACT_ENTITY_CLASS, "interact"),
            ITEM_NAME(ITEM_NAME_CLASS, "rename_item", NetProtocolRange.since(NetProtocol.v1_13)),
            JIGSAW_GENERATE(JIGSAW_GENERATE_CLASS, "jigsaw_generate", NetProtocolRange.since(NetProtocol.v1_16_1)),
            KEEP_ALIVE(KEEP_ALIVE_CLIENT_CLASS, "keep_alive"),
            PICK_ITEM(PICK_ITEM_CLASS, "pick_item", NetProtocolRange.since(NetProtocol.v1_13)),
            PING(PING_REQUEST_CLASS, "ping_request", NetProtocolRange.since(NetProtocol.v1_20_2)),
            PLAYER_ABILITIES(PLAYER_ABILITIES_CLASS, "player_abilities"),
            PLAYER_DIGGING(PLAYER_DIGGING_CLASS, "player_action"),
            PLAYER_MOVEMENT(PLAYER_MOVEMENT_CLASS, "move_player_status_only", NetProtocolRange.until(NetProtocol.v1_16_5)),
            PLAYER_POSITION(PLAYER_POSITION_CLASS, "move_player_pos"),
            PLAYER_POS_ROT(PLAYER_POSITION_AND_ROTATION_CLASS, "move_player_pos_rot"),
            PLAYER_ROTATION(PLAYER_ROTATION_CLASS, "move_player_rot"),
            PONG(PONG_RESPONSE_CLASS, "pong", NetProtocolRange.since(NetProtocol.v1_17)),
            RECIPE_DISPLAYED(RECIPE_DISPLAYED_CLASS, "recipe_book_seen_recipe", NetProtocolRange.since(NetProtocol.v1_12)),
            RECIPE_SETTINGS(RECIPE_SETTINGS_CLASS, "recipe_book_change_settings", NetProtocolRange.since(NetProtocol.v1_16_2)),
            RESOURCE_PACK_STATUS(RESOURCE_PACK_STATUS_CLASS, "resource_pack"),
            SELECT_TRADE(SELECT_TRADE_CLASS, "select_trade", NetProtocolRange.since(NetProtocol.v1_13)),
            SET_BEACON_EFFECT(SET_BEACON_EFFECT_CLASS, "set_beacon", NetProtocolRange.since(NetProtocol.v1_13)),
            SET_COMMAND_MINECART(SET_COMMAND_MINECART_CLASS, "set_command_minecart", NetProtocolRange.since(NetProtocol.v1_13)),
            SET_JIGSAW(SET_JIGSAW_CLASS, "set_jigsaw_block", NetProtocolRange.since(NetProtocol.v1_14)),
            SPECTATE(SPECTATE_CLASS, "teleport_to_entity"),
            STEER_VEHICLE(STEER_VEHICLE_CLASS, "player_input"),
            STRUCT(STRUCT_CLASS, "set_structure_block", NetProtocolRange.since(NetProtocol.v1_13)),
            TAB_COMPLETE(TAB_COMPLETE_CLASS, "command_suggestion"),
            TELEPORT_CONFIRM(TELEPORT_CONFIRM_CLASS, "accept_teleportation", NetProtocolRange.since(NetProtocol.v1_9)),
            TILE_NBT_QUERY(TILE_NBT_QUERY_CLASS, "block_entity_tag_query", NetProtocolRange.since(NetProtocol.v1_13)),
            TRANSACTION(TRANSACTION_CLASS, "container_ack", NetProtocolRange.until(NetProtocol.v1_16_5)),
            UPDATE_COMMAND_BLOCK(UPDATE_COMMAND_BLOCK_CLASS, "set_command_block", NetProtocolRange.since(NetProtocol.v1_13)),
            UPDATE_SIGN(UPDATE_SIGN_CLASS, "sign_update"),
            USE_ITEM(USE_ITEM_CLASS, "use_item", NetProtocolRange.since(NetProtocol.v1_9)),
            BLOCK_PLACE(BLOCK_PLACE_CLASS, "use_item_on"),
            VEHICLE_MOVE(VEHICLE_MOVE_CLASS, "move_vehicle", NetProtocolRange.since(NetProtocol.v1_9)),
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsClass;
            private final int packetId = idRegistry.acquire();
            private final NetProtocolRange protocolRange;

            Client(Class<?> nmsClass, String keyName) {
                this(nmsClass, keyName, NetProtocolRange.ALL);
            }

            Client(Class<?> nmsClass, String keyName, NetProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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
        private static final Class<?> ATTACH_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutAttachEntity");
        private static final Class<?> BLOCK_ACTION_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockAction");
        private static final Class<?> BLOCK_BREAK_ANIMATION_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockBreakAnimation");
        private static final Class<?> BLOCK_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockChange");
        private static final Class<?> CAMERA_CLASS = Reflection.getClass("{play}.PacketPlayOutCamera");
        private static final Class<?> CLOSE_WINDOW_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutCloseWindow");
        private static final Class<?> COLLECT_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayOutCollect");
        private static final Class<?> CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getUnsureClass("{play}.PacketPlayOutCustomPayload", "{common}.ClientboundCustomPayloadPacket");;
        private static final Class<?> DISCONNECT_CLASS = Reflection.getUnsureClass("{play}.PacketPlayOutKickDisconnect", "{common}.ClientboundDisconnectPacket");
        private static final Class<?> ENTITY_ANIMATION_CLASS = Reflection.getClass("{play}.PacketPlayOutAnimation");
        private static final Class<?> ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutEntity");
        private static final Class<?> ENTITY_DESTROY_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityDestroy");
        private static final Class<?> ENTITY_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityEffect");
        private static final Class<?> ENTITY_EQUIPMENT_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityEquipment");
        private static final Class<?> ENTITY_LOOK_CLASS = Reflection.getClass("{play}.PacketPlayOutEntity$PacketPlayOutEntityLook");
        private static final Class<?> ENTITY_METADATA_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityMetadata");
        private static final Class<?> ENTITY_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayOutEntity$PacketPlayOutRelEntityMove");
        private static final Class<?> ENTITY_MOVE_LOOK_CLASS = Reflection.getClass("{play}.PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook");
        private static final Class<?> ENTITY_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityHeadRotation");
        private static final Class<?> ENTITY_STATUS_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityStatus");
        private static final Class<?> ENTITY_TELEPORT_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityTeleport");
        private static final Class<?> ENTITY_VELOCITY_CLASS = Reflection.getClass("{play}.PacketPlayOutEntityVelocity");
        private static final Class<?> EXPERIENCE_CLASS = Reflection.getClass("{play}.PacketPlayOutExperience");
        private static final Class<?> EXPLOSION_CLASS = Reflection.getClass("{play}.PacketPlayOutExplosion");
        private static final Class<?> GAME_STATE_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayOutGameStateChange");
        private static final Class<?> HELD_ITEM_CHANGE_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutHeldItemSlot");
        private static final Class<?> JOIN_GAME_CLASS = Reflection.getClass("{play}.PacketPlayOutLogin");
        private static final Class<?> KEEP_ALIVE_SERVER_CLASS = Reflection.getUnsureClass("{play}.PacketPlayOutKeepAlive", "{common}.ClientboundKeepAlivePacket");
        private static final Class<?> MAP_ITEM_DATA_CLASS = Reflection.getClass("{play}.PacketPlayOutMap");
        private static final Class<?> MULTI_BLOCK_CHANGE_CLASS = Reflection.getClass("{play}.PacketPlayOutMultiBlockChange");
        private static final Class<?> OPEN_SIGN_EDITOR_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenSignEditor");
        private static final Class<?> OPEN_WINDOW_CLASS = Reflection.getClass("{play}.PacketPlayOutOpenWindow");
        private static final Class<?> PLAYER_POSITION_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutPosition");
        private static final Class<?> REMOVE_ENTITY_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayOutRemoveEntityEffect");
        private static final Class<?> RESPAWN_CLASS = Reflection.getClass("{play}.PacketPlayOutRespawn");
        private static final Class<?> SCOREBOARD_DISPLAY_OBJECTIVE_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardDisplayObjective");
        private static final Class<?> SCOREBOARD_OBJECTIVE_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardObjective");
        private static final Class<?> SCOREBOARD_SCORE_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardScore");
        private static final Class<?> SCOREBOARD_TEAM_CLASS = Reflection.getClass("{play}.PacketPlayOutScoreboardTeam");
        private static final Class<?> SERVER_DIFFICULTY_CLASS = Reflection.getClass("{play}.PacketPlayOutServerDifficulty");
        private static final Class<?> SET_SLOT_CLASS = Reflection.getClass("{play}.PacketPlayOutSetSlot");
        private static final Class<?> SOUND_CLASS = Reflection.getClass("{play}.PacketPlayOutNamedSoundEffect");
        private static final Class<?> SPAWN_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntity");
        private static final Class<?> SPAWN_EXPERIENCE_ORB_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntityExperienceOrb");
        private static final Class<?> SPAWN_POSITION_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnPosition");
        private static final Class<?> STATISTICS_CLASS = Reflection.getClass("{play}.PacketPlayOutStatistic");
        private static final Class<?> TAB_COMPLETE_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutTabComplete");
        private static final Class<?> TAB_LIST_CLASS = Reflection.getClass("{play}.PacketPlayOutPlayerListHeaderFooter");
        private static final Class<?> TILE_ENTITY_DATA_CLASS = Reflection.getClass("{play}.PacketPlayOutTileEntityData");
        private static final Class<?> TIME_UPDATE_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateTime");
        private static final Class<?> UPDATE_ATTRIBUTES_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateAttributes");
        private static final Class<?> UPDATE_HEALTH_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateHealth");
        private static final Class<?> WINDOW_DATA_CLASS = Reflection.getClass("{play}.PacketPlayOutWindowData");
        private static final Class<?> WINDOW_ITEMS_CLASS = Reflection.getClass("{play}.PacketPlayOutWindowItems");
        private static final Class<?> WORLD_EVENT_CLASS = Reflection.getClass("{play}.PacketPlayOutWorldEvent");
        private static final Class<?> WORLD_PARTICLES_CLASS = Reflection.getClass("{play}.PacketPlayOutWorldParticles");
        private static Class<?> ADD_VIBRATION_SIGNAL_CLASS;
        private static Class<?> ADVANCEMENTS_SERVER_CLASS;
        private static Class<?> AUTO_RECIPE_SERVER_CLASS;
        private static Class<?> PLAYER_SLEEP_CLASS;
        private static Class<?> BLOCK_BROKE_ACK_CLASS;
        private static Class<?> BLOCK_CHANGED_ACK_CLASS;
        private static Class<?> BOSS_EVENT_CLASS;
        private static Class<?> BUNDLE_CLASS;
        private static Class<?> BUNDLE_DELIMITER_CLASS;
        private static Class<?> CHAT_PREVIEW_SERVER_CLASS;
        private static Class<?> CHUNKS_BIOMES_CLASS;
        private static Class<?> CHUNK_BATCH_FINISHED_CLASS;
        private static Class<?> CHUNK_BATCH_START_CLASS;
        private static Class<?> CLEAR_TITLES_CLASS;
        private static Class<?> COMBAT_EVENT_CLASS;
        private static Class<?> COMMANDS_CLASS;
        private static Class<?> COOKIE_REQUEST_CLASS;
        private static Class<?> CUSTOM_CHAT_COMPLETIONS_CLASS;
        private static Class<?> CUSTOM_SOUND_EFFECT_CLASS;
        private static Class<?> DAMAGE_EVENT_CLASS;
        private static Class<?> DEBUG_SAMPLE_CLASS;
        private static Class<?> DELETE_CHAT_MESSAGE_CLASS;
        private static Class<?> DISGUISED_CHAT_CLASS;
        private static Class<?> ENTITY_SOUND_CLASS;
        private static Class<?> HURT_ANIMATION_CLASS;
        private static Class<?> INITIALIZE_BORDER_CLASS;
        private static Class<?> LEVEL_CHUNK_WITH_LIGHT_CLASS;
        private static Class<?> LIGHT_UPDATE_CLASS;
        private static Class<?> LOOK_AT_CLASS;
        private static Class<?> MAP_BULK_CHUNK_CLASS;
        private static Class<?> MAP_CHUNK_CLASS;
        private static Class<?> MOUNT_CLASS;
        private static Class<?> SPAWN_NAMED_ENTITY_CLASS;
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
        private static Class<?> PLAYER_INFO_CLASS;
        private static Class<?> PLAYER_INFO_REMOVE_CLASS;
        private static Class<?> PLAYER_INFO_UPDATE_CLASS;
        private static Class<?> PONG_CLASS;
        private static Class<?> PROJECTILE_POWER_CLASS;
        private static Class<?> RECIPES_CLASS;
        private static Class<?> RECIPE_UPDATE_CLASS;
        private static Class<?> RESET_SCORE_CLASS;
        private static Class<?> RESOURCE_PACK_POP_CLASS;
        private static Class<?> RESOURCE_PACK_PUSH_CLASS;
        private static Class<?> RESOURCE_PACK_SEND_CLASS;
        private static Class<?> SELECT_ADVANCEMENT_TAB_CLASS;
        private static Class<?> SERVER_DATA_CLASS;
        private static Class<?> SET_ACTION_BAR_TEXT_CLASS;
        private static Class<?> SET_BORDER_SIZE_CLASS;
        private static Class<?> SET_BORDER_WARNING_DELAY_CLASS;
        private static Class<?> SET_BORDER_WARNING_DISTANCE_CLASS;
        private static Class<?> SET_COMPRESSION_CLASS;
        private static Class<?> SET_COOLDOWN_CLASS;
        private static Class<?> SET_DISPLAY_CHAT_PREVIEW_CLASS;
        private static Class<?> SET_SIMULATION_DISTANCE_CLASS;
        private static Class<?> SET_SUBTITLE_TEXT_CLASS;
        private static Class<?> SET_TITLES_ANIMATION_CLASS;
        private static Class<?> SET_TITLE_TEXT_CLASS;
        private static Class<?> SPAWN_ENTITY_LIVING_CLASS;
        private static Class<?> SPAWN_ENTITY_PAINTING_CLASS;
        private static Class<?> SPAWN_GLOBAL_ENTITY_CLASS;
        private static Class<?> START_CONFIGURATION_CLASS;
        private static Class<?> STOP_SOUND_CLASS;
        private static Class<?> STORE_COOKIE_CLASS;
        private static Class<?> SYSTEM_CHAT_CLASS;
        private static Class<?> TICKING_STATE_CLASS;
        private static Class<?> TICKING_STEP_CLASS;
        private static Class<?> TITLE_CLASS;
        private static Class<?> TRANSACTION_SERVER_CLASS;
        private static Class<?> TRANSFER_CLASS;
        private static Class<?> UNLOAD_CHUNK_CLASS;
        private static Class<?> UPDATE_ENABLED_FEATURES_CLASS;
        private static Class<?> UPDATE_ENTITY_NBT_CLASS;
        private static Class<?> UPDATE_SIGN_SERVER_CLASS;
        private static Class<?> UPDATE_TAGS_CLASS;
        private static Class<?> VEHICLE_MOVE_SERVER_CLASS;
        private static Class<?> VIEW_CENTRE_CLASS;
        private static Class<?> VIEW_DISTANCE_CLASS;
        private static Class<?> WORLD_BORDER_CENTER_CLASS;
        private static Class<?> WORLD_BORDER_CLASS;
        private static Class<?> WORLD_BORDER_LERP_SIZE_CLASS;

        static {
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                MAP_BULK_CHUNK_CLASS = Reflection.getClass("{play}.PacketPlayOutMapChunkBulk");
                UPDATE_ENTITY_NBT_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateEntityNBT");
                SET_COMPRESSION_CLASS = Reflection.getClass("{play}.PacketPlayOutSetCompression");
            }
            if (protocolVersion.isBetween(NetProtocol.v1_9, NetProtocol.v1_19_2)) {
                CUSTOM_SOUND_EFFECT_CLASS = Reflection.getClass("{play}.PacketPlayOutCustomSoundEffect");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_9_2)) {
                UPDATE_SIGN_SERVER_CLASS = Reflection.getClass("{play}.PacketPlayOutUpdateSign");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_13_2)) {
                PLAYER_SLEEP_CLASS = Reflection.getClass("{play}.PacketPlayOutBed");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_15_2)) {
                SPAWN_GLOBAL_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutSpawnEntityWeather");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                COMBAT_EVENT_CLASS = Reflection.getClass("{play}.PacketPlayOutCombatEvent");
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
                BOSS_EVENT_CLASS = Reflection.getClass("{play}.PacketPlayOutBoss");
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
                    UPDATE_TAGS_CLASS = Reflection.getClass("{play}.PacketPlayOutTags");
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
                BLOCK_BROKE_ACK_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockBreak");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_17)) {
                CLEAR_TITLES_CLASS = Reflection.getClass("{play}.ClientboundClearTitlesPacket");
                INITIALIZE_BORDER_CLASS = Reflection.getClass("{play}.ClientboundInitializeBorderPacket");
                PING_CLASS = Reflection.getUnsureClass("{play}.ClientboundPingPacket", "{common}.ClientboundPingPacket");
                PLAYER_COMBAT_END_CLASS = Reflection.getClass("{play}.ClientboundPlayerCombatEndPacket");
                PLAYER_COMBAT_ENTER_CLASS = Reflection.getClass("{play}.ClientboundPlayerCombatEnterPacket");
                PLAYER_COMBAT_KILL_CLASS = Reflection.getClass("{play}.ClientboundPlayerCombatKillPacket");
                SET_ACTION_BAR_TEXT_CLASS = Reflection.getClass("{play}.ClientboundSetActionBarTextPacket");
                WORLD_BORDER_CENTER_CLASS = Reflection.getClass("{play}.ClientboundSetBorderCenterPacket");
                WORLD_BORDER_LERP_SIZE_CLASS = Reflection.getClass("{play}.ClientboundSetBorderLerpSizePacket");
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
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_18)) {
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
                DELETE_CHAT_MESSAGE_CLASS = Reflection.getClass("{play}.ClientboundDeleteChatPacket");
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
            } else {
                PLAYER_INFO_CLASS = Reflection.getClass("{play}.PacketPlayOutPlayerInfo");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_19_4)) {
                BUNDLE_CLASS = Reflection.getClass("{play}.ClientboundBundlePacket");
                BUNDLE_DELIMITER_CLASS = Reflection.getUnsureClass("{play}.ClientboundBundleDelimiterPacket", "{nms}.network.protocol.BundleDelimiterPacket");
                CHUNKS_BIOMES_CLASS = Reflection.getClass("{play}.ClientboundChunksBiomesPacket");
                DAMAGE_EVENT_CLASS = Reflection.getClass("{play}.ClientboundDamageEventPacket");
                HURT_ANIMATION_CLASS = Reflection.getClass("{play}.ClientboundHurtAnimationPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CHUNK_BATCH_FINISHED_CLASS = Reflection.getClass("{play}.ClientboundChunkBatchFinishedPacket");
                CHUNK_BATCH_START_CLASS = Reflection.getClass("{play}.ClientboundChunkBatchStartPacket");
                START_CONFIGURATION_CLASS = Reflection.getClass("{play}.ClientboundStartConfigurationPacket");
                PONG_CLASS = Reflection.getUnsureClass("{status}.PacketStatusOutPong", "{ping}.ClientboundPongResponsePacket");
                UPDATE_TAGS_CLASS = Reflection.getClass("{common}.ClientboundUpdateTagsPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_4)) {
                RESET_SCORE_CLASS = Reflection.getClass("{play}.ClientboundResetScorePacket");
                RESOURCE_PACK_POP_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPopPacket");
                RESOURCE_PACK_PUSH_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPushPacket");
                TICKING_STATE_CLASS = Reflection.getClass("{play}.ClientboundTickingStatePacket");
                TICKING_STEP_CLASS = Reflection.getClass("{play}.ClientboundTickingStepPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                COOKIE_REQUEST_CLASS = Reflection.getClass("{cookie}.ClientboundCookieRequestPacket");
                DEBUG_SAMPLE_CLASS = Reflection.getClass("{play}.ClientboundDebugSamplePacket");
                PROJECTILE_POWER_CLASS = Reflection.getClass("{play}.ClientboundProjectilePowerPacket");
                STORE_COOKIE_CLASS = Reflection.getClass("{common}.ClientboundStoreCookiePacket");
                TRANSFER_CLASS = Reflection.getClass("{common}.ClientboundTransferPacket");
            }
            if (protocolVersion.isOlderThan(NetProtocol.v1_20_2)) {
                SPAWN_NAMED_ENTITY_CLASS = Reflection.getClass("{play}.PacketPlayOutNamedEntitySpawn");
            }
            if (protocolVersion.isOlderThan(NetProtocol.v1_20_4)) {
                RESOURCE_PACK_SEND_CLASS = Reflection.getUnsureClass("{play}.PacketPlayOutResourcePackSend", "{common}.ClientboundResourcePackPacket");
            }
        }

        public enum Server implements RangedPacketType {

            ABILITIES(ABILITIES_SERVER_CLASS, "player_abilities"),
            ADD_VIBRATION_SIGNAL(ADD_VIBRATION_SIGNAL_CLASS, "add_vibration_signal", NetProtocolRange.between(NetProtocol.v1_17, NetProtocol.v1_18_2)),
            ADVANCEMENTS(ADVANCEMENTS_SERVER_CLASS, "update_advancements", NetProtocolRange.since(NetProtocol.v1_12)),
            ATTACH_ENTITY(ATTACH_ENTITY_CLASS, "set_entity_link"),
            AUTO_RECIPE(AUTO_RECIPE_SERVER_CLASS, "place_ghost_recipe", NetProtocolRange.since(NetProtocol.v1_12_1)),
            BLOCK_ACTION(BLOCK_ACTION_CLASS, "block_event"),
            BLOCK_BREAK_ANIMATION(BLOCK_BREAK_ANIMATION_CLASS, "block_destruction"),
            BLOCK_BROKE_ACK(BLOCK_BROKE_ACK_CLASS, "block_break_ack", NetProtocolRange.between(NetProtocol.v1_14, NetProtocol.v1_18_2)),
            BLOCK_CHANGE(BLOCK_CHANGE_CLASS, "block_update"),
            BLOCK_CHANGED_ACK(BLOCK_CHANGED_ACK_CLASS, "block_changed_ack", NetProtocolRange.since(NetProtocol.v1_19)),
            BOSS_EVENT(BOSS_EVENT_CLASS, "boss_event", NetProtocolRange.since(NetProtocol.v1_9)),
            BUNDLE(BUNDLE_CLASS, "bundle", NetProtocolRange.since(NetProtocol.v1_19_4)),
            BUNDLE_DELIMITER(BUNDLE_DELIMITER_CLASS, "bundle_delimiter", NetProtocolRange.since(NetProtocol.v1_19_4)),
            CAMERA(CAMERA_CLASS, "set_camera"),
            CHAT_PREVIEW(CHAT_PREVIEW_SERVER_CLASS, "chat_preview", NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            CHUNKS_BIOMES(CHUNKS_BIOMES_CLASS, "chunks_biomes", NetProtocolRange.since(NetProtocol.v1_19_4)),
            CHUNK_BATCH_FINISHED(CHUNK_BATCH_FINISHED_CLASS, "chunk_batch_finished", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CHUNK_BATCH_START(CHUNK_BATCH_START_CLASS, "chunk_batch_start", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CLEAR_TITLES(CLEAR_TITLES_CLASS, "clear_titles", NetProtocolRange.since(NetProtocol.v1_17)),
            CLOSE_WINDOW(CLOSE_WINDOW_SERVER_CLASS, "container_close"),
            COLLECT_ITEM(COLLECT_ITEM_CLASS, "take_item_entity"),
            COMBAT_EVENT(COMBAT_EVENT_CLASS, "player_combat", NetProtocolRange.until(NetProtocol.v1_16_5)),
            COMMANDS(COMMANDS_CLASS, "commands", NetProtocolRange.since(NetProtocol.v1_13)),
            COOKIE_REQUEST(COOKIE_REQUEST_CLASS, "cookie_request", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CUSTOM_CHAT_COMPLETIONS(CUSTOM_CHAT_COMPLETIONS_CLASS, "custom_chat_completions", NetProtocolRange.since(NetProtocol.v1_19_1)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, "custom_payload"),
            CUSTOM_SOUND_EFFECT(CUSTOM_SOUND_EFFECT_CLASS, "custom_sound", NetProtocolRange.between(NetProtocol.v1_9, NetProtocol.v1_19_2)),
            DAMAGE_EVENT(DAMAGE_EVENT_CLASS, "damage_event", NetProtocolRange.since(NetProtocol.v1_19_4)),
            DEBUG_SAMPLE(DEBUG_SAMPLE_CLASS, "debug_sample", NetProtocolRange.since(NetProtocol.v1_20_6)),
            DELETE_CHAT_MESSAGE(DELETE_CHAT_MESSAGE_CLASS, "delete_chat", NetProtocolRange.since(NetProtocol.v1_19_1)),
            DISCONNECT(DISCONNECT_CLASS, "disconnect"),
            DISGUISED_CHAT(DISGUISED_CHAT_CLASS, "disguised_chat", NetProtocolRange.since(NetProtocol.v1_19_3)),
            ENTITY(ENTITY_CLASS, "move_entity", NetProtocolRange.until(NetProtocol.v1_16_5)),
            ENTITY_ANIMATION(ENTITY_ANIMATION_CLASS, "animate"),
            ENTITY_DESTROY(ENTITY_DESTROY_CLASS, "remove_entities"),
            ENTITY_EFFECT(ENTITY_EFFECT_CLASS, "update_mob_effect"),
            ENTITY_EQUIPMENT(ENTITY_EQUIPMENT_CLASS, "set_equipment"),
            ENTITY_LOOK(ENTITY_LOOK_CLASS, "move_entity_rot"),
            ENTITY_METADATA(ENTITY_METADATA_CLASS, "set_entity_data"),
            ENTITY_POSITION(ENTITY_MOVE_CLASS, "move_entity_pos"),
            ENTITY_POS_ROT(ENTITY_MOVE_LOOK_CLASS, "move_entity_pos_rot"),
            ENTITY_ROTATION(ENTITY_ROTATION_CLASS, "rotate_head"),
            ENTITY_SOUND(ENTITY_SOUND_CLASS, "sound_entity", NetProtocolRange.since(NetProtocol.v1_14)),
            ENTITY_STATUS(ENTITY_STATUS_CLASS, "entity_event"),
            ENTITY_TELEPORT(ENTITY_TELEPORT_CLASS, "teleport_entity"),
            ENTITY_VELOCITY(ENTITY_VELOCITY_CLASS, "set_entity_motion"),
            EXPERIENCE(EXPERIENCE_CLASS, "set_experience"),
            EXPLOSION(EXPLOSION_CLASS, "explode"),
            GAME_STATE_CHANGE(GAME_STATE_CHANGE_CLASS, "game_event"),
            HELD_ITEM_CHANGE(HELD_ITEM_CHANGE_SERVER_CLASS, "set_carried_item"),
            HURT_ANIMATION(HURT_ANIMATION_CLASS, "hurt_animation", NetProtocolRange.since(NetProtocol.v1_19_4)),
            INITIALIZE_BORDER(INITIALIZE_BORDER_CLASS, "initialize_border", NetProtocolRange.since(NetProtocol.v1_17)),
            JOIN_GAME(JOIN_GAME_CLASS, "login"),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, "keep_alive"),
            LEVEL_CHUNK_WITH_LIGHT(LEVEL_CHUNK_WITH_LIGHT_CLASS, "level_chunk_with_light", NetProtocolRange.since(NetProtocol.v1_18)),
            LIGHT_UPDATE(LIGHT_UPDATE_CLASS, "light_update", NetProtocolRange.since(NetProtocol.v1_14)),
            LOOK_AT(LOOK_AT_CLASS, "player_look_at", NetProtocolRange.since(NetProtocol.v1_13)),
            MAP_BULK_CHUNK(MAP_BULK_CHUNK_CLASS, "map_bulk_chunk", NetProtocolRange.until(NetProtocol.v1_8_8)),
            MAP_CHUNK(MAP_CHUNK_CLASS, "level_chunk", NetProtocolRange.until(NetProtocol.v1_17_1)),
            MAP_ITEM_DATA(MAP_ITEM_DATA_CLASS, "map_item_data"),
            MOUNT(MOUNT_CLASS, "set_passengers", NetProtocolRange.since(NetProtocol.v1_9)),
            MULTI_BLOCK_CHANGE(MULTI_BLOCK_CHANGE_CLASS, "section_blocks_update"),
            NBT_QUERY(NBT_QUERY_CLASS, "tag_query", NetProtocolRange.since(NetProtocol.v1_13)),
            OPEN_BOOK(OPEN_BOOK_CLASS, "open_book", NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_SIGN_EDITOR(OPEN_SIGN_EDITOR_CLASS, "open_sign_editor"),
            OPEN_WINDOW(OPEN_WINDOW_CLASS, "open_screen"),
            OPEN_WINDOW_HORSE(OPEN_WINDOW_HORSE_CLASS, "horse_screen_open", NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_WINDOW_MERCHANT(OPEN_WINDOW_MERCHANT_CLASS, "merchant_offers", NetProtocolRange.since(NetProtocol.v1_14)),
            PING(PING_CLASS, "ping", NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_CHAT(PLAYER_CHAT_CLASS, "player_chat"),
            PLAYER_CHAT_HEADER(PLAYER_CHAT_HEADER_CLASS, "player_chat_header", NetProtocolRange.between(NetProtocol.v1_19_1, NetProtocol.v1_19_2)),
            PLAYER_COMBAT_END(PLAYER_COMBAT_END_CLASS, "player_combat_end", NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_COMBAT_ENTER(PLAYER_COMBAT_ENTER_CLASS, "player_combat_enter", NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_COMBAT_KILL(PLAYER_COMBAT_KILL_CLASS, "player_combat_kill", NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_INFO(PLAYER_INFO_CLASS, "player_info", NetProtocolRange.until(NetProtocol.v1_19_2)),
            PLAYER_INFO_REMOVE(PLAYER_INFO_REMOVE_CLASS, "player_info_remove", NetProtocolRange.since(NetProtocol.v1_19_3)),
            PLAYER_INFO_UPDATE(PLAYER_INFO_UPDATE_CLASS, "player_info_update", NetProtocolRange.since(NetProtocol.v1_19_3)),
            PLAYER_POSITION(PLAYER_POSITION_SERVER_CLASS, "player_position"),
            PLAYER_SLEEP(PLAYER_SLEEP_CLASS, "player_sleep", NetProtocolRange.until(NetProtocol.v1_13_2)),
            PONG(PONG_CLASS, "pong_response", NetProtocolRange.since(NetProtocol.v1_20_2)),
            PROJECTILE_POWER(PROJECTILE_POWER_CLASS, "projectile_power", NetProtocolRange.since(NetProtocol.v1_20_6)),
            RECIPES(RECIPES_CLASS, "recipe", NetProtocolRange.since(NetProtocol.v1_12)),
            RECIPE_UPDATE(RECIPE_UPDATE_CLASS, "update_recipes", NetProtocolRange.since(NetProtocol.v1_13)),
            REMOVE_ENTITY_EFFECT(REMOVE_ENTITY_EFFECT_CLASS, "remove_mob_effect"),
            RESET_SCORE(RESET_SCORE_CLASS, "reset_score", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_POP(RESOURCE_PACK_POP_CLASS, "resource_pack_pop", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_PUSH(RESOURCE_PACK_PUSH_CLASS, "resource_pack_push", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_CLASS, "resource_pack", NetProtocolRange.until(NetProtocol.v1_20_2)),
            RESPAWN(RESPAWN_CLASS, "respawn"),
            SCOREBOARD_DISPLAY_OBJECTIVE(SCOREBOARD_DISPLAY_OBJECTIVE_CLASS, "set_display_objective"),
            SCOREBOARD_OBJECTIVE(SCOREBOARD_OBJECTIVE_CLASS, "set_objective"),
            SCOREBOARD_SCORE(SCOREBOARD_SCORE_CLASS, "set_score"),
            SCOREBOARD_TEAM(SCOREBOARD_TEAM_CLASS, "set_player_team"),
            SELECT_ADVANCEMENT_TAB(SELECT_ADVANCEMENT_TAB_CLASS, "select_advancements_tab", NetProtocolRange.since(NetProtocol.v1_12)),
            SERVER_DATA(SERVER_DATA_CLASS, "server_data", NetProtocolRange.since(NetProtocol.v1_19)),
            SERVER_DIFFICULTY(SERVER_DIFFICULTY_CLASS, "change_difficulty"),
            SET_ACTION_BAR_TEXT(SET_ACTION_BAR_TEXT_CLASS, "set_action_bar_text", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_SIZE(SET_BORDER_SIZE_CLASS, "set_border_size", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_WARNING_DELAY(SET_BORDER_WARNING_DELAY_CLASS, "set_border_warning_delay", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_BORDER_WARNING_DISTANCE(SET_BORDER_WARNING_DISTANCE_CLASS, "set_border_warning_distance", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, "set_compression", NetProtocolRange.until(NetProtocol.v1_8_8)),
            SET_COOLDOWN(SET_COOLDOWN_CLASS, "cooldown", NetProtocolRange.since(NetProtocol.v1_9)),
            SET_DISPLAY_CHAT_PREVIEW(SET_DISPLAY_CHAT_PREVIEW_CLASS, "set_display_chat_preview", NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            SET_SIMULATION_DISTANCE(SET_SIMULATION_DISTANCE_CLASS, "set_simulation_distance", NetProtocolRange.since(NetProtocol.v1_18)),
            SET_SLOT(SET_SLOT_CLASS, "container_set_slot"),
            SET_SUBTITLE_TEXT(SET_SUBTITLE_TEXT_CLASS, "set_subtitle_text", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_TITLES_ANIMATION(SET_TITLES_ANIMATION_CLASS, "set_titles_animation", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_TITLE_TEXT(SET_TITLE_TEXT_CLASS, "set_title_text", NetProtocolRange.since(NetProtocol.v1_17)),
            SOUND(SOUND_CLASS, "sound"),
            SPAWN_ENTITY(SPAWN_ENTITY_CLASS, "add_entity"),
            SPAWN_ENTITY_LIVING(SPAWN_ENTITY_LIVING_CLASS, "add_mob", NetProtocolRange.until(NetProtocol.v1_18_2)),
            SPAWN_ENTITY_PAINTING(SPAWN_ENTITY_PAINTING_CLASS, "add_painting", NetProtocolRange.until(NetProtocol.v1_18_2)),
            SPAWN_GLOBAL_ENTITY(SPAWN_GLOBAL_ENTITY_CLASS, "add_global_entity", NetProtocolRange.until(NetProtocol.v1_15_2)),
            SPAWN_EXPERIENCE_ORB(SPAWN_EXPERIENCE_ORB_CLASS, "add_experience_orb"),
            SPAWN_NAMED_ENTITY(SPAWN_NAMED_ENTITY_CLASS, "add_player", NetProtocolRange.until(NetProtocol.v1_20_1)),
            SPAWN_POSITION(SPAWN_POSITION_CLASS, "set_default_spawn_position"),
            START_CONFIGURATION(START_CONFIGURATION_CLASS, "start_configuration", NetProtocolRange.since(NetProtocol.v1_20_2)),
            STATISTICS(STATISTICS_CLASS, "award_stats"),
            STOP_SOUND(STOP_SOUND_CLASS, "stop_sound", NetProtocolRange.since(NetProtocol.v1_13)),
            STORE_COOKIE(STORE_COOKIE_CLASS, "store_cookie", NetProtocolRange.since(NetProtocol.v1_20_6)),
            SYSTEM_CHAT(SYSTEM_CHAT_CLASS, "system_chat", NetProtocolRange.since(NetProtocol.v1_19)),
            TAB_COMPLETE(TAB_COMPLETE_SERVER_CLASS, "command_suggestions"),
            TAB_LIST(TAB_LIST_CLASS, "tab_list"),
            TICKING_STATE(TICKING_STATE_CLASS, "ticking_state", NetProtocolRange.since(NetProtocol.v1_20_4)),
            TICKING_STEP(TICKING_STEP_CLASS, "ticking_step", NetProtocolRange.since(NetProtocol.v1_20_4)),
            TILE_ENTITY_DATA(TILE_ENTITY_DATA_CLASS, "block_entity_data"),
            TIME_UPDATE(TIME_UPDATE_CLASS, "set_time"),
            TITLE(TITLE_CLASS, "set_titles", NetProtocolRange.until(NetProtocol.v1_16_5)),
            TRANSACTION(TRANSACTION_SERVER_CLASS, "container_ack", NetProtocolRange.until(NetProtocol.v1_16_5)),
            TRANSFER(TRANSFER_CLASS, "transfer", NetProtocolRange.since(NetProtocol.v1_20_6)),
            UNLOAD_CHUNK(UNLOAD_CHUNK_CLASS, "forget_level_chunk", NetProtocolRange.since(NetProtocol.v1_9)),
            UPDATE_ATTRIBUTES(UPDATE_ATTRIBUTES_CLASS, "update_attributes"),
            UPDATE_ENABLED_FEATURES(UPDATE_ENABLED_FEATURES_CLASS, "update_enabled_features", NetProtocolRange.between(NetProtocol.v1_19_3, NetProtocol.v1_20_1)),
            UPDATE_ENTITY_NBT(UPDATE_ENTITY_NBT_CLASS, "update_entity_nbt", NetProtocolRange.until(NetProtocol.v1_8_8)),
            UPDATE_HEALTH(UPDATE_HEALTH_CLASS, "set_health"),
            UPDATE_SIGN(UPDATE_SIGN_SERVER_CLASS, "update_sign", NetProtocolRange.until(NetProtocol.v1_9_2)),
            UPDATE_TAGS(UPDATE_TAGS_CLASS, "update_tags", NetProtocolRange.since(NetProtocol.v1_13)),
            VEHICLE_MOVE(VEHICLE_MOVE_SERVER_CLASS, "move_vehicle", NetProtocolRange.since(NetProtocol.v1_9)),
            VIEW_CENTRE(VIEW_CENTRE_CLASS, "set_chunk_cache_center", NetProtocolRange.since(NetProtocol.v1_14)),
            VIEW_DISTANCE(VIEW_DISTANCE_CLASS, "set_chunk_cache_radius", NetProtocolRange.since(NetProtocol.v1_14)),
            WINDOW_DATA(WINDOW_DATA_CLASS, "container_set_data"),
            WINDOW_ITEMS(WINDOW_ITEMS_CLASS, "container_set_content"),
            WORLD_BORDER(WORLD_BORDER_CLASS, "set_border", NetProtocolRange.until(NetProtocol.v1_16_5)),
            WORLD_BORDER_CENTER(WORLD_BORDER_CENTER_CLASS, "set_border_center", NetProtocolRange.since(NetProtocol.v1_17)),
            WORLD_BORDER_LERP_SIZE(WORLD_BORDER_LERP_SIZE_CLASS, "set_border_lerp_size", NetProtocolRange.since(NetProtocol.v1_17)),
            WORLD_EVENT(WORLD_EVENT_CLASS, "level_event"),
            WORLD_PARTICLES(WORLD_PARTICLES_CLASS, "level_particles")
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsClass;
            private final int packetId = idRegistry.acquire();
            private final NetProtocolRange protocolRange;

            Server(Class<?> nmsClass, String keyName) {
                this(nmsClass, keyName, NetProtocolRange.ALL);
            }

            Server(Class<?> nmsClass, String keyName, NetProtocolRange protocolRange) {
                this.nmsClass = nmsClass;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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

        private static Class<?> CLIENT_INFORMATION_CLASS;
        private static Class<?> COOKIE_RESPONSE_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_CLIENT_CLASS;
        private static Class<?> FINISH_CONFIGURATION_CLIENT_CLASS;
        private static Class<?> KEEP_ALIVE_CLIENT_CLASS;
        private static Class<?> PONG_CLASS;
        private static Class<?> RESOURCE_PACK_SEND_CLIENT_CLASS;
        private static Class<?> SELECT_KNOWN_PACKS_CLIENT_CLASS;

        static {
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CLIENT_INFORMATION_CLASS = Reflection.getClass("{common}.ServerboundClientInformationPacket");
                CUSTOM_PAYLOAD_CLIENT_CLASS = Reflection.getClass("{common}.ServerboundCustomPayloadPacket");
                FINISH_CONFIGURATION_CLIENT_CLASS = Reflection.getClass("{configuration}.ServerboundFinishConfigurationPacket");
                KEEP_ALIVE_CLIENT_CLASS = Reflection.getClass("{common}.ServerboundKeepAlivePacket");
                PONG_CLASS = Reflection.getClass("{common}.ServerboundPongPacket");
                RESOURCE_PACK_SEND_CLIENT_CLASS = Reflection.getClass("{common}.ServerboundResourcePackPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                COOKIE_RESPONSE_CLASS = Reflection.getClass("{cookie}.ServerboundCookieResponsePacket");
                SELECT_KNOWN_PACKS_CLIENT_CLASS = Reflection.getClass("{configuration}.ServerboundSelectKnownPacks");
            }
        }

        public enum Client implements RangedPacketType {
            CLIENT_INFORMATION(CLIENT_INFORMATION_CLASS, "client_information", NetProtocolRange.since(NetProtocol.v1_20_2)),
            COOKIE_RESPONSE(COOKIE_RESPONSE_CLASS, "cookie_response", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLIENT_CLASS, "custom_payload", NetProtocolRange.since(NetProtocol.v1_20_2)),
            FINISH_CONFIGURATION(FINISH_CONFIGURATION_CLIENT_CLASS, "finish_configuration", NetProtocolRange.since(NetProtocol.v1_20_2)),
            KEEP_ALIVE(KEEP_ALIVE_CLIENT_CLASS, "keep_alive", NetProtocolRange.since(NetProtocol.v1_20_2)),
            PONG(PONG_CLASS, "pong", NetProtocolRange.since(NetProtocol.v1_20_2)),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_CLIENT_CLASS, "resource_pack", NetProtocolRange.since(NetProtocol.v1_20_2)),
            SELECT_KNOWN_PACKS(SELECT_KNOWN_PACKS_CLIENT_CLASS, "select_known_packs", NetProtocolRange.since(NetProtocol.v1_20_6)),
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsPacket;
            private final int packetId = idRegistry.acquire();
            private final NetProtocolRange protocolRange;

            Client(Class<?> nmsPacket, String keyName, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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

        private static Class<?> COOKIE_REQUEST_CLASS;
        private static Class<?> CUSTOM_PAYLOAD_SERVER_CLASS;
        private static Class<?> FINISH_CONFIGURATION_SERVER_CLASS;
        private static Class<?> KEEP_ALIVE_SERVER_CLASS;
        private static Class<?> DISCONNECT_CLASS;
        private static Class<?> PING_CLASS;
        private static Class<?> RESOURCE_PACK_SEND_SERVER_CLASS;
        private static Class<?> RESOURCE_PACK_POP_CLASS;
        private static Class<?> RESOURCE_PACK_PUSH_CLASS;
        private static Class<?> REGISTRY_DATA_CLASS;
        private static Class<?> RESET_CHAT_CLASS;
        private static Class<?> SELECT_KNOWN_PACKS_SERVER_CLASS;
        private static Class<?> STORE_COOKIE_CLASS;
        private static Class<?> TRANSFER_CLASS;
        private static Class<?> UPDATE_ENABLED_FEATURES_CLASS;
        private static Class<?> UPDATE_TAGS_CLASS;

        static {
            if (protocolVersion.isEqual(NetProtocol.v1_20_2)) {
                RESOURCE_PACK_SEND_SERVER_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_SERVER_CLASS = Reflection.getClass("{common}.ClientboundCustomPayloadPacket");
                FINISH_CONFIGURATION_SERVER_CLASS = Reflection.getClass("{configuration}.ClientboundFinishConfigurationPacket");
                KEEP_ALIVE_SERVER_CLASS = Reflection.getClass("{common}.ClientboundKeepAlivePacket");
                DISCONNECT_CLASS = Reflection.getClass("{common}.ClientboundDisconnectPacket");
                PING_CLASS = Reflection.getClass("{common}.ClientboundPingPacket");
                REGISTRY_DATA_CLASS = Reflection.getClass("{configuration}.ClientboundRegistryDataPacket");
                UPDATE_ENABLED_FEATURES_CLASS = Reflection.getClass("{configuration}.ClientboundUpdateEnabledFeaturesPacket");
                UPDATE_TAGS_CLASS = Reflection.getClass("{common}.ClientboundUpdateTagsPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_4)) {
                RESOURCE_PACK_POP_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPopPacket");
                RESOURCE_PACK_PUSH_CLASS = Reflection.getClass("{common}.ClientboundResourcePackPushPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                COOKIE_REQUEST_CLASS = Reflection.getClass("{cookie}.ClientboundCookieRequestPacket");
                RESET_CHAT_CLASS = Reflection.getClass("{configuration}.ClientboundResetChatPacket");
                SELECT_KNOWN_PACKS_SERVER_CLASS = Reflection.getClass("{configuration}.ClientboundSelectKnownPacks");
                STORE_COOKIE_CLASS = Reflection.getClass("{common}.ClientboundStoreCookiePacket");
                TRANSFER_CLASS = Reflection.getClass("{common}.ClientboundTransferPacket");
            }
        }

        public enum Server implements RangedPacketType {
            COOKIE_REQUEST(COOKIE_REQUEST_CLASS, "cookie_request", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, "custom_payload", NetProtocolRange.since(NetProtocol.v1_20_2)),
            FINISH_CONFIGURATION(FINISH_CONFIGURATION_SERVER_CLASS, "finish_configuration", NetProtocolRange.since(NetProtocol.v1_20_2)),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, "keep_alive", NetProtocolRange.since(NetProtocol.v1_20_2)),
            DISCONNECT(DISCONNECT_CLASS, "disconnect", NetProtocolRange.since(NetProtocol.v1_20_2)),
            PING(PING_CLASS, "ping", NetProtocolRange.since(NetProtocol.v1_20_2)),
            REGISTRY_DATA(REGISTRY_DATA_CLASS, "registry_data", NetProtocolRange.since(NetProtocol.v1_20_2)),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_SERVER_CLASS, "resource_pack", NetProtocolRange.only(NetProtocol.v1_20_2)),
            RESOURCE_PACK_POP(RESOURCE_PACK_POP_CLASS, "resource_pack_pop", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_PUSH(RESOURCE_PACK_PUSH_CLASS, "resource_pack_push", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESET_CHAT(RESET_CHAT_CLASS, "reset_chat", NetProtocolRange.since(NetProtocol.v1_20_6)),
            SELECT_KNOWN_PACKS(SELECT_KNOWN_PACKS_SERVER_CLASS, "select_known_packs", NetProtocolRange.since(NetProtocol.v1_20_6)),
            STORE_COOKIE(STORE_COOKIE_CLASS, "store_cookie", NetProtocolRange.since(NetProtocol.v1_20_6)),
            TRANSFER(TRANSFER_CLASS, "transfer", NetProtocolRange.since(NetProtocol.v1_20_6)),
            UPDATE_ENABLED_FEATURES(UPDATE_ENABLED_FEATURES_CLASS, "update_enabled_features", NetProtocolRange.since(NetProtocol.v1_20_2)),
            UPDATE_TAGS(UPDATE_TAGS_CLASS, "update_tags", NetProtocolRange.since(NetProtocol.v1_20_2)),
            ;

            private final LazyInit<Integer> minecraftIdInit = LazyInit.getMinecraftIDInit(this);
            private final String keyName;
            private final Class<?> nmsPacket;
            private final int packetId = idRegistry.acquire();
            private final NetProtocolRange protocolRange;

            Server(Class<?> nmsPacket, String keyName, NetProtocolRange protocolRange) {
                this.nmsPacket = nmsPacket;
                this.protocolRange = protocolRange;
                this.keyName = keyName;
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
            public String getKeyName() {
                return keyName;
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
