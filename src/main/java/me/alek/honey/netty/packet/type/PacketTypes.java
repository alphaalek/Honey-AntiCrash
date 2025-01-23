package me.alek.honey.netty.packet.type;

import me.alek.honey.compat.via.ViaProtocol;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.LegacyProtocol;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LazyInit;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PacketTypes {

    private static final Array<PacketType> UNKNOWN_ARRAY = Array.just(PacketType.UNKNOWN);

    public static boolean isUnknown(PacketType type) {
        return type == null || type == PacketType.UNKNOWN;
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
            Bukkit.getLogger().severe("Done registering: protocol: " + NetProtocol.getProtocol() + ", state: " + state + ", bound: " + bound + " protocol) but found missing packet-type indices: " + indices);
            Bukkit.getLogger().severe(typesArray.toString());
        }
    }

    public static Array<PacketType> getAllTypesAvailableFor(PacketState state, PacketBound bound) {
        return Array.from(getAllPacketTypes().stream()
                .filter(t -> t.getBound().equals(bound))
                .filter(t -> t.getState().equals(state)).toArray());
    }

    public static Array<PacketType> getTypesAvailableFor(NetProtocol protocol, PacketState state, PacketBound bound) {
        if (protocol.equals(NetProtocol.getProtocol())) {
            return getTypesAvailableFor(state, bound);
        }

        return Array.from(getAllTypesAvailableFor(state, bound).stream()
                .filter(t -> !(t instanceof NetProtocolRange.Ranged) || ((NetProtocolRange.Ranged) t).getRange().match(protocol))
                .toArray());
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
            if (type.getKeyName() != null && type.getKeyName().equals(key)) {
                return type;
            }
        }

        return PacketType.UNKNOWN;
    }

    public static PacketType getTypeFromKey(PacketState state, PacketBound bound, String key) {
        for (PacketType type : getAllTypesAvailableFor(state, bound)) {
            if (type.getKeyName() != null && type.getKeyName().equals(key)) {
                return type;
            }
        }

        return PacketType.UNKNOWN;
    }

    public static PacketType getTypeFromViaKey(NetProtocol protocol, PacketState state, PacketBound bound, String key) {
        for (PacketType type : getTypesAvailableFor(protocol, state, bound)) {
            try {
                Field field = type.getClass().getField(((Enum<?>)type).name());
                ViaProtocol.MultipleUnique multiVia = field.getAnnotation(ViaProtocol.MultipleUnique.class);
                if (multiVia != null) {
                    for (ViaProtocol.Unique via : multiVia.uniques()) {
                        if (checkVia(via, protocol, key)) {
                            return type;
                        }
                    }
                }

                ViaProtocol.Unique via = field.getAnnotation(ViaProtocol.Unique.class);
                if (via != null && checkVia(via, protocol, key)) {
                    return type;
                }

                if (type.getKeyName() != null && type.getKeyName().equals(key)) {
                    return type;
                }

            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to acquire packet type via data from via key for type " + type, ex);
            }
        }

        return PacketType.UNKNOWN;
    }

    private static boolean checkVia(ViaProtocol.Unique via, NetProtocol protocol, String key) {
        return NetProtocolRange.matchProtocol(protocol, via.to(), via.from())
                && ViaProtocol.Unique.Target.match(via.target())
                && via.name().equals(key);
    }

    public static int getMinecraftId(PacketType type) {
        if (type instanceof RangedPacketType && !((RangedPacketType) type).getRange().match()) {
            throw new IllegalArgumentException("Not present in server version");
        }

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            Object streamCodec = CodecFactory.getStreamCodec1_20_6(type.getBound(), type.getState());

            List<Object> codecs = ReflectStructure.Provider.acquireStructure(streamCodec.getClass())
                    .<List<Object>>withWeakType(List.class).withTarget(streamCodec)
                    .readField(0);
            int i = 0;
            for (Object codec : codecs) {
                Object packetType = ReflectStructure.Provider.acquireStructure(codec.getClass())
                        .withTarget(codec)
                        .readField(null, (in, f) -> !NMS.getStreamCodecClass().isAssignableFrom(f.getHandle().getType()));
                Object minecraftKey = ReflectStructure.Provider.acquireStructure(NMS.getPacketTypeClass())
                        .withType(NMS.getMinecraftKeyClass()).withTarget(packetType)
                        .readField(0);

                PacketType typeFromCodec = PacketTypes.getTypeFromKey(NetProtocol.getProtocol(), type.getState(), type.getBound(), minecraftKey.toString().split(":")[1]);
                if (typeFromCodec.equals(type)) {
                    return i;
                }
                i++;
            }
            return -1;
        } else {
            LegacyProtocol enumProtocol = LegacyProtocol.getEnumProtocolDelegate(type.getBound(), type.getState());

            return enumProtocol.getPacketId(type.getNmsClass());
        }
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

    public enum NameGetStrategy {
        BY_NAME,

        BY_CLASS_NAME,

        EITHER
    }

    public static class Handshake {

        private static final Class<?> SET_PROTOCOL_CLASS = Reflection.getClass("{handshake}.PacketHandshakingInSetProtocol");

        public enum Client implements PacketType {

            @ViaProtocol.Unique(name = "client_intention")
            SET_PROTOCOL(SET_PROTOCOL_CLASS, "intention");

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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
        private static Class<?> LOGIN_ACKNOWLEDGED_CLASS;

        static {
            if (protocolVersion.isBetween(NetProtocol.v1_13, NetProtocol.v1_20_1)) {
                CUSTOM_PAYLOAD_CLIENT_CLASS = Reflection.getClass("{login}.PacketLoginInCustomPayload");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                CUSTOM_PAYLOAD_CLIENT_CLASS = Reflection.getClass("{login}.ServerboundCustomQueryAnswerPacket");
                LOGIN_ACKNOWLEDGED_CLASS = Reflection.getClass("{login}.ServerboundLoginAcknowledgedPacket");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_20_6)) {
                COOKIE_RESPONSE_CLASS = Reflection.getClass("{cookie}.ServerboundCookieResponsePacket");
            }
        }

        public enum Client implements RangedPacketType {
            @ViaProtocol.IgnoreIfMissing
            COOKIE_RESPONSE(COOKIE_RESPONSE_CLASS, "cookie_response", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.IgnoreIfMissing
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLIENT_CLASS, "custom_query_answer", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "encryption_key")
            ENCRYPTION_BEGIN(ENCRYPTION_BEGIN_CLASS, "key"),
            @ViaProtocol.IgnoreIfMissing
            LOGIN_ACKNOWLEDGED(LOGIN_ACKNOWLEDGED_CLASS, "login_acknowledged", NetProtocolRange.since(NetProtocol.v1_20_2)),
            HELLO(LOGIN_START_CLASS, "hello");

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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
            @ViaProtocol.Unique(name = "login_finished", target = ViaProtocol.Unique.Target.MODERN)
            LOGIN_SUCCESS(LOGIN_SUCCESS_CLASS, "game_profile"),
            @ViaProtocol.IgnoreIfMissing
            COOKIE_REQUEST(COOKIE_REQUEST_CLASS, "cookie_request", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.IgnoreIfMissing
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, "custom_query", NetProtocolRange.since(NetProtocol.v1_13)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, "login_compression");

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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

        private static final Class<?> SWING_CLASS = Reflection.getClass("{play}.PacketPlayInArmAnimation");
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
        private static Class<?> ADVANCEMENTS_CLASS;
        private static Class<?> AUTO_RECIPE_CLASS;
        private static Class<?> BLOCK_PLACE_CLASS;
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
        private static Class<?> GENERATE_JIGSAW_CLASS;
        private static Class<?> PICK_ITEM_CLASS;
        private static Class<?> PING_REQUEST_CLASS;
        private static Class<?> PLAYER_MOVEMENT_CLASS;
        private static Class<?> PLAYER_POS_ROT_CLASS;
        private static Class<?> PLAYER_POSITION_CLASS;
        private static Class<?> PLAYER_ROTATION_CLASS;
        private static Class<?> PONG_RESPONSE_CLASS;
        private static Class<?> PREPARE_CRAFTING_GRID_CLASS;
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
            if (protocolVersion.isEqual(NetProtocol.v1_8_8)) {
                BLOCK_PLACE_CLASS = Reflection.getClass("{play}.PacketPlayInBlockPlace");
            }
            if (protocolVersion.isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                PLAYER_MOVEMENT_CLASS = Reflection.getClass("{play}.PacketPlayInFlying");
                PLAYER_POSITION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPosition");
                PLAYER_POS_ROT_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPositionLook");
                PLAYER_ROTATION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInLook");
                TRANSACTION_CLASS = Reflection.getClass("{play}.PacketPlayInTransaction");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_9)) {
                USE_ITEM_CLASS = Reflection.getClass("{play}.PacketPlayInBlockPlace");
                BOAT_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayInBoatMove");
                BLOCK_PLACE_CLASS = Reflection.getClass("{play}.PacketPlayInUseItem");
                TELEPORT_CONFIRM_CLASS = Reflection.getClass("{play}.PacketPlayInTeleportAccept");
                VEHICLE_MOVE_CLASS = Reflection.getClass("{play}.PacketPlayInVehicleMove");
            }
            if (protocolVersion.isEqual(NetProtocol.v1_12)) {
                PREPARE_CRAFTING_GRID_CLASS = Reflection.getClass("{play}.PacketPlayInPrepareCraftingGrid");
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
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_16)) {
                GENERATE_JIGSAW_CLASS = Reflection.getClass("{play}.PacketPlayInJigsawGenerate");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_16_2)) {
                RECIPE_SETTINGS_CLASS = Reflection.getClass("{play}.PacketPlayInRecipeSettings");
            }
            if (protocolVersion.isNewerThanOrEqual(NetProtocol.v1_17)) {
                PLAYER_POSITION_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPosition");
                PLAYER_POS_ROT_CLASS = Reflection.getClass("{play}.PacketPlayInFlying$PacketPlayInPositionLook");
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

            @ViaProtocol.Unique(name = "advancement_tab", target = ViaProtocol.Unique.Target.LEGACY)
            ADVANCEMENTS(ADVANCEMENTS_CLASS, "seen_advancements", NetProtocolRange.since(NetProtocol.v1_12)),
            @ViaProtocol.Unique(name = "animation", target = ViaProtocol.Unique.Target.LEGACY)
            SWING(SWING_CLASS, "swing"),
            @ViaProtocol.Unique(name = "craft_recipe_request", target = ViaProtocol.Unique.Target.LEGACY)
            AUTO_RECIPE(AUTO_RECIPE_CLASS, "place_recipe", NetProtocolRange.since(NetProtocol.v1_12)),
            @ViaProtocol.Unique(name = "player_block_placement", target = ViaProtocol.Unique.Target.LEGACY)
            BLOCK_PLACE(BLOCK_PLACE_CLASS, "use_item_on"),
            @ViaProtocol.Unique(name = "steer_boat", target = ViaProtocol.Unique.Target.LEGACY)
            BOAT_MOVE(BOAT_MOVE_CLASS, "paddle_boat", NetProtocolRange.since(NetProtocol.v1_9)),
            BOOK_EDIT(BOOK_EDIT_CLASS, "edit_book", NetProtocolRange.since(NetProtocol.v1_13)),
            CHAT_ACKNOWLEDGED(CHAT_ACKNOWLEDGED_CLASS, "chat_ack", NetProtocolRange.since(NetProtocol.v1_19_1)),
            CHAT_COMMAND(CHAT_COMMAND_CLASS, "chat_command", NetProtocolRange.since(NetProtocol.v1_19)),
            CHAT_COMMAND_SIGNED(CHAT_COMMAND_SIGNED_CLASS, "chat_command_signed", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.Unique(name = "chat_message", target = ViaProtocol.Unique.Target.LEGACY)
            CHAT_MESSAGE(CHAT_MESSAGE_CLASS, "chat"),
            CHAT_PREVIEW(CHAT_PREVIEW_CLIENT_CLASS, "chat_preview", NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            CHAT_SESSION_UPDATE(CHAT_SESSION_UPDATE_CLASS, "chat_session_update", NetProtocolRange.since(NetProtocol.v1_19_3)),
            CHUNK_BATCH_RECEIVED(CHUNK_BATCH_RECEIVED_CLASS, "chunk_batch_received", NetProtocolRange.since(NetProtocol.v1_20_2)),
            @ViaProtocol.Unique(name = "click_window", target = ViaProtocol.Unique.Target.LEGACY)
            CLICK_WINDOW(CLICK_WINDOW_CLASS, "container_click"),
            @ViaProtocol.Unique(name = "click_window_button", target = ViaProtocol.Unique.Target.LEGACY)
            CLICK_WINDOW_BUTTON(CLICK_WINDOW_BUTTON_CLASS, "container_button_click"),
            @ViaProtocol.Unique(name = "client_status", target = ViaProtocol.Unique.Target.LEGACY)
            CLIENT_COMMAND(CLIENT_COMMAND_CLASS, "client_command"),
            @ViaProtocol.Unique(name = "client_settings", target = ViaProtocol.Unique.Target.LEGACY)
            CLIENT_INFORMATION(CLIENT_INFORMATION_CLASS, "client_information"),
            @ViaProtocol.Unique(name = "close_window", target = ViaProtocol.Unique.Target.LEGACY)
            CLOSE_WINDOW(CLOSE_WINDOW_CLASS, "container_close"),
            CONFIGURATION_START_ACKNOWLEDGED(CONFIGURATION_START_ACKNOWLEDGED_CLASS, "configuration_acknowledged", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CONTAINER_SLOT_STATE_CHANGED(CONTAINER_SLOT_STATE_CHANGED_CLASS, "container_slot_state_changed", NetProtocolRange.since(NetProtocol.v1_20_4)),
            COOKIE_RESPONSE(COOKIE_RESPONSE_CLASS, "cookie_response", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.Unique(name = "creative_inventory_action", target = ViaProtocol.Unique.Target.LEGACY)
            CREATIVE_INVENTORY_ACTION(CREATIVE_INVENTORY_ACTION_CLASS, "set_creative_mode_slot"),
            @ViaProtocol.Unique(name = "plugin_message", target = ViaProtocol.Unique.Target.LEGACY)
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_CLASS, "custom_payload"),
            DEBUG_SAMPLE_SUBSCRIPTION(DEBUG_SAMPLE_SUBSCRIPTION_CLASS, "debug_sample_subscription", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.Unique(name = "set_difficulty", target = ViaProtocol.Unique.Target.LEGACY)
            DIFFICULTY_CHANGE(DIFFICULTY_CHANGE_CLASS, "change_difficulty", NetProtocolRange.since(NetProtocol.v1_14)),
            DIFFICULTY_LOCK(DIFFICULTY_LOCK_CLASS, "lock_difficulty", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "entity_action", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_ACTION(ENTITY_ACTION_CLASS, "player_command"),
            @ViaProtocol.Unique(name = "entity_nbt_request", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_NBT_QUERY(ENTITY_NBT_QUERY_CLASS, "entity_tag_query", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "player_movement", target = ViaProtocol.Unique.Target.LEGACY)
            GROUND(GROUND_CLASS, "move_player_status_only", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "held_item_change", target = ViaProtocol.Unique.Target.LEGACY)
            HELD_ITEM_CHANGE(HELD_ITEM_CHANGE_CLASS, "set_carried_item"),
            @ViaProtocol.Unique(name = "interact_entity", target = ViaProtocol.Unique.Target.LEGACY)
            INTERACT_ENTITY(INTERACT_ENTITY_CLASS, "interact"),
            ITEM_NAME(ITEM_NAME_CLASS, "rename_item", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "generate_jigsaw", target = ViaProtocol.Unique.Target.LEGACY)
            GENERATE_JIGSAW(GENERATE_JIGSAW_CLASS, "jigsaw_generate", NetProtocolRange.since(NetProtocol.v1_16)),
            KEEP_ALIVE(KEEP_ALIVE_CLIENT_CLASS, "keep_alive"),
            PICK_ITEM(PICK_ITEM_CLASS, "pick_item", NetProtocolRange.since(NetProtocol.v1_13)),
            PING(PING_REQUEST_CLASS, "ping_request", NetProtocolRange.since(NetProtocol.v1_20_2)),
            PLAYER_ABILITIES(PLAYER_ABILITIES_CLASS, "player_abilities"),
            @ViaProtocol.Unique(name = "player_digging", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_DIGGING(PLAYER_DIGGING_CLASS, "player_action"),
            @ViaProtocol.Unique(name = "player_movement", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_MOVEMENT(PLAYER_MOVEMENT_CLASS, "move_player_status_only", NetProtocolRange.until(NetProtocol.v1_16_5)),
            @ViaProtocol.Unique(name = "player_position", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_POSITION(PLAYER_POSITION_CLASS, "move_player_pos"),
            @ViaProtocol.Unique(name = "player_position_and_rotation", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_POS_ROT(Play.PLAYER_POS_ROT_CLASS, "move_player_pos_rot"),
            @ViaProtocol.Unique(name = "player_rotation", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_ROTATION(PLAYER_ROTATION_CLASS, "move_player_rot"),
            PONG(PONG_RESPONSE_CLASS, "pong", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "crafting_recipe_placement", target = ViaProtocol.Unique.Target.MODERN)
            PREPARE_CRAFTING_GRID(PREPARE_CRAFTING_GRID_CLASS, "prepare_crafting_grid", NetProtocolRange.only(NetProtocol.v1_12)),
            @ViaProtocol.MultipleUnique(uniques = {
                    @ViaProtocol.Unique(name = "recipe_book_data", target = ViaProtocol.Unique.Target.LEGACY),
                    @ViaProtocol.Unique(name = "recipe_book_update", target = ViaProtocol.Unique.Target.MODERN)
            })
            RECIPE_DISPLAYED(RECIPE_DISPLAYED_CLASS, "recipe_book_change_settings", NetProtocolRange.since(NetProtocol.v1_12)),
            @ViaProtocol.Unique(name = "seen_recipe", target = ViaProtocol.Unique.Target.LEGACY)
            RECIPE_SETTINGS(RECIPE_SETTINGS_CLASS, "recipe_book_seen_recipe", NetProtocolRange.since(NetProtocol.v1_16_2)),
            @ViaProtocol.Unique(name = "resource_pack_status", target = ViaProtocol.Unique.Target.LEGACY)
            RESOURCE_PACK_STATUS(RESOURCE_PACK_STATUS_CLASS, "resource_pack"),
            SELECT_TRADE(SELECT_TRADE_CLASS, "select_trade", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "set_beacon_effect", target = ViaProtocol.Unique.Target.LEGACY)
            SET_BEACON_EFFECT(SET_BEACON_EFFECT_CLASS, "set_beacon", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "update_command_block_minecart", target = ViaProtocol.Unique.Target.LEGACY)
            SET_COMMAND_MINECART(SET_COMMAND_MINECART_CLASS, "set_command_minecart", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "update_jigsaw_block", target = ViaProtocol.Unique.Target.LEGACY)
            SET_JIGSAW(SET_JIGSAW_CLASS, "set_jigsaw_block", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "spectate", target = ViaProtocol.Unique.Target.LEGACY)
            SPECTATE(SPECTATE_CLASS, "teleport_to_entity"),
            @ViaProtocol.Unique(name = "steer_vehicle", target = ViaProtocol.Unique.Target.LEGACY)
            STEER_VEHICLE(STEER_VEHICLE_CLASS, "player_input"),
            @ViaProtocol.Unique(name = "update_structure_block", target = ViaProtocol.Unique.Target.LEGACY)
            STRUCT(STRUCT_CLASS, "set_structure_block", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "tab_complete", target = ViaProtocol.Unique.Target.LEGACY)
            TAB_COMPLETE(TAB_COMPLETE_CLASS, "command_suggestion"),
            @ViaProtocol.Unique(name = "teleport_confirm", target = ViaProtocol.Unique.Target.LEGACY)
            TELEPORT_CONFIRM(TELEPORT_CONFIRM_CLASS, "accept_teleportation", NetProtocolRange.since(NetProtocol.v1_9)),
            @ViaProtocol.Unique(name = "query_block_nbt", target = ViaProtocol.Unique.Target.LEGACY)
            TILE_NBT_QUERY(TILE_NBT_QUERY_CLASS, "block_entity_tag_query", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "window_confirmation", target = ViaProtocol.Unique.Target.LEGACY)
            TRANSACTION(TRANSACTION_CLASS, "container_ack", NetProtocolRange.until(NetProtocol.v1_16_5)),
            @ViaProtocol.Unique(name = "update_command_block", target = ViaProtocol.Unique.Target.LEGACY)
            UPDATE_COMMAND_BLOCK(UPDATE_COMMAND_BLOCK_CLASS, "set_command_block", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "update_sign", target = ViaProtocol.Unique.Target.LEGACY)
            UPDATE_SIGN(UPDATE_SIGN_CLASS, "sign_update"),
            USE_ITEM(USE_ITEM_CLASS, "use_item", NetProtocolRange.since(NetProtocol.v1_9)),
            @ViaProtocol.Unique(name = "vehicle_move", target = ViaProtocol.Unique.Target.LEGACY)
            VEHICLE_MOVE(VEHICLE_MOVE_CLASS, "move_vehicle", NetProtocolRange.since(NetProtocol.v1_9)),
            ;

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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
        private static final Class<?> BLOCK_UPDATE_CLASS = Reflection.getClass("{play}.PacketPlayOutBlockChange");
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
        private static final Class<?> SPAWN_PARTICLE_CLASS = Reflection.getClass("{play}.PacketPlayOutWorldParticles");
        private static Class<?> ADD_VIBRATION_SIGNAL_CLASS;
        private static Class<?> ADVANCEMENTS_SERVER_CLASS;
        private static Class<?> AUTO_RECIPE_SERVER_CLASS;
        private static Class<?> PLAYER_SLEEP_CLASS;
        private static Class<?> BLOCK_BROKE_ACK_CLASS;
        private static Class<?> BLOCK_CHANGED_ACK_CLASS;
        private static Class<?> BOSS_BAR_CLASS;
        private static Class<?> BUNDLE_CLASS;
        private static Class<?> BUNDLE_DELIMITER_CLASS;
        private static Class<?> CHAT_PREVIEW_SERVER_CLASS;
        private static Class<?> CHUNK_BIOMES_CLASS;
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
        private static Class<?> ACTION_BAR_CLASS;
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
        private static Class<?> VIEW_CENTER_CLASS;
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
                BOSS_BAR_CLASS = Reflection.getClass("{play}.PacketPlayOutBoss");
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
                VIEW_CENTER_CLASS = Reflection.getClass("{play}.PacketPlayOutViewCentre");
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
                ACTION_BAR_CLASS = Reflection.getClass("{play}.ClientboundSetActionBarTextPacket");
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
                MAP_CHUNK_CLASS = Reflection.getClass("{play}.ClientboundLevelChunkWithLightPacket");
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
                CHUNK_BIOMES_CLASS = Reflection.getClass("{play}.ClientboundChunksBiomesPacket");
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
            @ViaProtocol.Unique(name = "actionbar", target = ViaProtocol.Unique.Target.LEGACY)
            ACTION_BAR(ACTION_BAR_CLASS, "set_action_bar_text", NetProtocolRange.since(NetProtocol.v1_17)),
            ADD_VIBRATION_SIGNAL(ADD_VIBRATION_SIGNAL_CLASS, "add_vibration_signal", NetProtocolRange.between(NetProtocol.v1_17, NetProtocol.v1_18_2)),
            @ViaProtocol.Unique(name = "advancements", target = ViaProtocol.Unique.Target.LEGACY)
            ADVANCEMENTS(ADVANCEMENTS_SERVER_CLASS, "update_advancements", NetProtocolRange.since(NetProtocol.v1_12)),
            @ViaProtocol.Unique(name = "attach_entity", target = ViaProtocol.Unique.Target.LEGACY)
            ATTACH_ENTITY(ATTACH_ENTITY_CLASS, "set_entity_link"),
            @ViaProtocol.Unique(name = "craft_recipe_response", target = ViaProtocol.Unique.Target.LEGACY)
            AUTO_RECIPE(AUTO_RECIPE_SERVER_CLASS, "place_ghost_recipe", NetProtocolRange.since(NetProtocol.v1_12_1)),
            @ViaProtocol.Unique(name = "block_action", target = ViaProtocol.Unique.Target.LEGACY)
            BLOCK_ACTION(BLOCK_ACTION_CLASS, "block_event"),
            @ViaProtocol.Unique(name = "block_break_animation", target = ViaProtocol.Unique.Target.LEGACY)
            BLOCK_BREAK_ANIMATION(BLOCK_BREAK_ANIMATION_CLASS, "block_destruction"),
            @ViaProtocol.Unique(name = "acknowledge_player_digging", target = ViaProtocol.Unique.Target.LEGACY)
            BLOCK_BROKE_ACK(BLOCK_BROKE_ACK_CLASS, "block_break_ack", NetProtocolRange.between(NetProtocol.v1_14, NetProtocol.v1_18_2)),
            @ViaProtocol.Unique(name = "block_change", target = ViaProtocol.Unique.Target.LEGACY)
            BLOCK_UPDATE(BLOCK_UPDATE_CLASS, "block_update"),
            BLOCK_CHANGED_ACK(BLOCK_CHANGED_ACK_CLASS, "block_changed_ack", NetProtocolRange.since(NetProtocol.v1_19)),
            @ViaProtocol.Unique(name = "bossbar", target = ViaProtocol.Unique.Target.LEGACY)
            BOSS_BAR(BOSS_BAR_CLASS, "boss_event", NetProtocolRange.since(NetProtocol.v1_9)),
            BUNDLE(BUNDLE_CLASS, "bundle", NetProtocolRange.since(NetProtocol.v1_19_4)),
            BUNDLE_DELIMITER(BUNDLE_DELIMITER_CLASS, "bundle_delimiter", NetProtocolRange.since(NetProtocol.v1_19_4)),
            @ViaProtocol.Unique(name = "camera", target = ViaProtocol.Unique.Target.LEGACY)
            CAMERA(CAMERA_CLASS, "set_camera"),
            CHAT_PREVIEW(CHAT_PREVIEW_SERVER_CLASS, "chat_preview", NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            @ViaProtocol.Unique(name = "chunk_biomes", target = ViaProtocol.Unique.Target.LEGACY)
            CHUNK_BIOMES(CHUNK_BIOMES_CLASS, "chunks_biomes", NetProtocolRange.since(NetProtocol.v1_19_4)),
            CHUNK_BATCH_FINISHED(CHUNK_BATCH_FINISHED_CLASS, "chunk_batch_finished", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CHUNK_BATCH_START(CHUNK_BATCH_START_CLASS, "chunk_batch_start", NetProtocolRange.since(NetProtocol.v1_20_2)),
            CLEAR_TITLES(CLEAR_TITLES_CLASS, "clear_titles", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "close_window", target = ViaProtocol.Unique.Target.LEGACY)
            CLOSE_WINDOW(CLOSE_WINDOW_SERVER_CLASS, "container_close"),
            @ViaProtocol.Unique(name = "collect_item", target = ViaProtocol.Unique.Target.LEGACY)
            COLLECT_ITEM(COLLECT_ITEM_CLASS, "take_item_entity"),
            @ViaProtocol.Unique(name = "combat_event", target = ViaProtocol.Unique.Target.LEGACY)
            COMBAT_EVENT(COMBAT_EVENT_CLASS, "player_combat", NetProtocolRange.until(NetProtocol.v1_16_5)),
            @ViaProtocol.Unique(name = "declare_commands", target = ViaProtocol.Unique.Target.LEGACY)
            COMMANDS(COMMANDS_CLASS, "commands", NetProtocolRange.since(NetProtocol.v1_13)),
            COOKIE_REQUEST(COOKIE_REQUEST_CLASS, "cookie_request", NetProtocolRange.since(NetProtocol.v1_20_6)),
            CUSTOM_CHAT_COMPLETIONS(CUSTOM_CHAT_COMPLETIONS_CLASS, "custom_chat_completions", NetProtocolRange.since(NetProtocol.v1_19_1)),
            @ViaProtocol.Unique(name = "plugin_message", target = ViaProtocol.Unique.Target.LEGACY)
            CUSTOM_PAYLOAD(CUSTOM_PAYLOAD_SERVER_CLASS, "custom_payload"),
            @ViaProtocol.Unique(name = "named_sound", target = ViaProtocol.Unique.Target.LEGACY)
            CUSTOM_SOUND_EFFECT(CUSTOM_SOUND_EFFECT_CLASS, "custom_sound", NetProtocolRange.between(NetProtocol.v1_9, NetProtocol.v1_19_2)),
            DAMAGE_EVENT(DAMAGE_EVENT_CLASS, "damage_event", NetProtocolRange.since(NetProtocol.v1_19_4)),
            DEBUG_SAMPLE(DEBUG_SAMPLE_CLASS, "debug_sample", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.Unique(name = "delete_chat_message", target = ViaProtocol.Unique.Target.LEGACY)
            DELETE_CHAT_MESSAGE(DELETE_CHAT_MESSAGE_CLASS, "delete_chat", NetProtocolRange.since(NetProtocol.v1_19_1)),
            DISCONNECT(DISCONNECT_CLASS, "disconnect"),
            DISGUISED_CHAT(DISGUISED_CHAT_CLASS, "disguised_chat", NetProtocolRange.since(NetProtocol.v1_19_3)),
            @ViaProtocol.Unique(name = "entity_movement", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY(ENTITY_CLASS, "move_entity", NetProtocolRange.until(NetProtocol.v1_16_5)),
            @ViaProtocol.Unique(name = "entity_animation", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_ANIMATION(ENTITY_ANIMATION_CLASS, "animate"),
            @ViaProtocol.Unique(name = "destroy_entities", target = ViaProtocol.Unique.Target.LEGACY, to = @NetProtocol.To(to = NetProtocol.v1_16_5))
            ENTITIES_DESTROY(ENTITY_DESTROY_CLASS, "remove_entities", NetProtocolRange.all().exclude(NetProtocol.v1_17)),
            ENTITY_DESTROY(ENTITY_DESTROY_CLASS, "remove_entity", NetProtocolRange.only(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "entity_effect", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_EFFECT(ENTITY_EFFECT_CLASS, "update_mob_effect"),
            @ViaProtocol.Unique(name = "entity_equipment", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_EQUIPMENT(ENTITY_EQUIPMENT_CLASS, "set_equipment"),
            @ViaProtocol.Unique(name = "entity_head_look", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_LOOK(ENTITY_LOOK_CLASS, "move_entity_rot"),
            @ViaProtocol.Unique(name = "entity_metadata", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_METADATA(ENTITY_METADATA_CLASS, "set_entity_data"),
            @ViaProtocol.Unique(name = "entity_position", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_POSITION(ENTITY_MOVE_CLASS, "move_entity_pos"),
            @ViaProtocol.Unique(name = "entity_position_and_rotation", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_POS_ROT(ENTITY_MOVE_LOOK_CLASS, "move_entity_pos_rot"),
            @ViaProtocol.Unique(name = "entity_rotation", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_ROTATION(ENTITY_ROTATION_CLASS, "rotate_head"),
            @ViaProtocol.Unique(name = "entity_sound", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_SOUND(ENTITY_SOUND_CLASS, "sound_entity", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "entity_status", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_STATUS(ENTITY_STATUS_CLASS, "entity_event"),
            @ViaProtocol.Unique(name = "entity_teleport", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_TELEPORT(ENTITY_TELEPORT_CLASS, "teleport_entity"),
            @ViaProtocol.Unique(name = "entity_velocity", target = ViaProtocol.Unique.Target.LEGACY)
            ENTITY_VELOCITY(ENTITY_VELOCITY_CLASS, "set_entity_motion"),
            EXPERIENCE(EXPERIENCE_CLASS, "set_experience"),
            @ViaProtocol.Unique(name = "explosion", target = ViaProtocol.Unique.Target.LEGACY)
            EXPLOSION(EXPLOSION_CLASS, "explode"),
            GAME_STATE_CHANGE(GAME_STATE_CHANGE_CLASS, "game_event"),
            @ViaProtocol.MultipleUnique(uniques = {
                    @ViaProtocol.Unique(name = "held_item_change", target = ViaProtocol.Unique.Target.LEGACY),
                    @ViaProtocol.Unique(name = "set_equipped_item", target = ViaProtocol.Unique.Target.MODERN)
            })
            HELD_ITEM_CHANGE(HELD_ITEM_CHANGE_SERVER_CLASS, "set_carried_item"),
            @ViaProtocol.Unique(name = "hit_animation", target = ViaProtocol.Unique.Target.LEGACY)
            HURT_ANIMATION(HURT_ANIMATION_CLASS, "hurt_animation", NetProtocolRange.since(NetProtocol.v1_19_4)),
            @ViaProtocol.Unique(name = "world_border_init", target = ViaProtocol.Unique.Target.LEGACY)
            INITIALIZE_BORDER(INITIALIZE_BORDER_CLASS, "initialize_border", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "join_game", target = ViaProtocol.Unique.Target.LEGACY)
            JOIN_GAME(JOIN_GAME_CLASS, "login"),
            KEEP_ALIVE(KEEP_ALIVE_SERVER_CLASS, "keep_alive"),
            @ViaProtocol.Unique(name = "update_light", target = ViaProtocol.Unique.Target.LEGACY)
            LIGHT_UPDATE(LIGHT_UPDATE_CLASS, "light_update", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "face_player", target = ViaProtocol.Unique.Target.LEGACY)
            LOOK_AT(LOOK_AT_CLASS, "player_look_at", NetProtocolRange.since(NetProtocol.v1_13)),
            MAP_BULK_CHUNK(MAP_BULK_CHUNK_CLASS, "map_bulk_chunk", NetProtocolRange.until(NetProtocol.v1_8_8)),
            @ViaProtocol.MultipleUnique(uniques = {
                    @ViaProtocol.Unique(name = "chunk_data", target = ViaProtocol.Unique.Target.LEGACY),
                    @ViaProtocol.Unique(name = "level_chunk", target = ViaProtocol.Unique.Target.MODERN, to = @NetProtocol.To(to = NetProtocol.v1_17_1)),
            })
            MAP_CHUNK(MAP_CHUNK_CLASS, "level_chunk_with_light"),
            @ViaProtocol.Unique(name = "map_data", target = ViaProtocol.Unique.Target.LEGACY)
            MAP_ITEM_DATA(MAP_ITEM_DATA_CLASS, "map_item_data"),
            MOUNT(MOUNT_CLASS, "set_passengers", NetProtocolRange.since(NetProtocol.v1_9)),
            @ViaProtocol.MultipleUnique(uniques = {
                    @ViaProtocol.Unique(name = "multi_block_change", target = ViaProtocol.Unique.Target.LEGACY),
                    @ViaProtocol.Unique(name = "chunk_blocks_update", target = ViaProtocol.Unique.Target.MODERN)
            })
            MULTI_BLOCK_CHANGE(MULTI_BLOCK_CHANGE_CLASS, "section_blocks_update"),
            @ViaProtocol.Unique(name = "nbt_query", target = ViaProtocol.Unique.Target.LEGACY)
            NBT_QUERY(NBT_QUERY_CLASS, "tag_query", NetProtocolRange.since(NetProtocol.v1_13)),
            OPEN_BOOK(OPEN_BOOK_CLASS, "open_book", NetProtocolRange.since(NetProtocol.v1_14)),
            OPEN_SIGN_EDITOR(OPEN_SIGN_EDITOR_CLASS, "open_sign_editor"),
            @ViaProtocol.Unique(name = "open_window", target = ViaProtocol.Unique.Target.LEGACY)
            OPEN_WINDOW(OPEN_WINDOW_CLASS, "open_screen"),
            @ViaProtocol.Unique(name = "open_horse_window", target = ViaProtocol.Unique.Target.LEGACY)
            OPEN_WINDOW_HORSE(OPEN_WINDOW_HORSE_CLASS, "horse_screen_open", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "trade_list", target = ViaProtocol.Unique.Target.LEGACY)
            OPEN_WINDOW_MERCHANT(OPEN_WINDOW_MERCHANT_CLASS, "merchant_offers", NetProtocolRange.since(NetProtocol.v1_14)),
            PING(PING_CLASS, "ping", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.MultipleUnique(uniques = {
                    @ViaProtocol.Unique(name = "chat_message", target = ViaProtocol.Unique.Target.LEGACY),
                    @ViaProtocol.Unique(name = "chat", target = ViaProtocol.Unique.Target.MODERN)
            })
            PLAYER_CHAT(PLAYER_CHAT_CLASS, "player_chat"),
            PLAYER_CHAT_HEADER(PLAYER_CHAT_HEADER_CLASS, "player_chat_header", NetProtocolRange.between(NetProtocol.v1_19_1, NetProtocol.v1_19_2)),
            @ViaProtocol.Unique(name = "combat_end", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_COMBAT_END(PLAYER_COMBAT_END_CLASS, "player_combat_end", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "combat_enter", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_COMBAT_ENTER(PLAYER_COMBAT_ENTER_CLASS, "player_combat_enter", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "combat_kill", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_COMBAT_KILL(PLAYER_COMBAT_KILL_CLASS, "player_combat_kill", NetProtocolRange.since(NetProtocol.v1_17)),
            PLAYER_INFO(PLAYER_INFO_CLASS, "player_info", NetProtocolRange.until(NetProtocol.v1_19_2)),
            PLAYER_INFO_REMOVE(PLAYER_INFO_REMOVE_CLASS, "player_info_remove", NetProtocolRange.since(NetProtocol.v1_19_3)),
            PLAYER_INFO_UPDATE(PLAYER_INFO_UPDATE_CLASS, "player_info_update", NetProtocolRange.since(NetProtocol.v1_19_3)),
            PLAYER_POSITION(PLAYER_POSITION_SERVER_CLASS, "player_position"),
            @ViaProtocol.Unique(name = "use_bed", target = ViaProtocol.Unique.Target.LEGACY)
            PLAYER_SLEEP(PLAYER_SLEEP_CLASS, "player_sleep", NetProtocolRange.until(NetProtocol.v1_13_2)),
            PONG(PONG_CLASS, "pong_response", NetProtocolRange.since(NetProtocol.v1_20_2)),
            PROJECTILE_POWER(PROJECTILE_POWER_CLASS, "projectile_power", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.Unique(name = "unlock_recipes", target = ViaProtocol.Unique.Target.LEGACY)
            RECIPES(RECIPES_CLASS, "recipe", NetProtocolRange.since(NetProtocol.v1_12)),
            @ViaProtocol.Unique(name = "declare_recipes", target = ViaProtocol.Unique.Target.LEGACY)
            RECIPE_UPDATE(RECIPE_UPDATE_CLASS, "update_recipes", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "remove_entity_effect", target = ViaProtocol.Unique.Target.LEGACY)
            REMOVE_ENTITY_EFFECT(REMOVE_ENTITY_EFFECT_CLASS, "remove_mob_effect"),
            RESET_SCORE(RESET_SCORE_CLASS, "reset_score", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_POP(RESOURCE_PACK_POP_CLASS, "resource_pack_pop", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_PUSH(RESOURCE_PACK_PUSH_CLASS, "resource_pack_push", NetProtocolRange.since(NetProtocol.v1_20_4)),
            RESOURCE_PACK_SEND(RESOURCE_PACK_SEND_CLASS, "resource_pack", NetProtocolRange.until(NetProtocol.v1_20_2)),
            RESPAWN(RESPAWN_CLASS, "respawn"),
            @ViaProtocol.Unique(name = "display_scoreboard", target = ViaProtocol.Unique.Target.LEGACY)
            SCOREBOARD_DISPLAY_OBJECTIVE(SCOREBOARD_DISPLAY_OBJECTIVE_CLASS, "set_display_objective"),
            @ViaProtocol.Unique(name = "scoreboard_objective", target = ViaProtocol.Unique.Target.LEGACY)
            SCOREBOARD_OBJECTIVE(SCOREBOARD_OBJECTIVE_CLASS, "set_objective"),
            @ViaProtocol.Unique(name = "update_score", target = ViaProtocol.Unique.Target.LEGACY)
            SCOREBOARD_SCORE(SCOREBOARD_SCORE_CLASS, "set_score"),
            @ViaProtocol.Unique(name = "teams", target = ViaProtocol.Unique.Target.LEGACY)
            SCOREBOARD_TEAM(SCOREBOARD_TEAM_CLASS, "set_player_team"),
            SELECT_ADVANCEMENT_TAB(SELECT_ADVANCEMENT_TAB_CLASS, "select_advancements_tab", NetProtocolRange.since(NetProtocol.v1_12)),
            SERVER_DATA(SERVER_DATA_CLASS, "server_data", NetProtocolRange.since(NetProtocol.v1_19)),
            @ViaProtocol.Unique(name = "server_difficulty", target = ViaProtocol.Unique.Target.LEGACY)
            SERVER_DIFFICULTY(SERVER_DIFFICULTY_CLASS, "change_difficulty"),
            @ViaProtocol.Unique(name = "world_border_size", target = ViaProtocol.Unique.Target.LEGACY)
            SET_BORDER_SIZE(SET_BORDER_SIZE_CLASS, "set_border_size", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "world_border_warning_delay", target = ViaProtocol.Unique.Target.LEGACY)
            SET_BORDER_WARNING_DELAY(SET_BORDER_WARNING_DELAY_CLASS, "set_border_warning_delay", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "world_border_warning_distance", target = ViaProtocol.Unique.Target.LEGACY)
            SET_BORDER_WARNING_DISTANCE(SET_BORDER_WARNING_DISTANCE_CLASS, "set_border_warning_distance", NetProtocolRange.since(NetProtocol.v1_17)),
            SET_COMPRESSION(SET_COMPRESSION_CLASS, "set_compression", NetProtocolRange.until(NetProtocol.v1_8_8)),
            SET_COOLDOWN(SET_COOLDOWN_CLASS, "cooldown", NetProtocolRange.since(NetProtocol.v1_9)),
            SET_DISPLAY_CHAT_PREVIEW(SET_DISPLAY_CHAT_PREVIEW_CLASS, "set_display_chat_preview", NetProtocolRange.between(NetProtocol.v1_19, NetProtocol.v1_19_2)),
            SET_SIMULATION_DISTANCE(SET_SIMULATION_DISTANCE_CLASS, "set_simulation_distance", NetProtocolRange.since(NetProtocol.v1_18)),
            @ViaProtocol.Unique(name = "set_slot", target = ViaProtocol.Unique.Target.LEGACY)
            SET_SLOT(SET_SLOT_CLASS, "container_set_slot"),
            @ViaProtocol.Unique(name = "title_subtitle", target = ViaProtocol.Unique.Target.LEGACY)
            SET_SUBTITLE_TEXT(SET_SUBTITLE_TEXT_CLASS, "set_subtitle_text", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "title_times", target = ViaProtocol.Unique.Target.LEGACY)
            SET_TITLES_ANIMATION(SET_TITLES_ANIMATION_CLASS, "set_titles_animation", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "title_text", target = ViaProtocol.Unique.Target.LEGACY)
            SET_TITLE_TEXT(SET_TITLE_TEXT_CLASS, "set_title_text", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.MultipleUnique(uniques = {
                    @ViaProtocol.Unique(name = "named_sound", target = ViaProtocol.Unique.Target.LEGACY, to = @NetProtocol.To(to = NetProtocol.v1_8_8)),
                    @ViaProtocol.Unique(name = "custom_sound", target = ViaProtocol.Unique.Target.MODERN, to = @NetProtocol.To(to = NetProtocol.v1_8_8))
                    // Is this a ViaVersion naming bug?
                    // These are the names of the CUSTOM_SOUND packet which came in 1.9
            })
            SOUND(SOUND_CLASS, "sound"),
            @ViaProtocol.Unique(name = "spawn_entity", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_ENTITY(SPAWN_ENTITY_CLASS, "add_entity"),
            @ViaProtocol.Unique(name = "spawn_mob", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_ENTITY_LIVING(SPAWN_ENTITY_LIVING_CLASS, "add_mob", NetProtocolRange.until(NetProtocol.v1_18_2)),
            @ViaProtocol.Unique(name = "spawn_painting", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_ENTITY_PAINTING(SPAWN_ENTITY_PAINTING_CLASS, "add_painting", NetProtocolRange.until(NetProtocol.v1_18_2)),
            @ViaProtocol.Unique(name = "spawn_global_entity", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_GLOBAL_ENTITY(SPAWN_GLOBAL_ENTITY_CLASS, "add_global_entity", NetProtocolRange.until(NetProtocol.v1_15_2)),
            @ViaProtocol.Unique(name = "spawn_experience_orb", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_EXPERIENCE_ORB(SPAWN_EXPERIENCE_ORB_CLASS, "add_experience_orb"),
            @ViaProtocol.Unique(name = "spawn_player", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_NAMED_ENTITY(SPAWN_NAMED_ENTITY_CLASS, "add_player", NetProtocolRange.until(NetProtocol.v1_20_1)),
            @ViaProtocol.Unique(name = "spawn_particle", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_PARTICLE(SPAWN_PARTICLE_CLASS, "level_particles"),
            @ViaProtocol.Unique(name = "spawn_position", target = ViaProtocol.Unique.Target.LEGACY)
            SPAWN_POSITION(SPAWN_POSITION_CLASS, "set_default_spawn_position"),
            START_CONFIGURATION(START_CONFIGURATION_CLASS, "start_configuration", NetProtocolRange.since(NetProtocol.v1_20_2)),
            @ViaProtocol.Unique(name = "statistics", target = ViaProtocol.Unique.Target.LEGACY)
            STATISTICS(STATISTICS_CLASS, "award_stats"),
            STOP_SOUND(STOP_SOUND_CLASS, "stop_sound", NetProtocolRange.since(NetProtocol.v1_13)),
            STORE_COOKIE(STORE_COOKIE_CLASS, "store_cookie", NetProtocolRange.since(NetProtocol.v1_20_6)),
            SYSTEM_CHAT(SYSTEM_CHAT_CLASS, "system_chat", NetProtocolRange.since(NetProtocol.v1_19)),
            @ViaProtocol.Unique(name = "tab_complete", target = ViaProtocol.Unique.Target.LEGACY)
            TAB_COMPLETE(TAB_COMPLETE_SERVER_CLASS, "command_suggestions"),
            TAB_LIST(TAB_LIST_CLASS, "tab_list"),
            TICKING_STATE(TICKING_STATE_CLASS, "ticking_state", NetProtocolRange.since(NetProtocol.v1_20_4)),
            TICKING_STEP(TICKING_STEP_CLASS, "ticking_step", NetProtocolRange.since(NetProtocol.v1_20_4)),
            TILE_ENTITY_DATA(TILE_ENTITY_DATA_CLASS, "block_entity_data"),
            @ViaProtocol.Unique(name = "time_update", target = ViaProtocol.Unique.Target.LEGACY)
            TIME_UPDATE(TIME_UPDATE_CLASS, "set_time"),
            @ViaProtocol.Unique(name = "title", target = ViaProtocol.Unique.Target.LEGACY)
            TITLE(TITLE_CLASS, "set_titles", NetProtocolRange.until(NetProtocol.v1_16_5)),
            @ViaProtocol.Unique(name = "window_confirmation", target = ViaProtocol.Unique.Target.LEGACY)
            TRANSACTION(TRANSACTION_SERVER_CLASS, "container_ack", NetProtocolRange.until(NetProtocol.v1_16_5)),
            TRANSFER(TRANSFER_CLASS, "transfer", NetProtocolRange.since(NetProtocol.v1_20_6)),
            @ViaProtocol.Unique(name = "unload_chunk", target = ViaProtocol.Unique.Target.LEGACY)
            UNLOAD_CHUNK(UNLOAD_CHUNK_CLASS, "forget_level_chunk", NetProtocolRange.since(NetProtocol.v1_9)),
            @ViaProtocol.Unique(name = "entity_properties", target = ViaProtocol.Unique.Target.LEGACY)
            UPDATE_ENTITY_ATTRIBUTES(UPDATE_ATTRIBUTES_CLASS, "update_attributes"),
            UPDATE_ENABLED_FEATURES(UPDATE_ENABLED_FEATURES_CLASS, "update_enabled_features", NetProtocolRange.between(NetProtocol.v1_19_3, NetProtocol.v1_20_1)),
            UPDATE_ENTITY_NBT(UPDATE_ENTITY_NBT_CLASS, "update_entity_nbt", NetProtocolRange.until(NetProtocol.v1_8_8)),
            @ViaProtocol.Unique(name = "update_health", target = ViaProtocol.Unique.Target.LEGACY)
            UPDATE_HEALTH(UPDATE_HEALTH_CLASS, "set_health"),
            UPDATE_SIGN(UPDATE_SIGN_SERVER_CLASS, "update_sign", NetProtocolRange.until(NetProtocol.v1_9_2)),
            @ViaProtocol.Unique(name = "tags", target = ViaProtocol.Unique.Target.LEGACY)
            UPDATE_TAGS(UPDATE_TAGS_CLASS, "update_tags", NetProtocolRange.since(NetProtocol.v1_13)),
            @ViaProtocol.Unique(name = "vehicle_move", target = ViaProtocol.Unique.Target.LEGACY)
            VEHICLE_MOVE(VEHICLE_MOVE_SERVER_CLASS, "move_vehicle", NetProtocolRange.since(NetProtocol.v1_9)),
            @ViaProtocol.Unique(name = "update_view_position", target = ViaProtocol.Unique.Target.LEGACY)
            VIEW_CENTER(VIEW_CENTER_CLASS, "set_chunk_cache_center", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "update_view_distance", target = ViaProtocol.Unique.Target.LEGACY)
            VIEW_DISTANCE(VIEW_DISTANCE_CLASS, "set_chunk_cache_radius", NetProtocolRange.since(NetProtocol.v1_14)),
            @ViaProtocol.Unique(name = "window_property", target = ViaProtocol.Unique.Target.LEGACY)
            WINDOW_DATA(WINDOW_DATA_CLASS, "container_set_data"),
            @ViaProtocol.Unique(name = "window_items", target = ViaProtocol.Unique.Target.LEGACY)
            WINDOW_ITEMS(WINDOW_ITEMS_CLASS, "container_set_content"),
            @ViaProtocol.Unique(name = "world_border", target = ViaProtocol.Unique.Target.LEGACY)
            WORLD_BORDER(WORLD_BORDER_CLASS, "set_border", NetProtocolRange.until(NetProtocol.v1_16_5)),
            @ViaProtocol.Unique(name = "world_border_center", target = ViaProtocol.Unique.Target.LEGACY)
            WORLD_BORDER_CENTER(WORLD_BORDER_CENTER_CLASS, "set_border_center", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "world_border_lerp_size", target = ViaProtocol.Unique.Target.LEGACY)
            WORLD_BORDER_LERP_SIZE(WORLD_BORDER_LERP_SIZE_CLASS, "set_border_lerp_size", NetProtocolRange.since(NetProtocol.v1_17)),
            @ViaProtocol.Unique(name = "effect", target = ViaProtocol.Unique.Target.LEGACY)
            WORLD_EVENT(WORLD_EVENT_CLASS, "level_event"),
            ;

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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

            private final LazyInit<Integer> minecraftIdInit = new LazyInit<>(() -> getMinecraftId(this));
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
