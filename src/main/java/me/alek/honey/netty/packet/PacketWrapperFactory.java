package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.netty.packetwrappers.login.server.*;
import me.alek.honey.netty.packetwrappers.play.client.*;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.utility.AsyncFuture;
import me.alek.honey.netty.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInCustomPayload;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.packetwrappers.play.server.*;
import me.alek.honey.netty.packetwrappers.status.client.WrappedStatusInPing;
import me.alek.honey.netty.packetwrappers.status.client.WrappedStatusInStart;
import me.alek.honey.netty.packetwrappers.status.server.WrappedStatusOutPong;
import me.alek.honey.netty.packetwrappers.status.server.WrappedStatusOutServerInfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class PacketWrapperFactory {

    private static class BufferedMapCreator<K, V> {

        private Map<K, V> streamConcurrentMap(Iterable<K> iterable, Function<K, V> function) {
            return streamMap(new ConcurrentHashMap<>(), iterable, function);
        }

        private Map<K, V> streamHashMap(Iterable<K> iterable, Function<K, V> function) {
            return streamMap(new HashMap<>(), iterable, function);
        }

        private Map<K, V> streamMap(Map<K, V> map, Iterable<K> iterable, Function<K, V> function) {
            for (K key : iterable) {
                map.put(key, function.apply(key));
            }
            return map;
        }
    }

    @SuppressWarnings("unchecked")
    public static Consumer<AsyncFuture> load() {
        return (future) -> {
            final BufferedMapCreator<PacketTypeEnum, MethodInvoker<Object>> mapCreator = new BufferedMapCreator<>();

            for (Method method : PacketWrapperFactory.class.getDeclaredMethods()) {
                PacketFactory wrappedPacket = method.getAnnotation(PacketFactory.class);
                if (wrappedPacket == null) {
                    continue;
                }
                final MethodInvoker<Object> methodInvoker = (MethodInvoker<Object>) Reflection.getMethod(method);
                METHODS_CACHE.put(wrappedPacket.value(), methodInvoker);
            }
            PLAY_CACHE = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PacketState.PLAY, PROTOCOL_VERSION), (packetType) -> {
                return getCachedInvoker(packetType.getNmsClass().getSimpleName());
            });
            HANDSHAKE_CACHE = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PacketState.HANDSHAKE, PROTOCOL_VERSION), (packetType) -> {
                return getCachedInvoker(packetType.getNmsClass().getSimpleName());
            });
            LOGIN_CACHE = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PacketState.LOGIN, PROTOCOL_VERSION), (packetType) -> {
                return getCachedInvoker(packetType.getNmsClass().getSimpleName());
            });
            STATUS_CACHE = mapCreator.streamConcurrentMap(PacketType.getTypesAvailableFor(PacketState.STATUS, PROTOCOL_VERSION), (packetType) -> {
                return getCachedInvoker(packetType.getNmsClass().getSimpleName());
            });
            future.done();
        };
    }

    private static final Protocol PROTOCOL_VERSION = Protocol.getProtocol();
    private static final Map<String, MethodInvoker<Object>> METHODS_CACHE = new HashMap<>();
    private static Map<PacketTypeEnum, MethodInvoker<Object>> PLAY_CACHE;
    private static Map<PacketTypeEnum, MethodInvoker<Object>> HANDSHAKE_CACHE;
    private static Map<PacketTypeEnum, MethodInvoker<Object>> LOGIN_CACHE;
    private static Map<PacketTypeEnum, MethodInvoker<Object>> STATUS_CACHE;

    private static MethodInvoker<Object> getCachedInvoker(String name) {
        if (METHODS_CACHE.get(name) != null) {
            return METHODS_CACHE.get(name);
        }
        throw new RuntimeException("Ingen packet wrapper for " + name);
    }

    public static MethodInvoker<Object> createWrapperInvoker(PacketState state, Class<?> nmsClass) {
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
    }

    public static MethodInvoker<Object> createPlayWrapperInvoker(Class<?> nmsClass) {
        return PLAY_CACHE.get(PacketType.getPacketType(nmsClass));
    }

    public static MethodInvoker<Object> createHandshakeWrapper(Class<?> nmsClass) {
        return HANDSHAKE_CACHE.get(PacketType.getPacketType(nmsClass));
    }

    public static MethodInvoker<Object> createLoginWrapper(Class<?> nmsClass) {
        return LOGIN_CACHE.get(PacketType.getPacketType(nmsClass));
    }

    public static MethodInvoker<Object> createStatusWrapper(Class<?> nmsClass) {
        return STATUS_CACHE.get(PacketType.getPacketType(nmsClass));
    }

    @PacketFactory("PacketHandshakingInSetProtocol")
    public static WrappedHandshakeInSetProtocol handshakeInSetProtocol(Object rawPacket, PacketContainer<WrappedHandshakeInSetProtocol> packetContainer) {
        return new WrappedHandshakeInSetProtocol(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginInCustomPayload")
    public static WrappedLoginInCustomPayload loginInCustomPayload(Object rawPacket, PacketContainer<WrappedLoginInCustomPayload> packetContainer) {
        return new WrappedLoginInCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginInEncryptionBegin")
    public static WrappedLoginInEncryptionResponse loginInEncryptionResponse(Object rawPacket, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {
        return new WrappedLoginInEncryptionResponse(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginInStart")
    public static WrappedLoginInLoginStart loginInLoginStart(Object rawPacket, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
        return new WrappedLoginInLoginStart(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginOutCustomPayload")
    public static WrappedLoginOutCustomPayload loginOutCustomPayload(Object rawPacket, PacketContainer<WrappedLoginOutCustomPayload> packetContainer) {
        return new WrappedLoginOutCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginOutDisconnect")
    public static WrappedLoginOutDisconnect loginOutDisconnect(Object rawPacket, PacketContainer<WrappedLoginOutDisconnect> packetContainer) {
        return new WrappedLoginOutDisconnect(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginOutEncryptionBegin")
    public static WrappedLoginOutEncryptionRequest loginOutEncryptionRequest(Object rawPacket, PacketContainer<WrappedLoginOutEncryptionRequest> packetContainer) {
        return new WrappedLoginOutEncryptionRequest(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginOutSetCompression")
    public static WrappedLoginOutSetCompression loginOutSetCompression(Object rawPacket, PacketContainer<WrappedLoginOutSetCompression> packetContainer) {
        return new WrappedLoginOutSetCompression(rawPacket, packetContainer);
    }

    @PacketFactory("PacketLoginOutSuccess")
    public static WrappedLoginOutSuccess loginOutSuccess(Object rawPacket, PacketContainer<WrappedLoginOutSuccess> packetContainer) {
        return new WrappedLoginOutSuccess(rawPacket, packetContainer);
    }

    @PacketFactory("PacketStatusInPing")
    public static WrappedStatusInPing statusInPing(Object rawPacket, PacketContainer<WrappedStatusInPing> packetContainer) {
        return new WrappedStatusInPing(rawPacket, packetContainer);
    }

    @PacketFactory("PacketStatusInStart")
    public static WrappedStatusInStart statusInStart(Object rawPacket, PacketContainer<WrappedStatusInStart> packetContainer) {
        return new WrappedStatusInStart(rawPacket, packetContainer);
    }

    @PacketFactory("PacketStatusOutPong")
    public static WrappedStatusOutPong statusOutPong(Object rawPacket, PacketContainer<WrappedStatusOutPong> packetContainer) {
        return new WrappedStatusOutPong(rawPacket, packetContainer);
    }

    @PacketFactory("PacketStatusOutServerInfo")
    public static WrappedStatusOutServerInfo statusOutServerInfo(Object rawPacket, PacketContainer<WrappedStatusOutServerInfo> packetContainer) {
        return new WrappedStatusOutServerInfo(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInAbilities")
    public static WrappedPlayInAbilities playInAbilities(Object rawPacket, PacketContainer<WrappedPlayInAbilities> packetContainer) {
        return new WrappedPlayInAbilities(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInAdvancements")
    public static WrappedPlayInAdvancements playInAdvancements(Object rawPacket, PacketContainer<WrappedPlayInAdvancements> packetContainer) {
        return new WrappedPlayInAdvancements(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInArmAnimation")
    public static WrappedPlayInArmAnimation playInArmAnimation(Object rawPacket, PacketContainer<WrappedPlayInArmAnimation> packetContainer) {
        return new WrappedPlayInArmAnimation(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInAutoRecipe")
    public static WrappedPlayInAutoRecipe playInAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayInAutoRecipe> packetContainer) {
        return new WrappedPlayInAutoRecipe(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInBeacon")
    public static WrappedPlayInBeacon playInBeacon(Object rawPacket, PacketContainer<WrappedPlayInBeacon> packetContainer) {
        return new WrappedPlayInBeacon(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInBEdit")
    public static WrappedPlayInBEdit playInBEdit(Object rawPacket, PacketContainer<WrappedPlayInBEdit> packetContainer) {
        return new WrappedPlayInBEdit(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInBlockDig")
    public static WrappedPlayInBlockDig playInBlockDig(Object rawPacket, PacketContainer<WrappedPlayInBlockDig> packetContainer) {
        return new WrappedPlayInBlockDig(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInBlockPlace")
    public static WrappedPlayInBlockPlace playInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        return new WrappedPlayInBlockPlace(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInBoatMove")
    public static WrappedPlayInBoatMove playInBoatMove(Object rawPacket, PacketContainer<WrappedPlayInBoatMove> packetContainer) {
        return new WrappedPlayInBoatMove(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInChat")
    public static WrappedPlayInChat playInChat(Object rawPacket, PacketContainer<WrappedPlayInChat> packetContainer) {
        return new WrappedPlayInChat(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInClientCommand")
    public static WrappedPlayInClientCommand playInClientCommand(Object rawPacket, PacketContainer<WrappedPlayInClientCommand> packetContainer) {
        return new WrappedPlayInClientCommand(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInCloseWindow")
    public static WrappedPlayInCloseWindow playInCloseWindow(Object rawPacket, PacketContainer<WrappedPlayInCloseWindow> packetContainer) {
        return new WrappedPlayInCloseWindow(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInCustomPayload")
    public static WrappedPlayInCustomPayload playInCustomPayload(Object rawPacket, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        return new WrappedPlayInCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInDifficultyChange")
    public static WrappedPlayInDifficultyChange playInDifficultyChange(Object rawPacket, PacketContainer<WrappedPlayInDifficultyChange> packetContainer) {
        return new WrappedPlayInDifficultyChange(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInDifficultyLock")
    public static WrappedPlayInDifficultyLock playInDifficultyLock(Object rawPacket, PacketContainer<WrappedPlayInDifficultyLock> packetContainer) {
        return new WrappedPlayInDifficultyLock(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInEnchantItem")
    public static WrappedPlayInEnchantItem playInEnchantItem(Object rawPacket, PacketContainer<WrappedPlayInEnchantItem> packetContainer) {
        return new WrappedPlayInEnchantItem(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInEntityAction")
    public static WrappedPlayInEntityAction playInEntityAction(Object rawPacket, PacketContainer<WrappedPlayInEntityAction> packetContainer) {
        return new WrappedPlayInEntityAction(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInEntityNBTQuery")
    public static WrappedPlayInEntityNBTQuery playInEntityNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInEntityNBTQuery> packetContainer) {
        return new WrappedPlayInEntityNBTQuery(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInFlying")
    public static <WP extends WrappedPlayInFlying<WP>> WrappedPlayInFlying<WP> playInFlying(Object rawPacket, PacketContainer<WP> packetContainer) {
        return new WrappedPlayInFlying<>(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInFlying$d")
    public static WrappedPlayInGround playInGround(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        return new WrappedPlayInGround(rawPacket, packetContainer);
    }

    @PacketFactory("d")
    public static WrappedPlayInGround playInGround_v1_17(Object rawPacket, PacketContainer<WrappedPlayInGround> packetContainer) {
        return new WrappedPlayInGround(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInHeldItemSlot")
    public static WrappedPlayInHeldItemSlot playInHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayInHeldItemSlot> packetContainer) {
        return new WrappedPlayInHeldItemSlot(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInItemName")
    public static WrappedPlayInItemName playInItemName(Object rawPacket, PacketContainer<WrappedPlayInItemName> packetContainer) {
        return new WrappedPlayInItemName(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInJigsawGenerate")
    public static WrappedPlayInJigsawGenerate playInJigsawGenerate(Object rawPacket, PacketContainer<WrappedPlayInJigsawGenerate> packetContainer) {
        return new WrappedPlayInJigsawGenerate(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInKeepAlive")
    public static WrappedPlayInKeepAlive playInKeepAlive(Object rawPacket, PacketContainer<WrappedPlayInKeepAlive> packetContainer) {
        return new WrappedPlayInKeepAlive(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInFlying$PacketPlayInLook")
    public static WrappedPlayInLook playInLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        return new WrappedPlayInLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInLook")
    public static WrappedPlayInLook playInLook(Object rawPacket, PacketContainer<WrappedPlayInLook> packetContainer) {
        return new WrappedPlayInLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInPickItem")
    public static WrappedPlayInPickItem playInPickItem(Object rawPacket, PacketContainer<WrappedPlayInPickItem> packetContainer) {
        return new WrappedPlayInPickItem(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInFlying$PacketPlayInPosition")
    public static WrappedPlayInPosition playInPosition_v1_17(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        return new WrappedPlayInPosition(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInPosition")
    public static WrappedPlayInPosition playInPosition(Object rawPacket, PacketContainer<WrappedPlayInPosition> packetContainer) {
        return new WrappedPlayInPosition(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInFlying$PacketPlayInPositionLook")
    public static WrappedPlayInPositionLook playInPositionLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        return new WrappedPlayInPositionLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInPositionLook")
    public static WrappedPlayInPositionLook playInPositionLook(Object rawPacket, PacketContainer<WrappedPlayInPositionLook> packetContainer) {
        return new WrappedPlayInPositionLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInRecipeDisplayed")
    public static WrappedPlayInRecipeDisplayed playInRecipeDisplayed(Object rawPacket, PacketContainer<WrappedPlayInRecipeDisplayed> packetContainer) {
        return new WrappedPlayInRecipeDisplayed(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInRecipeSettings")
    public static WrappedPlayInRecipeSettings playInRecipeSettings(Object rawPacket, PacketContainer<WrappedPlayInRecipeSettings> packetContainer) {
        return new WrappedPlayInRecipeSettings(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInResourcePackStatus")
    public static WrappedPlayInResourcePackStatus playInResourcePackStatus(Object rawPacket, PacketContainer<WrappedPlayInResourcePackStatus> packetContainer) {
        return new WrappedPlayInResourcePackStatus(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSetCommandBlock")
    public static WrappedPlayInSetCommandBlock playInSetCommandBlock(Object rawPacket, PacketContainer<WrappedPlayInSetCommandBlock> packetContainer) {
        return new WrappedPlayInSetCommandBlock(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSetCommandMinecart")
    public static WrappedPlayInSetCommandMinecart playInSetCommandMinecart(Object rawPacket, PacketContainer<WrappedPlayInSetCommandMinecart> packetContainer) {
        return new WrappedPlayInSetCommandMinecart(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSetCreativeSlot")
    public static WrappedPlayInSetCreativeSlot playInSetCreativeSlot(Object rawPacket, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {
        return new WrappedPlayInSetCreativeSlot(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSetJigsaw")
    public static WrappedPlayInSetJigsaw playInSetJigsaw(Object rawPacket, PacketContainer<WrappedPlayInSetJigsaw> packetContainer) {
        return new WrappedPlayInSetJigsaw(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSettings")
    public static WrappedPlayInSettings playInSettings(Object rawPacket, PacketContainer<WrappedPlayInSettings> packetContainer) {
        return new WrappedPlayInSettings(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSpectate")
    public static WrappedPlayInSpectate playInSpectate(Object rawPacket, PacketContainer<WrappedPlayInSpectate> packetContainer) {
        return new WrappedPlayInSpectate(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInSteerVehicle")
    public static WrappedPlayInSteerVehicle playInSteerVehicle(Object rawPacket, PacketContainer<WrappedPlayInSteerVehicle> packetContainer) {
        return new WrappedPlayInSteerVehicle(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInStruct")
    public static WrappedPlayInStruct playInStruct(Object rawPacket, PacketContainer<WrappedPlayInStruct> packetContainer) {
        return new WrappedPlayInStruct(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInTabComplete")
    public static WrappedPlayInTabComplete playInTabComplete(Object rawPacket, PacketContainer<WrappedPlayInTabComplete> packetContainer) {
        return new WrappedPlayInTabComplete(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInTeleportAccept")
    public static WrappedPlayInTeleportAccept playInTeleportAccept(Object rawPacket, PacketContainer<WrappedPlayInTeleportAccept> packetContainer) {
        return new WrappedPlayInTeleportAccept(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInTileNBTQuery")
    public static WrappedPlayInTileNBTQuery playInTileNBTQuery(Object rawPacket, PacketContainer<WrappedPlayInTileNBTQuery> packetContainer) {
        return new WrappedPlayInTileNBTQuery(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInTransaction")
    public static WrappedPlayInTransaction playInTransaction(Object rawPacket, PacketContainer<WrappedPlayInTransaction> packetContainer) {
        return new WrappedPlayInTransaction(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInTrSel")
    public static WrappedPlayInTrSel playInTrSel(Object rawPacket, PacketContainer<WrappedPlayInTrSel> packetContainer) {
        return new WrappedPlayInTrSel(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInUpdateSign")
    public static WrappedPlayInUpdateSign playInUpdateSign(Object rawPacket, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        return new WrappedPlayInUpdateSign(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInUseEntity")
    public static WrappedPlayInUseEntity playInUseEntity(Object rawPacket, PacketContainer<WrappedPlayInUseEntity> packetContainer) {
        return new WrappedPlayInUseEntity(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInUseItem")
    public static WrappedPlayInUseItem playInUseItem(Object rawPacket, PacketContainer<WrappedPlayInUseItem> packetContainer) {
        return new WrappedPlayInUseItem(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInVehicleMove")
    public static WrappedPlayInVehicleMove playInVehicleMove(Object rawPacket, PacketContainer<WrappedPlayInVehicleMove> packetContainer) {
        return new WrappedPlayInVehicleMove(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayInWindowClick")
    public static WrappedPlayInWindowClick playInWindowClick(Object rawPacket, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        return new WrappedPlayInWindowClick(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutAbilities")
    public static WrappedPlayOutAbilities playOutAbilities(Object rawPacket, PacketContainer<WrappedPlayOutAbilities> packetContainer) {
        return new WrappedPlayOutAbilities(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutAdvancements")
    public static WrappedPlayOutAdvancements playOutAdvancements(Object rawPacket, PacketContainer<WrappedPlayOutAdvancements> packetContainer) {
        return new WrappedPlayOutAdvancements(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutAnimation")
    public static WrappedPlayOutAnimation playOutAnimation(Object rawPacket, PacketContainer<WrappedPlayOutAnimation> packetContainer) {
        return new WrappedPlayOutAnimation(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutAttachEntity")
    public static WrappedPlayOutAttachEntity playOutAttachEntity(Object rawPacket, PacketContainer<WrappedPlayOutAttachEntity> packetContainer) {
        return new WrappedPlayOutAttachEntity(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutAutoRecipe")
    public static WrappedPlayOutAutoRecipe playOutAutoRecipe(Object rawPacket, PacketContainer<WrappedPlayOutAutoRecipe> packetContainer) {
        return new WrappedPlayOutAutoRecipe(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutBed")
    public static WrappedPlayOutBed playOutBed(Object rawPacket, PacketContainer<WrappedPlayOutBed> packetContainer) {
        return new WrappedPlayOutBed(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutBlockAction")
    public static WrappedPlayOutBlockAction playOutBlockAction(Object rawPacket, PacketContainer<WrappedPlayOutBlockAction> packetContainer) {
        return new WrappedPlayOutBlockAction(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutBlockBreak")
    public static WrappedPlayOutBlockBreak playOutBlockBreak(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreak> packetContainer) {
        return new WrappedPlayOutBlockBreak(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutBlockBreakAnimation")
    public static WrappedPlayOutBlockBreakAnimation playOutBlockBreakAnimation(Object rawPacket, PacketContainer<WrappedPlayOutBlockBreakAnimation> packetContainer) {
        return new WrappedPlayOutBlockBreakAnimation(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutBlockChange")
    public static WrappedPlayOutBlockChange playOutBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutBlockChange> packetContainer) {
        return new WrappedPlayOutBlockChange(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutBoss")
    public static WrappedPlayOutBoss playOutBoss(Object rawPacket, PacketContainer<WrappedPlayOutBoss> packetContainer) {
        return new WrappedPlayOutBoss(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutCamera")
    public static WrappedPlayOutCamera playOutCamera(Object rawPacket, PacketContainer<WrappedPlayOutCamera> packetContainer) {
        return new WrappedPlayOutCamera(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutChat")
    public static WrappedPlayOutChat playOutChat(Object rawPacket, PacketContainer<WrappedPlayOutChat> packetContainer) {
        return new WrappedPlayOutChat(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutCloseWindow")
    public static WrappedPlayOutCloseWindow playOutCloseWindow(Object rawPacket, PacketContainer<WrappedPlayOutCloseWindow> packetContainer) {
        return new WrappedPlayOutCloseWindow(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutCollect")
    public static WrappedPlayOutCollect playOutCollect(Object rawPacket, PacketContainer<WrappedPlayOutCollect> packetContainer) {
        return new WrappedPlayOutCollect(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutCombatEvent")
    public static WrappedPlayOutCombatEvent playOutCombatEvent(Object rawPacket, PacketContainer<WrappedPlayOutCombatEvent> packetContainer) {
        return new WrappedPlayOutCombatEvent(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutCommands")
    public static WrappedPlayOutCommands playOutCommands(Object rawPacket, PacketContainer<WrappedPlayOutCommands> packetContainer) {
        return new WrappedPlayOutCommands(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutCustomPayload")
    public static WrappedPlayOutCustomPayload playOutCustomPayload(Object rawPacket, PacketContainer<WrappedPlayOutCustomPayload> packetContainer) {
        return new WrappedPlayOutCustomPayload(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntity")
    public static WrappedPlayOutEntity playOutEntity(Object rawPacket, PacketContainer<WrappedPlayOutEntity> packetContainer) {
        return new WrappedPlayOutEntity(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityDestroy")
    public static WrappedPlayOutEntityDestroy playOutEntityDestroy(Object rawPacket, PacketContainer<WrappedPlayOutEntityDestroy> packetContainer) {
        return new WrappedPlayOutEntityDestroy(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityEffect")
    public static WrappedPlayOutEntityEffect playOutEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutEntityEffect> packetContainer) {
        return new WrappedPlayOutEntityEffect(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityEquipment")
    public static WrappedPlayOutEntityEquipment playOutEntityEquipment(Object rawPacket, PacketContainer<WrappedPlayOutEntityEquipment> packetContainer) {
        return new WrappedPlayOutEntityEquipment(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityHeadRotation")
    public static WrappedPlayOutEntityHeadRotation playOutEntityHeadRotation(Object rawPacket, PacketContainer<WrappedPlayOutEntityHeadRotation> packetContainer) {
        return new WrappedPlayOutEntityHeadRotation(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityLook")
    public static WrappedPlayOutEntityLook playOutEntityLook(Object rawPacket, PacketContainer<WrappedPlayOutEntityLook> packetContainer) {
        return new WrappedPlayOutEntityLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntity$PacketPlayOutEntityLook")
    public static WrappedPlayOutEntityLook playOutEntityLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutEntityLook> packetContainer) {
        return new WrappedPlayOutEntityLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityMetadata")
    public static WrappedPlayOutEntityMetadata playOutEntityMetadata(Object rawPacket, PacketContainer<WrappedPlayOutEntityMetadata> packetContainer) {
        return new WrappedPlayOutEntityMetadata(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntitySound")
    public static WrappedPlayOutEntitySound playOutEntitySound(Object rawPacket, PacketContainer<WrappedPlayOutEntitySound> packetContainer) {
        return new WrappedPlayOutEntitySound(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityStatus")
    public static WrappedPlayOutEntityStatus playOutEntityStatus(Object rawPacket, PacketContainer<WrappedPlayOutEntityStatus> packetContainer) {
        return new WrappedPlayOutEntityStatus(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityTeleport")
    public static WrappedPlayOutEntityTeleport playOutEntityTeleport(Object rawPacket, PacketContainer<WrappedPlayOutEntityTeleport> packetContainer) {
        return new WrappedPlayOutEntityTeleport(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntityVelocity")
    public static WrappedPlayOutEntityVelocity playOutEntityVelocity(Object rawPacket, PacketContainer<WrappedPlayOutEntityVelocity> packetContainer) {
        return new WrappedPlayOutEntityVelocity(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutExperience")
    public static WrappedPlayOutExperience playOutExperience(Object rawPacket, PacketContainer<WrappedPlayOutExperience> packetContainer) {
        return new WrappedPlayOutExperience(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutExplosion")
    public static WrappedPlayOutExplosion playOutExplosion(Object rawPacket, PacketContainer<WrappedPlayOutExplosion> packetContainer) {
        return new WrappedPlayOutExplosion(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutGameStateChange")
    public static WrappedPlayOutGameStateChange playOutGameStateChange(Object rawPacket, PacketContainer<WrappedPlayOutGameStateChange> packetContainer) {
        return new WrappedPlayOutGameStateChange(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutHeldItemSlot")
    public static WrappedPlayOutHeldItemSlot playOutHeldItemSlot(Object rawPacket, PacketContainer<WrappedPlayOutHeldItemSlot> packetContainer) {
        return new WrappedPlayOutHeldItemSlot(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutKeepAlive")
    public static WrappedPlayOutKeepAlive playOutKeepAlive(Object rawPacket, PacketContainer<WrappedPlayOutKeepAlive> packetContainer) {
        return new WrappedPlayOutKeepAlive(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutKickDisconnect")
    public static WrappedPlayOutKickDisconnect playOutKickDisconnect(Object rawPacket, PacketContainer<WrappedPlayOutKickDisconnect> packetContainer) {
        return new WrappedPlayOutKickDisconnect(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutLightUpdate")
    public static WrappedPlayOutLightUpdate playOutLightUpdate(Object rawPacket, PacketContainer<WrappedPlayOutLightUpdate> packetContainer) {
        return new WrappedPlayOutLightUpdate(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutLogin")
    public static WrappedPlayOutLogin playOutLogin(Object rawPacket, PacketContainer<WrappedPlayOutLogin> packetContainer) {
        return new WrappedPlayOutLogin(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutLookAt")
    public static WrappedPlayOutLookAt playOutLookAt(Object rawPacket, PacketContainer<WrappedPlayOutLookAt> packetContainer) {
        return new WrappedPlayOutLookAt(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutMap")
    public static WrappedPlayOutMap playOutMap(Object rawPacket, PacketContainer<WrappedPlayOutMap> packetContainer) {
        return new WrappedPlayOutMap(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutMapChunk")
    public static WrappedPlayOutMapChunk playOutMapChunk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunk> packetContainer) {
        return new WrappedPlayOutMapChunk(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutMapChunkBulk")
    public static WrappedPlayOutMapChunkBulk playOutMapChunkBulk(Object rawPacket, PacketContainer<WrappedPlayOutMapChunkBulk> packetContainer) {
        return new WrappedPlayOutMapChunkBulk(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutMount")
    public static WrappedPlayOutMount playOutMount(Object rawPacket, PacketContainer<WrappedPlayOutMount> packetContainer) {
        return new WrappedPlayOutMount(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutMultiBlockChange")
    public static WrappedPlayOutMultiBlockChange playOutMultiBlockChange(Object rawPacket, PacketContainer<WrappedPlayOutMultiBlockChange> packetContainer) {
        return new WrappedPlayOutMultiBlockChange(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutNamedEntitySpawn")
    public static WrappedPlayOutNamedEntitySpawn playOutNamedEntitySpawn(Object rawPacket, PacketContainer<WrappedPlayOutNamedEntitySpawn> packetContainer) {
        return new WrappedPlayOutNamedEntitySpawn(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutNamedSoundEffect")
    public static WrappedPlayOutNamedSoundEffect playOutNamedSoundEffect(Object rawPacket, PacketContainer<WrappedPlayOutNamedSoundEffect> packetContainer) {
        return new WrappedPlayOutNamedSoundEffect(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutNBTQuery")
    public static WrappedPlayOutNBTQuery playOutNBTQuery(Object rawPacket, PacketContainer<WrappedPlayOutNBTQuery> packetContainer) {
        return new WrappedPlayOutNBTQuery(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutOpenBook")
    public static WrappedPlayOutOpenBook playOutOpenBook(Object rawPacket, PacketContainer<WrappedPlayOutOpenBook> packetContainer) {
        return new WrappedPlayOutOpenBook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutOpenSignEditor")
    public static WrappedPlayOutOpenSignEditor playOutOpenSignEditor(Object rawPacket, PacketContainer<WrappedPlayOutOpenSignEditor> packetContainer) {
        return new WrappedPlayOutOpenSignEditor(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutOpenWindow")
    public static WrappedPlayOutOpenWindow playOutOpenWindow(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindow> packetContainer) {
        return new WrappedPlayOutOpenWindow(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutOpenWindowHorse")
    public static WrappedPlayOutOpenWindowHorse playOutOpenWindowHorse(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowHorse> packetContainer) {
        return new WrappedPlayOutOpenWindowHorse(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutOpenWindowMerchant")
    public static WrappedPlayOutOpenWindowMerchant playOutOpenWindowMerchant(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindowMerchant> packetContainer){
        return new WrappedPlayOutOpenWindowMerchant(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutPlayerInfo")
    public static WrappedPlayOutPlayerInfo playOutPlayerInfo(Object rawPacket, PacketContainer<WrappedPlayOutPlayerInfo> packetContainer) {
        return new WrappedPlayOutPlayerInfo(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutPlayerListHeaderFooter")
    public static WrappedPlayOutPlayerListHeaderFooter playOutPlayerListHeaderFooter(Object rawPacket, PacketContainer<WrappedPlayOutPlayerListHeaderFooter> packetContainer) {
        return new WrappedPlayOutPlayerListHeaderFooter(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutPosition")
    public static WrappedPlayOutPosition playOutPosition(Object rawPacket, PacketContainer<WrappedPlayOutPosition> packetContainer) {
        return new WrappedPlayOutPosition(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutRecipes")
    public static WrappedPlayOutRecipes playOutRecipes(Object rawPacket, PacketContainer<WrappedPlayOutRecipes> packetContainer) {
        return new WrappedPlayOutRecipes(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutRecipeUpdate")
    public static WrappedPlayOutRecipeUpdate playOutRecipeUpdate(Object rawPacket, PacketContainer<WrappedPlayOutRecipeUpdate> packetContainer) {
        return new WrappedPlayOutRecipeUpdate(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutRelEntityMove")
    public static WrappedPlayOutRelEntityMove playOutRelEntityMove(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMove> packetContainer) {
        return new WrappedPlayOutRelEntityMove(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntity$PacketPlayOutRelEntityMove")
    public static WrappedPlayOutRelEntityMove playOutRelEntityMove_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMove> packetContainer) {
        return new WrappedPlayOutRelEntityMove(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutRelEntityMoveLook")
    public static WrappedPlayOutRelEntityMoveLook playOutRelEntityMoveLook(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMoveLook> packetContainer) {
        return new WrappedPlayOutRelEntityMoveLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook")
    public static WrappedPlayOutRelEntityMoveLook playOutRelEntityMoveLook_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutRelEntityMoveLook> packetContainer) {
        return new WrappedPlayOutRelEntityMoveLook(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutRemoveEntityEffect")
    public static WrappedPlayOutRemoveEntityEffect playOutRemoveEntityEffect(Object rawPacket, PacketContainer<WrappedPlayOutRemoveEntityEffect> packetContainer) {
        return new WrappedPlayOutRemoveEntityEffect(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutResourcePackSend")
    public static WrappedPlayOutResourcePackSend playOutResourcePackSend(Object rawPacket, PacketContainer<WrappedPlayOutResourcePackSend> packetContainer) {
        return new WrappedPlayOutResourcePackSend(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutRespawn")
    public static WrappedPlayOutRespawn playOutRespawn(Object rawPacket, PacketContainer<WrappedPlayOutRespawn> packetContainer) {
        return new WrappedPlayOutRespawn(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutScoreboardDisplayObjective")
    public static WrappedPlayOutScoreboardDisplayObjective playOutScoreboardDisplayObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardDisplayObjective> packetContainer) {
        return new WrappedPlayOutScoreboardDisplayObjective(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutScoreboardObjective")
    public static WrappedPlayOutScoreboardObjective playOutScoreboardObjective(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardObjective> packetContainer) {
        return new WrappedPlayOutScoreboardObjective(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutScoreboardScore")
    public static WrappedPlayOutScoreboardScore playOutScoreboardScore(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardScore> packetContainer) {
        return new WrappedPlayOutScoreboardScore(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutScoreboardTeam")
    public static WrappedPlayOutScoreboardTeam playOutScoreboardTeam(Object rawPacket, PacketContainer<WrappedPlayOutScoreboardTeam> packetContainer) {
        return new WrappedPlayOutScoreboardTeam(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSelectAdvancementTab")
    public static WrappedPlayOutSelectAdvancementTab playOutSelectAdvancementTab(Object rawPacket, PacketContainer<WrappedPlayOutSelectAdvancementTab> packetContainer) {
        return new WrappedPlayOutSelectAdvancementTab(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutServerDifficulty")
    public static WrappedPlayOutServerDifficulty playOutServerDifficulty(Object rawPacket, PacketContainer<WrappedPlayOutServerDifficulty> packetContainer) {
        return new WrappedPlayOutServerDifficulty(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSetCompression")
    public static WrappedPlayOutSetCompression playOutSetCompression(Object rawPacket, PacketContainer<WrappedPlayOutSetCompression> packetContainer) {
        return new WrappedPlayOutSetCompression(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSetCooldown")
    public static WrappedPlayOutSetCooldown playOutSetCooldown(Object rawPacket, PacketContainer<WrappedPlayOutSetCooldown> packetContainer) {
        return new WrappedPlayOutSetCooldown(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSetSlot")
    public static WrappedPlayOutSetSlot playOutSetSlot(Object rawPacket, PacketContainer<WrappedPlayOutSetSlot> packetContainer) {
        return new WrappedPlayOutSetSlot(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSpawnEntity")
    public static WrappedPlayOutSpawnEntity playOutSpawnEntity(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntity> packetContainer) {
        return new WrappedPlayOutSpawnEntity(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSpawnEntityExperienceOrb")
    public static WrappedPlayOutSpawnEntityExperienceOrb playOutSpawnEntityExperienceOrb(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityExperienceOrb> packetContainer) {
        return new WrappedPlayOutSpawnEntityExperienceOrb(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSpawnEntityLiving")
    public static WrappedPlayOutSpawnEntityLiving playOutSpawnEntityLiving(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityLiving> packetContainer) {
        return new WrappedPlayOutSpawnEntityLiving(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSpawnEntityPainting")
    public static WrappedPlayOutSpawnEntityPainting playOutSpawnEntityPainting(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityPainting> packetContainer) {
        return new WrappedPlayOutSpawnEntityPainting(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSpawnEntityWeather")
    public static WrappedPlayOutSpawnEntityWeather playOutSpawnEntityWeather(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityWeather> packetContainer) {
        return new WrappedPlayOutSpawnEntityWeather(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutSpawnPosition")
    public static WrappedPlayOutSpawnPosition playOutSpawnPosition(Object rawPacket, PacketContainer<WrappedPlayOutSpawnPosition> packetContainer) {
        return new WrappedPlayOutSpawnPosition(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutStatistic")
    public static WrappedPlayOutStatistic playOutStatistic(Object rawPacket, PacketContainer<WrappedPlayOutStatistic> packetContainer) {
        return new WrappedPlayOutStatistic(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutStopSound")
    public static WrappedPlayOutStopSound playOutStopSound(Object rawPacket, PacketContainer<WrappedPlayOutStopSound> packetContainer) {
        return new WrappedPlayOutStopSound(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutTabComplete")
    public static WrappedPlayOutTabComplete playOutTabComplete(Object rawPacket, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        return new WrappedPlayOutTabComplete(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutTags")
    public static WrappedPlayOutTags playOutTags(Object rawPacket, PacketContainer<WrappedPlayOutTags> packetContainer) {
        return new WrappedPlayOutTags(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutTileEntityData")
    public static WrappedPlayOutTileEntityData playOutTileEntityData(Object rawPacket, PacketContainer<WrappedPlayOutTileEntityData> packetContainer) {
        return new WrappedPlayOutTileEntityData(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutTitle")
    public static WrappedPlayOutTitle playOutTitle(Object rawPacket, PacketContainer<WrappedPlayOutTitle> packetContainer) {
        return new WrappedPlayOutTitle(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutTransaction")
    public static WrappedPlayOutTransaction playOutTransaction(Object rawPacket, PacketContainer<WrappedPlayOutTransaction> packetContainer) {
        return new WrappedPlayOutTransaction(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutUnloadChunk")
    public static WrappedPlayOutUnloadChunk playOutUnloadChunk(Object rawPacket, PacketContainer<WrappedPlayOutUnloadChunk> packetContainer) {
        return new WrappedPlayOutUnloadChunk(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutUpdateAttributes")
    public static WrappedPlayOutUpdateAttributes playOutUpdateAttributes(Object rawPacket, PacketContainer<WrappedPlayOutUpdateAttributes> packetContainer) {
        return new WrappedPlayOutUpdateAttributes(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutUpdateEntityNBT")
    public static WrappedPlayOutUpdateEntityNBT playOutUpdateEntityNBT(Object rawPacket, PacketContainer<WrappedPlayOutUpdateEntityNBT> packetContainer) {
        return new WrappedPlayOutUpdateEntityNBT(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutUpdateHealth")
    public static WrappedPlayOutUpdateHealth playOutUpdateHealth(Object rawPacket, PacketContainer<WrappedPlayOutUpdateHealth> packetContainer) {
        return new WrappedPlayOutUpdateHealth(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutUpdateSign")
    public static WrappedPlayOutUpdateSign playOutUpdateSign(Object rawPacket, PacketContainer<WrappedPlayOutUpdateSign> packetContainer) {
        return new WrappedPlayOutUpdateSign(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutUpdateTime")
    public static WrappedPlayOutUpdateTime playOutUpdateTime(Object rawPacket, PacketContainer<WrappedPlayOutUpdateTime> packetContainer) {
        return new WrappedPlayOutUpdateTime(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutVehicleMove")
    public static WrappedPlayOutVehicleMove playOutVehicleMove(Object rawPacket, PacketContainer<WrappedPlayOutVehicleMove> packetContainer) {
        return new WrappedPlayOutVehicleMove(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutViewCentre")
    public static WrappedPlayOutViewCentre playOutViewCentre(Object rawPacket, PacketContainer<WrappedPlayOutViewCentre> packetContainer) {
        return new WrappedPlayOutViewCentre(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutViewDistance")
    public static  WrappedPlayOutViewDistance playOutViewDistance(Object rawPacket, PacketContainer<WrappedPlayOutViewDistance> packetContainer) {
        return new WrappedPlayOutViewDistance(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutWindowData")
    public static WrappedPlayOutWindowData playOutWindowData(Object rawPacket, PacketContainer<WrappedPlayOutWindowData> packetContainer) {
        return new WrappedPlayOutWindowData(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutWindowItems")
    public static WrappedPlayOutWindowItems playOutWindowItems(Object rawPacket, PacketContainer<WrappedPlayOutWindowItems> packetContainer) {
        return new WrappedPlayOutWindowItems(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutWorldBorder")
    public static WrappedPlayOutWorldBorder playOutWorldBorder(Object rawPacket, PacketContainer<WrappedPlayOutWorldBorder> packetContainer) {
        return new WrappedPlayOutWorldBorder(rawPacket, packetContainer);
    }

    @PacketFactory("ClientboundInitializeBorderPacket")
    public static WrappedPlayOutWorldBorder playOutWorldBorder_v1_17(Object rawPacket, PacketContainer<WrappedPlayOutWorldBorder> packetContainer) {
        return new WrappedPlayOutWorldBorder(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutWorldEvent")
    public static WrappedPlayOutWorldEvent playOutWorldEvent(Object rawPacket, PacketContainer<WrappedPlayOutWorldEvent> packetContainer) {
        return new WrappedPlayOutWorldEvent(rawPacket, packetContainer);
    }

    @PacketFactory("PacketPlayOutWorldParticles")
    public static WrappedPlayOutWorldParticles playOutWorldParticles(Object rawPacket, PacketContainer<WrappedPlayOutWorldParticles> packetContainer) {
        return new WrappedPlayOutWorldParticles(rawPacket, packetContainer);
    }
}
