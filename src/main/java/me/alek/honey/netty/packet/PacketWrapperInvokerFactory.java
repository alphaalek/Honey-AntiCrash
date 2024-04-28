package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.CommonPacketStateTypeEnum;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInCustomPayload;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.packetwrappers.login.server.*;
import me.alek.honey.packetwrappers.play.client.*;
import me.alek.honey.packetwrappers.play.server.*;
import me.alek.honey.packetwrappers.status.client.WrappedStatusInPing;
import me.alek.honey.packetwrappers.status.client.WrappedStatusInStart;
import me.alek.honey.packetwrappers.status.server.WrappedStatusOutPong;
import me.alek.honey.packetwrappers.status.server.WrappedStatusOutServerInfo;
import me.alek.honey.netty.util.NetProtocol;
import org.bukkit.Bukkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PacketWrapperInvokerFactory {

    private static class BufferedMapCreator<K, V> {

        private Map<K, V> streamConcurrentMap(Iterable<K> iterable, Function<K, V> function) {
            return streamMap(new ConcurrentHashMap<>(), iterable, function);
        }

        private Map<K, V> streamHashMap(Iterable<K> iterable, Function<K, V> function) {
            return streamMap(new HashMap<>(), iterable, function);
        }

        private Map<K, V> streamMap(Map<K, V> map, Iterable<K> iterable, Function<K, V> function) {
            for (K key : iterable) {
                V applied = function.apply(key);
                if (applied != null) {
                    map.put(key, applied);
                }
            }
            return map;
        }
    }

    public static void load() {
        final BufferedMapCreator<PacketTypeEnum, MethodHandle> mapCreator = new BufferedMapCreator<>();

        for (Method method : PacketWrapperInvokerFactory.class.getDeclaredMethods()) {
            PacketFactory wrappedPacket = method.getAnnotation(PacketFactory.class);
            if (wrappedPacket == null) {
                continue;
            }
            try {
                MethodType methodType = MethodType.methodType(wrappedPacket.clazz(), Object.class, PacketContainer.class);
                MethodHandle methodHandle = LOOKUP.findStatic(PacketWrapperInvokerFactory.class, method.getName(), methodType);

                INVOKERS.put(wrappedPacket.value(), methodHandle);
            } catch (Exception ex) {
                ex.printStackTrace();
                Bukkit.getLogger().severe("Error occurred at finding invoker for \"" + wrappedPacket.value() + "\".");
            }
        }

        PLAY = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PROTOCOL_VERSION, PacketState.PLAY), (packetType) -> {
            if (packetType.getNmsClass() == null) HoneyPlugin.get().getPlugin().getLogger().severe("Found wrong packet mapping: " + packetType.getClass().getName() + " " + packetType.getPacketId());
            return getCachedInvoker(packetType, packetType.getNmsClass().getSimpleName());
        });
        HANDSHAKE = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PROTOCOL_VERSION, PacketState.HANDSHAKE), (packetType) -> {
            if (packetType.getNmsClass() == null) HoneyPlugin.get().getPlugin().getLogger().severe("Found wrong packet mapping: " + packetType.getClass().getName() + " " + packetType.getPacketId());
            return getCachedInvoker(packetType, packetType.getNmsClass().getSimpleName());
        });
        LOGIN = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PROTOCOL_VERSION, PacketState.LOGIN), (packetType) -> {
            if (packetType.getNmsClass() == null) HoneyPlugin.get().getPlugin().getLogger().severe("Found wrong packet mapping: " + packetType.getClass().getName() + " " + packetType.getPacketId());
            return getCachedInvoker(packetType, packetType.getNmsClass().getSimpleName());
        });
        STATUS = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PROTOCOL_VERSION, PacketState.STATUS), (packetType) -> {
            if (packetType.getNmsClass() == null) HoneyPlugin.get().getPlugin().getLogger().severe("Found wrong packet mapping: " + packetType.getClass().getName() + " " + packetType.getPacketId());
            return getCachedInvoker(packetType, packetType.getNmsClass().getSimpleName());
        });
    }

    private static final NetProtocol PROTOCOL_VERSION = NetProtocol.getProtocol();
    private static final Map<PacketTypeEnum, String> NAMES_FROM_TYPE = new HashMap<>();
    private static final Map<String, PacketTypeEnum> NAMES_TO_TYPE = new HashMap<>();
    private static final Map<String, MethodHandle> INVOKERS = new HashMap<>();
    private static Map<PacketTypeEnum, MethodHandle> PLAY;
    private static Map<PacketTypeEnum, MethodHandle> HANDSHAKE;
    private static Map<PacketTypeEnum, MethodHandle> LOGIN;
    private static Map<PacketTypeEnum, MethodHandle> STATUS;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static MethodHandle getCachedInvoker(PacketTypeEnum packetType, String name) {
        NAMES_FROM_TYPE.put(packetType, name);
        NAMES_TO_TYPE.put(name, packetType);
        if (INVOKERS.get(name) != null) {
            return INVOKERS.get(name);
        }

        return null;
    }

    public static MethodHandle createWrapperInvoker(PacketTypeEnum packetType, Class<?> nmsClass) {
        PacketState state = !packetType.isCommonState() ? packetType.getState() : ((CommonPacketStateTypeEnum)packetType).getCommonStates().get(0);
        switch (state) {
            case PLAY:
                return createPlayWrapperInvoker(nmsClass);
            case HANDSHAKE:
                return createHandshakeWrapper(nmsClass);
            case LOGIN:
                return createLoginWrapper(nmsClass);
            case STATUS:
                return createStatusWrapper(nmsClass);
            default:
                return null;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface PacketFactory {

        String value();
        
        Class<?> clazz();
    }

    public static String getNameFor(PacketTypeEnum packetType) {
        return NAMES_FROM_TYPE.get(packetType);
    }

    public static PacketTypeEnum getPacketTypeForName(String name) {
        return NAMES_TO_TYPE.get(name);
    }

    public static PacketTypeEnum getPacketTypeForNameIgnoreCase(String name) {
        for (Map.Entry<String, PacketTypeEnum> packetTypeEntry : NAMES_TO_TYPE.entrySet()) {
            if (packetTypeEntry.getKey().equalsIgnoreCase(name))
                return packetTypeEntry.getValue();
        }
        return null;
    }

    private static MethodHandle createWrapperInvoker(Class<?> nmsClass, Map<PacketTypeEnum, MethodHandle> map) {
        final PacketTypeEnum packetType = PacketType.getPacketType(nmsClass);
        if (packetType == PacketType.UNKNOWN || map == null) {
            return null;
        }

        return map.get(packetType);
    }

    public static MethodHandle createPlayWrapperInvoker(Class<?> nmsClass) {
        return createWrapperInvoker(nmsClass, PLAY);
    }

    public static MethodHandle createHandshakeWrapper(Class<?> nmsClass) {
        return createWrapperInvoker(nmsClass, HANDSHAKE);
    }

    public static MethodHandle createLoginWrapper(Class<?> nmsClass) {
        return createWrapperInvoker(nmsClass, LOGIN);
    }

    public static MethodHandle createStatusWrapper(Class<?> nmsClass) {
        return createWrapperInvoker(nmsClass, STATUS);
    }

    @PacketFactory(value = "PacketHandshakingInSetProtocol", clazz = WrappedHandshakeInSetProtocol.class)
    public static WrappedHandshakeInSetProtocol handshakeInSetProtocol(Object rawPacket, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        return new WrappedHandshakeInSetProtocol(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginInCustomPayload", clazz = WrappedLoginInCustomPayload.class)
    public static WrappedLoginInCustomPayload loginInCustomPayload(Object rawPacket, PacketContainer<WrappedLoginInCustomPayload> packetContainer) {
        return new WrappedLoginInCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginInEncryptionBegin", clazz = WrappedLoginInEncryptionResponse.class)
    public static WrappedLoginInEncryptionResponse loginInEncryptionResponse(Object rawPacket, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {
        return new WrappedLoginInEncryptionResponse(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginInStart", clazz = WrappedLoginInLoginStart.class)
    public static WrappedLoginInLoginStart loginInLoginStart(Object rawPacket, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
        return new WrappedLoginInLoginStart(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginOutCustomPayload", clazz = WrappedLoginOutCustomPayload.class)
    public static WrappedLoginOutCustomPayload loginOutCustomPayload(Object rawPacket, PacketContainer<WrappedLoginOutCustomPayload> packetContainer) {
        return new WrappedLoginOutCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginOutDisconnect", clazz = WrappedLoginOutDisconnect.class)
    public static WrappedLoginOutDisconnect loginOutDisconnect(Object rawPacket, PacketContainer<WrappedLoginOutDisconnect> packetContainer) {
        return new WrappedLoginOutDisconnect(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginOutEncryptionBegin", clazz = WrappedLoginOutEncryptionRequest.class)
    public static WrappedLoginOutEncryptionRequest loginOutEncryptionRequest(Object rawPacket, PacketContainer<WrappedLoginOutEncryptionRequest> packetContainer) {
        return new WrappedLoginOutEncryptionRequest(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginOutSetCompression", clazz = WrappedLoginOutSetCompression.class)
    public static WrappedLoginOutSetCompression loginOutSetCompression(Object rawPacket, PacketContainer<WrappedLoginOutSetCompression> packetContainer) {
        return new WrappedLoginOutSetCompression(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketLoginOutSuccess", clazz = WrappedLoginOutSuccess.class)
    public static WrappedLoginOutSuccess loginOutSuccess(Object rawPacket, PacketContainer<WrappedLoginOutSuccess> packetContainer) {
        return new WrappedLoginOutSuccess(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketStatusInPing", clazz = WrappedStatusInPing.class)
    public static WrappedStatusInPing statusInPing(Object rawPacket, PacketContainer<WrappedStatusInPing> packetContainer) {
        return new WrappedStatusInPing(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketStatusInStart", clazz = WrappedStatusInStart.class)
    public static WrappedStatusInStart statusInStart(Object rawPacket, PacketContainer<WrappedStatusInStart> packetContainer) {
        return new WrappedStatusInStart(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketStatusOutPong", clazz = WrappedStatusOutPong.class)
    public static WrappedStatusOutPong statusOutPong(Object rawPacket, PacketContainer<WrappedStatusOutPong> packetContainer) {
        return new WrappedStatusOutPong(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketStatusOutServerInfo", clazz = WrappedStatusOutServerInfo.class)
    public static WrappedStatusOutServerInfo statusOutServerInfo(Object rawPacket, PacketContainer<WrappedStatusOutServerInfo> packetContainer) {
        return new WrappedStatusOutServerInfo(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInAbilities", clazz = WrappedPlayInAbilities.class)
    public static WrappedPlayInAbilities playInAbilities(Object rawPacket, PacketContainer<WrappedPlayInAbilities> packetContainer) {
        return new WrappedPlayInAbilities(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInAdvancements", clazz = WrappedPlayInAdvancements.class)
    public static WrappedPlayInAdvancements playInAdvancements(Object rawPacket, PacketContainer<WrappedPlayInAdvancements> packetContainer) {
        return new WrappedPlayInAdvancements(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInArmAnimation", clazz = WrappedPlayInArmAnimation.class)
    public static WrappedPlayInArmAnimation playInArmAnimation(Object rawPacket, PacketContainer<WrappedPlayInArmAnimation> packetContainer) {
        return new WrappedPlayInArmAnimation(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInAutoRecipe", clazz = WrappedPlayInAutoRecipe.class)
    public static WrappedPlayInAutoRecipe playInAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayInAutoRecipe> packetContainer) {
        return new WrappedPlayInAutoRecipe(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInBeacon", clazz = WrappedPlayInBeacon.class)
    public static WrappedPlayInBeacon playInBeacon(Object rawPacket, PacketContainer<WrappedPlayInBeacon> packetContainer) {
        return new WrappedPlayInBeacon(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInBEdit", clazz = WrappedPlayInBEdit.class)
    public static WrappedPlayInBEdit playInBEdit(Object rawPacket, PacketContainer<WrappedPlayInBEdit> packetContainer) {
        return new WrappedPlayInBEdit(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInBlockDig", clazz = WrappedPlayInBlockDig.class)
    public static WrappedPlayInBlockDig playInBlockDig(Object rawPacket, PacketContainer<WrappedPlayInBlockDig> packetContainer) {
        return new WrappedPlayInBlockDig(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInBlockPlace", clazz = WrappedPlayInBlockPlace.class)
    public static WrappedPlayInBlockPlace playInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        return new WrappedPlayInBlockPlace(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInBoatMove", clazz = WrappedPlayInBoatMove.class)
    public static WrappedPlayInBoatMove playInBoatMove(Object rawPacket, PacketContainer<WrappedPlayInBoatMove> packetContainer) {
        return new WrappedPlayInBoatMove(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInChat", clazz = WrappedPlayInChat.class)
    public static WrappedPlayInChat playInChat(Object rawPacket, PacketContainer<WrappedPlayInChat> packetContainer) {
        return new WrappedPlayInChat(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInClientCommand", clazz = WrappedPlayInClientCommand.class)
    public static WrappedPlayInClientCommand playInClientCommand(Object rawPacket, PacketContainer<WrappedPlayInClientCommand> packetContainer) {
        return new WrappedPlayInClientCommand(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInCloseWindow", clazz = WrappedPlayInCloseWindow.class)
    public static WrappedPlayInCloseWindow playInCloseWindow(Object rawPacket, PacketContainer<WrappedPlayInCloseWindow> packetContainer) {
        return new WrappedPlayInCloseWindow(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInCustomPayload", clazz = WrappedPlayInCustomPayload.class)
    public static WrappedPlayInCustomPayload playInCustomPayload(Object rawPacket, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        return new WrappedPlayInCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInDifficultyChange", clazz = WrappedPlayInDifficultyChange.class)
    public static WrappedPlayInDifficultyChange playInDifficultyChange(Object rawPacket, PacketContainer<WrappedPlayInDifficultyChange> packetContainer) {
        return new WrappedPlayInDifficultyChange(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInDifficultyLock", clazz = WrappedPlayInDifficultyLock.class)
    public static WrappedPlayInDifficultyLock playInDifficultyLock(Object rawPacket, PacketContainer<WrappedPlayInDifficultyLock> packetContainer) {
        return new WrappedPlayInDifficultyLock(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInEnchantItem", clazz = WrappedPlayInEnchantItem.class)
    public static WrappedPlayInEnchantItem playInEnchantItem(Object rawPacket, PacketContainer<WrappedPlayInEnchantItem> packetContainer) {
        return new WrappedPlayInEnchantItem(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInEntityAction", clazz = WrappedPlayInEntityAction.class)
    public static WrappedPlayInEntityAction playInEntityAction(Object rawPacket, PacketContainer<WrappedPlayInEntityAction> packetContainer) {
        return new WrappedPlayInEntityAction(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInEntityNBTQuery", clazz = WrappedPlayInEntityNBTQuery.class)
    public static WrappedPlayInEntityNBTQuery playInEntityNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInEntityNBTQuery> packetContainer) {
        return new WrappedPlayInEntityNBTQuery(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInFlying", clazz = WrappedPlayInFlying.class)
    public static <WP extends WrappedPlayInFlying<WP>> WrappedPlayInFlying<WP> playInFlying(Object rawPacket, PacketContainer<WP> packetContainer) {
        return new WrappedPlayInFlying<>(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInFlying$d", clazz = WrappedPlayInGround.class)
    public static WrappedPlayInGround playInGround(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        return new WrappedPlayInGround(rawPacket, packetContainer);
    }

    @PacketFactory(value = "d", clazz = WrappedPlayInGround.class)
    public static WrappedPlayInGround playInGround_v1_17(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        return new WrappedPlayInGround(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInHeldItemSlot", clazz = WrappedPlayInHeldItemSlot.class)
    public static WrappedPlayInHeldItemSlot playInHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {
        return new WrappedPlayInHeldItemSlot(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInItemName", clazz = WrappedPlayInItemName.class)
    public static WrappedPlayInItemName playInItemName(Object rawPacket, PacketContainer<WrappedPlayInItemName> packetContainer) {
        return new WrappedPlayInItemName(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInJigsawGenerate", clazz = WrappedPlayInJigsawGenerate.class)
    public static WrappedPlayInJigsawGenerate playInJigsawGenerate(Object rawPacket, PacketContainer<WrappedPlayInJigsawGenerate> packetContainer) {
        return new WrappedPlayInJigsawGenerate(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInKeepAlive", clazz = WrappedPlayInKeepAlive.class)
    public static WrappedPlayInKeepAlive playInKeepAlive(Object rawPacket, PacketContainer<WrappedPlayInKeepAlive> packetContainer) {
        return new WrappedPlayInKeepAlive(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInFlying$PacketPlayInLook", clazz = WrappedPlayInLook.class)
    public static WrappedPlayInLook playInLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        return new WrappedPlayInLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInLook", clazz = WrappedPlayInLook.class)
    public static WrappedPlayInLook playInLook(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        return new WrappedPlayInLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInPickItem", clazz = WrappedPlayInPickItem.class)
    public static WrappedPlayInPickItem playInPickItem(Object rawPacket, PacketContainer<WrappedPlayInPickItem> packetContainer) {
        return new WrappedPlayInPickItem(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInFlying$PacketPlayInPosition", clazz = WrappedPlayInPosition.class)
    public static WrappedPlayInPosition playInPosition_v1_17(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        return new WrappedPlayInPosition(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInPosition", clazz = WrappedPlayInPosition.class)
    public static WrappedPlayInPosition playInPosition(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        return new WrappedPlayInPosition(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInFlying$PacketPlayInPositionLook", clazz = WrappedPlayInPositionLook.class)
    public static WrappedPlayInPositionLook playInPositionLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        return new WrappedPlayInPositionLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInPositionLook", clazz = WrappedPlayInPositionLook.class)
    public static WrappedPlayInPositionLook playInPositionLook(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        return new WrappedPlayInPositionLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInRecipeDisplayed", clazz = WrappedPlayInRecipeDisplayed.class)
    public static WrappedPlayInRecipeDisplayed playInRecipeDisplayed(Object rawPacket, PacketContainer<WrappedPlayInRecipeDisplayed> packetContainer) {
        return new WrappedPlayInRecipeDisplayed(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInRecipeSettings", clazz = WrappedPlayInRecipeSettings.class)
    public static WrappedPlayInRecipeSettings playInRecipeSettings(Object rawPacket, PacketContainer<WrappedPlayInRecipeSettings> packetContainer) {
        return new WrappedPlayInRecipeSettings(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInResourcePackStatus", clazz = WrappedPlayInResourcePackStatus.class)
    public static WrappedPlayInResourcePackStatus playInResourcePackStatus(Object rawPacket, PacketContainer<WrappedPlayInResourcePackStatus> packetContainer) {
        return new WrappedPlayInResourcePackStatus(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSetCommandBlock", clazz = WrappedPlayInSetCommandBlock.class)
    public static WrappedPlayInSetCommandBlock playInSetCommandBlock(Object rawPacket, PacketContainer<WrappedPlayInSetCommandBlock> packetContainer) {
        return new WrappedPlayInSetCommandBlock(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSetCommandMinecart", clazz = WrappedPlayInSetCommandMinecart.class)
    public static WrappedPlayInSetCommandMinecart playInSetCommandMinecart(Object rawPacket, PacketContainer<WrappedPlayInSetCommandMinecart> packetContainer) {
        return new WrappedPlayInSetCommandMinecart(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSetCreativeSlot", clazz = WrappedPlayInSetCreativeSlot.class)
    public static WrappedPlayInSetCreativeSlot playInSetCreativeSlot(Object rawPacket, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {
        return new WrappedPlayInSetCreativeSlot(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSetJigsaw", clazz = WrappedPlayInSetJigsaw.class)
    public static WrappedPlayInSetJigsaw playInSetJigsaw(Object rawPacket, PacketContainer<WrappedPlayInSetJigsaw> packetContainer) {
        return new WrappedPlayInSetJigsaw(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSettings", clazz = WrappedPlayInSettings.class)
    public static WrappedPlayInSettings playInSettings(Object rawPacket, PacketContainer<WrappedPlayInSettings> packetContainer) {
        return new WrappedPlayInSettings(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSpectate", clazz = WrappedPlayInSpectate.class)
    public static WrappedPlayInSpectate playInSpectate(Object rawPacket, PacketContainer<WrappedPlayInSpectate> packetContainer) {
        return new WrappedPlayInSpectate(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInSteerVehicle", clazz = WrappedPlayInSteerVehicle.class)
    public static WrappedPlayInSteerVehicle playInSteerVehicle(Object rawPacket, PacketContainer<WrappedPlayInSteerVehicle> packetContainer) {
        return new WrappedPlayInSteerVehicle(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInStruct", clazz = WrappedPlayInStruct.class)
    public static WrappedPlayInStruct playInStruct(Object rawPacket, PacketContainer<WrappedPlayInStruct> packetContainer) {
        return new WrappedPlayInStruct(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInTabComplete", clazz = WrappedPlayInTabComplete.class)
    public static WrappedPlayInTabComplete playInTabComplete(Object rawPacket, PacketContainer<WrappedPlayInTabComplete> packetContainer) {
        return new WrappedPlayInTabComplete(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInTeleportAccept", clazz = WrappedPlayInTeleportAccept.class)
    public static WrappedPlayInTeleportAccept playInTeleportAccept(Object rawPacket, PacketContainer<WrappedPlayInTeleportAccept> packetContainer) {
        return new WrappedPlayInTeleportAccept(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInTileNBTQuery", clazz = WrappedPlayInTileNBTQuery.class)
    public static WrappedPlayInTileNBTQuery playInTileNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInTileNBTQuery> packetContainer) {
        return new WrappedPlayInTileNBTQuery(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInTransaction", clazz = WrappedPlayInTransaction.class)
    public static WrappedPlayInTransaction playInTransaction(Object rawPacket, PacketContainer<WrappedPlayInTransaction> packetContainer) {
        return new WrappedPlayInTransaction(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInTrSel", clazz = WrappedPlayInTrSel.class)
    public static WrappedPlayInTrSel playInTrSel(Object rawPacket, PacketContainer<WrappedPlayInTrSel> packetContainer) {
        return new WrappedPlayInTrSel(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInUpdateSign", clazz = WrappedPlayInUpdateSign.class)
    public static WrappedPlayInUpdateSign playInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        return new WrappedPlayInUpdateSign(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInUseEntity", clazz = WrappedPlayInUseEntity.class)
    public static WrappedPlayInUseEntity playInUseEntity(Object rawPacket, PacketContainer<WrappedPlayInUseEntity> packetContainer) {
        return new WrappedPlayInUseEntity(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInUseItem", clazz = WrappedPlayInUseItem.class)
    public static WrappedPlayInUseItem playInUseItem(Object rawPacket, PacketContainer<WrappedPlayInUseItem> packetContainer) {
        return new WrappedPlayInUseItem(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInVehicleMove", clazz = WrappedPlayInVehicleMove.class)
    public static WrappedPlayInVehicleMove playInVehicleMove(Object rawPacket, PacketContainer<WrappedPlayInVehicleMove> packetContainer) {
        return new WrappedPlayInVehicleMove(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayInWindowClick", clazz = WrappedPlayInWindowClick.class)
    public static WrappedPlayInWindowClick playInWindowClick(Object rawPacket, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        return new WrappedPlayInWindowClick(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutAbilities", clazz = WrappedPlayOutAbilities.class)
    public static WrappedPlayOutAbilities playOutAbilities(Object rawPacket, PacketContainer<WrappedPlayOutAbilities> packetContainer) {
        return new WrappedPlayOutAbilities(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutAdvancements", clazz = WrappedPlayOutAdvancements.class)
    public static WrappedPlayOutAdvancements playOutAdvancements(Object rawPacket, PacketContainer<WrappedPlayOutAdvancements> packetContainer) {
        return new WrappedPlayOutAdvancements(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutAnimation", clazz = WrappedPlayOutAnimation.class)
    public static WrappedPlayOutAnimation playOutAnimation(Object rawPacket, PacketContainer<WrappedPlayOutAnimation> packetContainer) {
        return new WrappedPlayOutAnimation(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutAttachEntity", clazz = WrappedPlayOutAttachEntity.class)
    public static WrappedPlayOutAttachEntity playOutAttachEntity(Object rawPacket, PacketContainer<WrappedPlayOutAttachEntity> packetContainer) {
        return new WrappedPlayOutAttachEntity(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutAutoRecipe", clazz = WrappedPlayOutAutoRecipe.class)
    public static WrappedPlayOutAutoRecipe playOutAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayOutAutoRecipe> packetContainer) {
        return new WrappedPlayOutAutoRecipe(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutBed", clazz = WrappedPlayOutBed.class)
    public static WrappedPlayOutBed playOutBed(Object rawPacket, PacketContainer<WrappedPlayOutBed> packetContainer) {
        return new WrappedPlayOutBed(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutBlockAction", clazz = WrappedPlayOutBlockAction.class)
    public static WrappedPlayOutBlockAction playOutBlockAction(Object rawPacket, PacketContainer<WrappedPlayOutBlockAction> packetContainer) {
        return new WrappedPlayOutBlockAction(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutBlockBreak", clazz = WrappedPlayOutBlockBreak.class)
    public static WrappedPlayOutBlockBreak playOutBlockBreak(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreak> packetContainer) {
        return new WrappedPlayOutBlockBreak(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutBlockBreakAnimation", clazz = WrappedPlayOutBlockBreakAnimation.class)
    public static WrappedPlayOutBlockBreakAnimation playOutBlockBreakAnimation(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreakAnimation> packetContainer) {
        return new WrappedPlayOutBlockBreakAnimation(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutBlockChange", clazz = WrappedPlayOutBlockChange.class)
    public static WrappedPlayOutBlockChange playOutBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutBlockChange> packetContainer) {
        return new WrappedPlayOutBlockChange(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutBoss", clazz = WrappedPlayOutBoss.class)
    public static WrappedPlayOutBoss playOutBoss(Object rawPacket, PacketContainer<WrappedPlayOutBoss> packetContainer) {
        return new WrappedPlayOutBoss(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutCamera", clazz = WrappedPlayOutCamera.class)
    public static WrappedPlayOutCamera playOutCamera(Object rawPacket, PacketContainer<WrappedPlayOutCamera> packetContainer) {
        return new WrappedPlayOutCamera(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutChat", clazz = WrappedPlayOutChat.class)
    public static WrappedPlayOutChat playOutChat(Object rawPacket, PacketContainer<WrappedPlayOutChat> packetContainer) {
        return new WrappedPlayOutChat(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutCloseWindow", clazz = WrappedPlayOutCloseWindow.class)
    public static WrappedPlayOutCloseWindow playOutCloseWindow(Object rawPacket, PacketContainer<WrappedPlayOutCloseWindow> packetContainer) {
        return new WrappedPlayOutCloseWindow(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutCollect", clazz = WrappedPlayOutCollect.class)
    public static WrappedPlayOutCollect playOutCollect(Object rawPacket, PacketContainer<WrappedPlayOutCollect> packetContainer) {
        return new WrappedPlayOutCollect(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutCombatEvent", clazz = WrappedPlayOutCombatEvent.class)
    public static WrappedPlayOutCombatEvent playOutCombatEvent(Object rawPacket, PacketContainer<WrappedPlayOutCombatEvent> packetContainer) {
        return new WrappedPlayOutCombatEvent(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutCommands", clazz = WrappedPlayOutCommands.class)
    public static WrappedPlayOutCommands playOutCommands(Object rawPacket, PacketContainer<WrappedPlayOutCommands> packetContainer) {
        return new WrappedPlayOutCommands(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutCustomPayload", clazz = WrappedPlayOutCustomPayload.class)
    public static WrappedPlayOutCustomPayload playOutCustomPayload(Object rawPacket, PacketContainer<WrappedPlayOutCustomPayload> packetContainer) {
        return new WrappedPlayOutCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntity", clazz = WrappedPlayOutEntity.class)
    public static WrappedPlayOutEntity playOutEntity(Object rawPacket, PacketContainer<WrappedPlayOutEntity> packetContainer) {
        return new WrappedPlayOutEntity(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityDestroy", clazz = WrappedPlayOutEntityDestroy.class)
    public static WrappedPlayOutEntityDestroy playOutEntityDestroy(Object rawPacket, PacketContainer<WrappedPlayOutEntityDestroy> packetContainer) {
        return new WrappedPlayOutEntityDestroy(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityEffect", clazz = WrappedPlayOutEntityEffect.class)
    public static WrappedPlayOutEntityEffect playOutEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutEntityEffect> packetContainer) {
        return new WrappedPlayOutEntityEffect(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityEquipment", clazz = WrappedPlayOutEntityEquipment.class)
    public static WrappedPlayOutEntityEquipment playOutEntityEquipment(Object rawPacket, PacketContainer<WrappedPlayOutEntityEquipment> packetContainer) {
        return new WrappedPlayOutEntityEquipment(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityHeadRotation", clazz = WrappedPlayOutEntityHeadRotation.class)
    public static WrappedPlayOutEntityHeadRotation playOutEntityHeadRotation(Object rawPacket, PacketContainer<WrappedPlayOutEntityHeadRotation> packetContainer) {
        return new WrappedPlayOutEntityHeadRotation(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityLook", clazz = WrappedPlayOutEntityLook.class)
    public static WrappedPlayOutEntityLook playOutEntityLook(Object rawPacket, PacketContainer<WrappedPlayOutEntityLook> packetContainer) {
        return new WrappedPlayOutEntityLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntity$PacketPlayOutEntityLook", clazz = WrappedPlayOutEntityLook.class)
    public static WrappedPlayOutEntityLook playOutEntityLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutEntityLook> packetContainer) {
        return new WrappedPlayOutEntityLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityMetadata", clazz = WrappedPlayOutEntityMetadata.class)
    public static WrappedPlayOutEntityMetadata playOutEntityMetadata(Object rawPacket, PacketContainer<WrappedPlayOutEntityMetadata> packetContainer) {
        return new WrappedPlayOutEntityMetadata(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntitySound", clazz = WrappedPlayOutEntitySound.class)
    public static WrappedPlayOutEntitySound playOutEntitySound(Object rawPacket, PacketContainer<WrappedPlayOutEntitySound> packetContainer) {
        return new WrappedPlayOutEntitySound(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityStatus", clazz = WrappedPlayOutEntityStatus.class)
    public static WrappedPlayOutEntityStatus playOutEntityStatus(Object rawPacket, PacketContainer<WrappedPlayOutEntityStatus> packetContainer) {
        return new WrappedPlayOutEntityStatus(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityTeleport", clazz = WrappedPlayOutEntityTeleport.class)
    public static WrappedPlayOutEntityTeleport playOutEntityTeleport(Object rawPacket, PacketContainer<WrappedPlayOutEntityTeleport> packetContainer) {
        return new WrappedPlayOutEntityTeleport(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntityVelocity", clazz = WrappedPlayOutEntityVelocity.class)
    public static WrappedPlayOutEntityVelocity playOutEntityVelocity(Object rawPacket, PacketContainer<WrappedPlayOutEntityVelocity> packetContainer) {
        return new WrappedPlayOutEntityVelocity(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutExperience", clazz = WrappedPlayOutExperience.class)
    public static WrappedPlayOutExperience playOutExperience(Object rawPacket, PacketContainer<WrappedPlayOutExperience> packetContainer) {
        return new WrappedPlayOutExperience(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutExplosion", clazz = WrappedPlayOutExplosion.class)
    public static WrappedPlayOutExplosion playOutExplosion(Object rawPacket, PacketContainer<WrappedPlayOutExplosion> packetContainer) {
        return new WrappedPlayOutExplosion(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutGameStateChange", clazz = WrappedPlayOutGameStateChange.class)
    public static WrappedPlayOutGameStateChange playOutGameStateChange(Object rawPacket, PacketContainer<WrappedPlayOutGameStateChange> packetContainer) {
        return new WrappedPlayOutGameStateChange(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutHeldItemSlot", clazz = WrappedPlayOutHeldItemSlot.class)
    public static WrappedPlayOutHeldItemSlot playOutHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayOutHeldItemSlot> packetContainer) {
        return new WrappedPlayOutHeldItemSlot(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutKeepAlive", clazz = WrappedPlayOutKeepAlive.class)
    public static WrappedPlayOutKeepAlive playOutKeepAlive(Object rawPacket, PacketContainer<WrappedPlayOutKeepAlive> packetContainer) {
        return new WrappedPlayOutKeepAlive(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutKickDisconnect", clazz = WrappedPlayOutKickDisconnect.class)
    public static WrappedPlayOutKickDisconnect playOutKickDisconnect(Object rawPacket, PacketContainer<WrappedPlayOutKickDisconnect> packetContainer) {
        return new WrappedPlayOutKickDisconnect(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutLightUpdate", clazz = WrappedPlayOutLightUpdate.class)
    public static WrappedPlayOutLightUpdate playOutLightUpdate(Object rawPacket, PacketContainer<WrappedPlayOutLightUpdate> packetContainer) {
        return new WrappedPlayOutLightUpdate(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutLogin", clazz = WrappedPlayOutLogin.class)
    public static WrappedPlayOutLogin playOutLogin(Object rawPacket, PacketContainer<WrappedPlayOutLogin> packetContainer) {
        return new WrappedPlayOutLogin(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutLookAt", clazz = WrappedPlayOutLookAt.class)
    public static WrappedPlayOutLookAt playOutLookAt(Object rawPacket, PacketContainer<WrappedPlayOutLookAt> packetContainer) {
        return new WrappedPlayOutLookAt(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutMap", clazz = WrappedPlayOutMap.class)
    public static WrappedPlayOutMap playOutMap(Object rawPacket, PacketContainer<WrappedPlayOutMap> packetContainer) {
        return new WrappedPlayOutMap(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutMapChunk", clazz = WrappedPlayOutMapChunk.class)
    public static WrappedPlayOutMapChunk playOutMapChunk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunk> packetContainer) {
        return new WrappedPlayOutMapChunk(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutMapChunkBulk", clazz = WrappedPlayOutMapChunkBulk.class)
    public static WrappedPlayOutMapChunkBulk playOutMapChunkBulk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunkBulk> packetContainer) {
        return new WrappedPlayOutMapChunkBulk(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutMount", clazz = WrappedPlayOutMount.class)
    public static WrappedPlayOutMount playOutMount(Object rawPacket, PacketContainer<WrappedPlayOutMount> packetContainer) {
        return new WrappedPlayOutMount(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutMultiBlockChange", clazz = WrappedPlayOutMultiBlockChange.class)
    public static WrappedPlayOutMultiBlockChange playOutMultiBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutMultiBlockChange> packetContainer) {
        return new WrappedPlayOutMultiBlockChange(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutNamedEntitySpawn", clazz = WrappedPlayOutNamedEntitySpawn.class)
    public static WrappedPlayOutNamedEntitySpawn playOutNamedEntitySpawn(Object rawPacket, PacketContainer<WrappedPlayOutNamedEntitySpawn> packetContainer) {
        return new WrappedPlayOutNamedEntitySpawn(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutNamedSoundEffect", clazz = WrappedPlayOutNamedSoundEffect.class)
    public static WrappedPlayOutNamedSoundEffect playOutNamedSoundEffect(Object rawPacket, PacketContainer<WrappedPlayOutNamedSoundEffect> packetContainer) {
        return new WrappedPlayOutNamedSoundEffect(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutNBTQuery", clazz = WrappedPlayOutNBTQuery.class)
    public static WrappedPlayOutNBTQuery playOutNBTQuery(Object rawPacket, PacketContainer<WrappedPlayOutNBTQuery> packetContainer) {
        return new WrappedPlayOutNBTQuery(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutOpenBook", clazz = WrappedPlayOutOpenBook.class)
    public static WrappedPlayOutOpenBook playOutOpenBook(Object rawPacket, PacketContainer<WrappedPlayOutOpenBook> packetContainer) {
        return new WrappedPlayOutOpenBook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutOpenSignEditor", clazz = WrappedPlayOutOpenSignEditor.class)
    public static WrappedPlayOutOpenSignEditor playOutOpenSignEditor(Object rawPacket, PacketContainer<WrappedPlayOutOpenSignEditor> packetContainer) {
        return new WrappedPlayOutOpenSignEditor(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutOpenWindow", clazz = WrappedPlayOutOpenWindow.class)
    public static WrappedPlayOutOpenWindow playOutOpenWindow(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindow> packetContainer) {
        return new WrappedPlayOutOpenWindow(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutOpenWindowHorse", clazz = WrappedPlayOutOpenWindowHorse.class)
    public static WrappedPlayOutOpenWindowHorse playOutOpenWindowHorse(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowHorse> packetContainer) {
        return new WrappedPlayOutOpenWindowHorse(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutOpenWindowMerchant", clazz = WrappedPlayOutOpenWindowMerchant.class)
    public static WrappedPlayOutOpenWindowMerchant playOutOpenWindowMerchant(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowMerchant> packetContainer){
        return new WrappedPlayOutOpenWindowMerchant(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutPlayerInfo", clazz = WrappedPlayOutPlayerInfo.class)
    public static WrappedPlayOutPlayerInfo playOutPlayerInfo(Object rawPacket, PacketContainer<WrappedPlayOutPlayerInfo> packetContainer) {
        return new WrappedPlayOutPlayerInfo(rawPacket, packetContainer);
    }

    @PacketFactory(value = "ClientboundPlayerInfoUpdatePacket", clazz = WrappedPlayOutPlayerInfo.class)
    public static WrappedPlayOutPlayerInfo playOutPlayerInfo2(Object rawPacket, PacketContainer<WrappedPlayOutPlayerInfo> packetContainer) {
        return new WrappedPlayOutPlayerInfo(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutPlayerListHeaderFooter", clazz = WrappedPlayOutPlayerListHeaderFooter.class)
    public static WrappedPlayOutPlayerListHeaderFooter playOutPlayerListHeaderFooter(Object rawPacket, PacketContainer<WrappedPlayOutPlayerListHeaderFooter> packetContainer) {
        return new WrappedPlayOutPlayerListHeaderFooter(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutPosition", clazz = WrappedPlayOutPosition.class)
    public static WrappedPlayOutPosition playOutPosition(Object rawPacket, PacketContainer<WrappedPlayOutPosition> packetContainer) {
        return new WrappedPlayOutPosition(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutRecipes", clazz = WrappedPlayOutRecipes.class)
    public static WrappedPlayOutRecipes playOutRecipes(Object rawPacket, PacketContainer<WrappedPlayOutRecipes> packetContainer) {
        return new WrappedPlayOutRecipes(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutRecipeUpdate", clazz = WrappedPlayOutRecipeUpdate.class)
    public static WrappedPlayOutRecipeUpdate playOutRecipeUpdate(Object rawPacket, PacketContainer<WrappedPlayOutRecipeUpdate> packetContainer) {
        return new WrappedPlayOutRecipeUpdate(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutRelEntityMove", clazz = WrappedPlayOutRelEntityMove.class)
    public static WrappedPlayOutRelEntityMove playOutRelEntityMove(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMove> packetContainer) {
        return new WrappedPlayOutRelEntityMove(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntity$PacketPlayOutRelEntityMove", clazz = WrappedPlayOutRelEntityMove.class)
    public static WrappedPlayOutRelEntityMove playOutRelEntityMove_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMove> packetContainer) {
        return new WrappedPlayOutRelEntityMove(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutRelEntityMoveLook", clazz = WrappedPlayOutRelEntityMoveLook.class)
    public static WrappedPlayOutRelEntityMoveLook playOutRelEntityMoveLook(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMoveLook> packetContainer) {
        return new WrappedPlayOutRelEntityMoveLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook", clazz = WrappedPlayOutRelEntityMoveLook.class)
    public static WrappedPlayOutRelEntityMoveLook playOutRelEntityMoveLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMoveLook> packetContainer) {
        return new WrappedPlayOutRelEntityMoveLook(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutRemoveEntityEffect", clazz = WrappedPlayOutRemoveEntityEffect.class)
    public static WrappedPlayOutRemoveEntityEffect playOutRemoveEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutRemoveEntityEffect> packetContainer) {
        return new WrappedPlayOutRemoveEntityEffect(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutResourcePackSend", clazz = WrappedPlayOutResourcePackSend.class)
    public static WrappedPlayOutResourcePackSend playOutResourcePackSend(Object rawPacket, PacketContainer<WrappedPlayOutResourcePackSend> packetContainer) {
        return new WrappedPlayOutResourcePackSend(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutRespawn", clazz = WrappedPlayOutRespawn.class)
    public static WrappedPlayOutRespawn playOutRespawn(Object rawPacket, PacketContainer<WrappedPlayOutRespawn> packetContainer) {
        return new WrappedPlayOutRespawn(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutScoreboardDisplayObjective", clazz = WrappedPlayOutScoreboardDisplayObjective.class)
    public static WrappedPlayOutScoreboardDisplayObjective playOutScoreboardDisplayObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardDisplayObjective> packetContainer) {
        return new WrappedPlayOutScoreboardDisplayObjective(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutScoreboardObjective", clazz = WrappedPlayOutScoreboardObjective.class)
    public static WrappedPlayOutScoreboardObjective playOutScoreboardObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardObjective> packetContainer) {
        return new WrappedPlayOutScoreboardObjective(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutScoreboardScore", clazz = WrappedPlayOutScoreboardScore.class)
    public static WrappedPlayOutScoreboardScore playOutScoreboardScore(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardScore> packetContainer) {
        return new WrappedPlayOutScoreboardScore(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutScoreboardTeam", clazz = WrappedPlayOutScoreboardTeam.class)
    public static WrappedPlayOutScoreboardTeam playOutScoreboardTeam(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardTeam> packetContainer) {
        return new WrappedPlayOutScoreboardTeam(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSelectAdvancementTab", clazz = WrappedPlayOutSelectAdvancementTab.class)
    public static WrappedPlayOutSelectAdvancementTab playOutSelectAdvancementTab(Object rawPacket, PacketContainer<WrappedPlayOutSelectAdvancementTab> packetContainer) {
        return new WrappedPlayOutSelectAdvancementTab(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutServerDifficulty", clazz = WrappedPlayOutServerDifficulty.class)
    public static WrappedPlayOutServerDifficulty playOutServerDifficulty(Object rawPacket, PacketContainer<WrappedPlayOutServerDifficulty> packetContainer) {
        return new WrappedPlayOutServerDifficulty(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSetCompression", clazz = WrappedPlayOutSetCompression.class)
    public static WrappedPlayOutSetCompression playOutSetCompression(Object rawPacket, PacketContainer<WrappedPlayOutSetCompression> packetContainer) {
        return new WrappedPlayOutSetCompression(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSetCooldown", clazz = WrappedPlayOutSetCooldown.class)
    public static WrappedPlayOutSetCooldown playOutSetCooldown(Object rawPacket, PacketContainer<WrappedPlayOutSetCooldown> packetContainer) {
        return new WrappedPlayOutSetCooldown(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSetSlot", clazz = WrappedPlayOutSetSlot.class)
    public static WrappedPlayOutSetSlot playOutSetSlot(Object rawPacket, PacketContainer<WrappedPlayOutSetSlot> packetContainer) {
        return new WrappedPlayOutSetSlot(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSpawnEntity", clazz = WrappedPlayOutSpawnEntity.class)
    public static WrappedPlayOutSpawnEntity playOutSpawnEntity(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntity> packetContainer) {
        return new WrappedPlayOutSpawnEntity(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSpawnEntityExperienceOrb", clazz = WrappedPlayOutSpawnEntityExperienceOrb.class)
    public static WrappedPlayOutSpawnEntityExperienceOrb playOutSpawnEntityExperienceOrb(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityExperienceOrb> packetContainer) {
        return new WrappedPlayOutSpawnEntityExperienceOrb(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSpawnEntityLiving", clazz = WrappedPlayOutSpawnEntityLiving.class)
    public static WrappedPlayOutSpawnEntityLiving playOutSpawnEntityLiving(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityLiving> packetContainer) {
        return new WrappedPlayOutSpawnEntityLiving(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSpawnEntityPainting", clazz = WrappedPlayOutSpawnEntityPainting.class)
    public static WrappedPlayOutSpawnEntityPainting playOutSpawnEntityPainting(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityPainting> packetContainer) {
        return new WrappedPlayOutSpawnEntityPainting(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSpawnEntityWeather", clazz = WrappedPlayOutSpawnEntityWeather.class)
    public static WrappedPlayOutSpawnEntityWeather playOutSpawnEntityWeather(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityWeather> packetContainer) {
        return new WrappedPlayOutSpawnEntityWeather(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutSpawnPosition", clazz = WrappedPlayOutSpawnPosition.class)
    public static WrappedPlayOutSpawnPosition playOutSpawnPosition(Object rawPacket, PacketContainer<WrappedPlayOutSpawnPosition> packetContainer) {
        return new WrappedPlayOutSpawnPosition(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutStatistic", clazz = WrappedPlayOutStatistic.class)
    public static WrappedPlayOutStatistic playOutStatistic(Object rawPacket, PacketContainer<WrappedPlayOutStatistic> packetContainer) {
        return new WrappedPlayOutStatistic(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutStopSound", clazz = WrappedPlayOutStopSound.class)
    public static WrappedPlayOutStopSound playOutStopSound(Object rawPacket, PacketContainer<WrappedPlayOutStopSound> packetContainer) {
        return new WrappedPlayOutStopSound(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutTabComplete", clazz = WrappedPlayOutTabComplete.class)
    public static WrappedPlayOutTabComplete playOutTabComplete(Object rawPacket, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        return new WrappedPlayOutTabComplete(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutTags", clazz = WrappedPlayOutTags.class)
    public static WrappedPlayOutTags playOutTags(Object rawPacket, PacketContainer<WrappedPlayOutTags> packetContainer) {
        return new WrappedPlayOutTags(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutTileEntityData", clazz = WrappedPlayOutTileEntityData.class)
    public static WrappedPlayOutTileEntityData playOutTileEntityData(Object rawPacket, PacketContainer<WrappedPlayOutTileEntityData> packetContainer) {
        return new WrappedPlayOutTileEntityData(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutTitle", clazz = WrappedPlayOutTitle.class)
    public static WrappedPlayOutTitle playOutTitle(Object rawPacket, PacketContainer<WrappedPlayOutTitle> packetContainer) {
        return new WrappedPlayOutTitle(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutTransaction", clazz = WrappedPlayOutTransaction.class)
    public static WrappedPlayOutTransaction playOutTransaction(Object rawPacket, PacketContainer<WrappedPlayOutTransaction> packetContainer) {
        return new WrappedPlayOutTransaction(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutUnloadChunk", clazz = WrappedPlayOutUnloadChunk.class)
    public static WrappedPlayOutUnloadChunk playOutUnloadChunk(Object rawPacket, PacketContainer<WrappedPlayOutUnloadChunk> packetContainer) {
        return new WrappedPlayOutUnloadChunk(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutUpdateAttributes", clazz = WrappedPlayOutUpdateAttributes.class)
    public static WrappedPlayOutUpdateAttributes playOutUpdateAttributes(Object rawPacket, PacketContainer<WrappedPlayOutUpdateAttributes> packetContainer) {
        return new WrappedPlayOutUpdateAttributes(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutUpdateEntityNBT", clazz = WrappedPlayOutUpdateEntityNBT.class)
    public static WrappedPlayOutUpdateEntityNBT playOutUpdateEntityNBT(Object rawPacket, PacketContainer<WrappedPlayOutUpdateEntityNBT> packetContainer) {
        return new WrappedPlayOutUpdateEntityNBT(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutUpdateHealth", clazz = WrappedPlayOutUpdateHealth.class)
    public static WrappedPlayOutUpdateHealth playOutUpdateHealth(Object rawPacket, PacketContainer<WrappedPlayOutUpdateHealth> packetContainer) {
        return new WrappedPlayOutUpdateHealth(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutUpdateSign", clazz = WrappedPlayOutUpdateSign.class)
    public static WrappedPlayOutUpdateSign playOutUpdateSign(Object rawPacket, PacketContainer<WrappedPlayOutUpdateSign> packetContainer) {
        return new WrappedPlayOutUpdateSign(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutUpdateTime", clazz = WrappedPlayOutUpdateTime.class)
    public static WrappedPlayOutUpdateTime playOutUpdateTime(Object rawPacket, PacketContainer<WrappedPlayOutUpdateTime> packetContainer) {
        return new WrappedPlayOutUpdateTime(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutVehicleMove", clazz = WrappedPlayOutVehicleMove.class)
    public static WrappedPlayOutVehicleMove playOutVehicleMove(Object rawPacket, PacketContainer<WrappedPlayOutVehicleMove> packetContainer) {
        return new WrappedPlayOutVehicleMove(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutViewCentre", clazz = WrappedPlayOutViewCentre.class)
    public static WrappedPlayOutViewCentre playOutViewCentre(Object rawPacket, PacketContainer<WrappedPlayOutViewCentre> packetContainer) {
        return new WrappedPlayOutViewCentre(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutViewDistance", clazz = WrappedPlayOutViewDistance.class)
    public static  WrappedPlayOutViewDistance playOutViewDistance(Object rawPacket, PacketContainer<WrappedPlayOutViewDistance> packetContainer) {
        return new WrappedPlayOutViewDistance(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutWindowData", clazz = WrappedPlayOutWindowData.class)
    public static WrappedPlayOutWindowData playOutWindowData(Object rawPacket, PacketContainer<WrappedPlayOutWindowData> packetContainer) {
        return new WrappedPlayOutWindowData(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutWindowItems", clazz = WrappedPlayOutWindowItems.class)
    public static WrappedPlayOutWindowItems playOutWindowItems(Object rawPacket, PacketContainer<WrappedPlayOutWindowItems> packetContainer) {
        return new WrappedPlayOutWindowItems(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutWorldBorder", clazz = WrappedPlayOutWorldBorder.class)
    public static WrappedPlayOutWorldBorder playOutWorldBorder(Object rawPacket, PacketContainer<WrappedPlayOutWorldBorder> packetContainer) {
        return new WrappedPlayOutWorldBorder(rawPacket, packetContainer);
    }

    @PacketFactory(value = "ClientboundInitializeBorderPacket", clazz = WrappedPlayOutWorldBorder.class)
    public static WrappedPlayOutWorldBorder playOutWorldBorder_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutWorldBorder> packetContainer) {
        return new WrappedPlayOutWorldBorder(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutWorldEvent", clazz = WrappedPlayOutWorldEvent.class)
    public static WrappedPlayOutWorldEvent playOutWorldEvent(Object rawPacket, PacketContainer<WrappedPlayOutWorldEvent> packetContainer) {
        return new WrappedPlayOutWorldEvent(rawPacket, packetContainer);
    }

    @PacketFactory(value = "PacketPlayOutWorldParticles", clazz = WrappedPlayOutWorldParticles.class)
    public static WrappedPlayOutWorldParticles playOutWorldParticles(Object rawPacket, PacketContainer<WrappedPlayOutWorldParticles> packetContainer) {
        return new WrappedPlayOutWorldParticles(rawPacket, packetContainer);
    }
}
