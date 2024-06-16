package me.alek.honey.netty.util;

import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

public class NMS {

    private static final MethodInvoker<Object> getHandle = Reflection.getMethodFromReturnType(
            getCraftPlayerClass(), "getHandle", getEntityPlayerClass());
    private static final FieldAccessor<Object> playerConnection = Reflection.getField(
            getEntityPlayerClass(), 0, getPlayerConnectionClass());
    private static final FieldAccessor<Object> networkManager = Reflection.getField(
            getPlayerConnectionClass(), 0, getNetworkManagerClass());
    private static final FieldAccessor<Channel> channel = Reflection.getField(
            getNetworkManagerClass(), 0, Channel.class);
    private static final FieldAccessor<SocketAddress> socketAddress = Reflection.getField(
            getNetworkManagerClass(), 0, SocketAddress.class
    );

    private static final HashMap<String, Channel> lookupChannels = new HashMap<>();
    private static Object minecraftServer;
    private static Object serverConnection;

    public static synchronized Channel getChannel(String name) {
        return lookupChannels.get(name);
    }

    public static synchronized void submitChannelForName(String name, Channel channel) {
        lookupChannels.put(name, channel);
    }

    public static Channel getChannelFromPlayer(Player player) {
        return getChannelFromNetworkManager(getNetworkManager(player));
    }

    public static Channel getChannelFromNetworkManager(Object networkManager) {
        return channel.get(networkManager);
    }

    public static void removeChannelLookup(String name) {
        lookupChannels.remove(name);
    }

    public static Object getEntityPlayer(Player player) {
        return getHandle.invoke(player);
    }

    public static Object getPlayerConnection(Player player) {
        return playerConnection.get(getEntityPlayer(player));
    }

    public static Object getNetworkManager(Player player) {
        return networkManager.get(getPlayerConnection(player));
    }

    public static Object getMinecraftServer() {
        return minecraftServer != null
                ? minecraftServer
                : Reflection.getFieldAssignableFrom(NMS.getCraftServerClass(), 0, NMS.getMinecraftServerClass())
                        .get(Bukkit.getServer());
    }

    public static Object getServerConnection() {
        Object firstTry;

        return serverConnection != null ?
                serverConnection
                : ( (firstTry = getOrCreateServerConnection(0)) != null
                        ? firstTry
                        : getOrCreateServerConnection(1) );
    }

    private static Object getOrCreateServerConnection(int fieldIndex) {
        return Reflection.getMethodFromReturnType(NMS.getMinecraftServerClass(), fieldIndex, NMS.getServerConnectionClass())
                .invoke(getMinecraftServer());
    }

    public static InetSocketAddress getSocketAddress(Player player) {
        return (InetSocketAddress) socketAddress.get(getNetworkManager(player));
    }

    public static InetSocketAddress getSocketAddress(Object networkManager) {
        return (InetSocketAddress) socketAddress.get(networkManager);
    }

    public static Class<Object> getUnsureClass(String... params) {
        return Reflection.getUnsureObjectClass(params);
    }

    private static Class<Object> gameProfileClass;
    public static Class<Object> getGameProfileClass() {
        return gameProfileClass != null ? gameProfileClass : (gameProfileClass = getUnsureClass("com.mojang.authlib.GameProfile"));
    }

    private static Class<Object> entityPlayerClass;
    public static Class<Object> getEntityPlayerClass() {
        return entityPlayerClass != null ? entityPlayerClass : (entityPlayerClass = getUnsureClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer"));
    }

    private static Class<Object> entityClass;
    public static Class<Object> getEntityClass() {
        return entityClass != null ? entityClass : (entityClass = getUnsureClass("{nms}.Entity", "{nms}.world.entity.Entity"));
    }

    private static Class<Object> playerConnectionClass;
    public static Class<Object> getPlayerConnectionClass() {
        return playerConnectionClass != null ? playerConnectionClass : (playerConnectionClass = getUnsureClass("{nms}.PlayerConnection", "{nms}.server.network.PlayerConnection"));
    }

    private static Class<Object> networkManagerClass;
    public static Class<Object> getNetworkManagerClass() {
        return networkManagerClass != null ? networkManagerClass : (networkManagerClass = getUnsureClass("{nms}.NetworkManager", "{nms}.network.NetworkManager"));
    }

    private static Class<Object> minecraftServerClass;
    public static Class<Object> getMinecraftServerClass() {
        return minecraftServerClass != null ? minecraftServerClass : (minecraftServerClass = getUnsureClass("{nms}.MinecraftServer", "{nms}.server.MinecraftServer"));
    }

    private static Class<Object> craftServerClass;
    public static Class<Object> getCraftServerClass() {
        return craftServerClass != null ? craftServerClass : (craftServerClass = getUnsureClass("{obc}.CraftServer"));
    }

    private static Class<Object> IAsyncTaskHandlerClass;
    public static Class<Object> getIAsyncTaskHandlerClass() {
        return IAsyncTaskHandlerClass != null ? IAsyncTaskHandlerClass : (IAsyncTaskHandlerClass = getUnsureClass("{nms}.IAsyncTaskHandler", "{nms}.util.thread.IAsyncTaskHandler"));
    }

    private static Class<Object> craftMetaItemClass;
    public static Class<Object> getCraftMetaItemClass() {
        return craftMetaItemClass != null ? craftMetaItemClass : (craftMetaItemClass = getUnsureClass("{obc}.inventory.CraftMetaItem"));
    }

    private static Class<Object> serverConnectionClass;
    public static Class<Object> getServerConnectionClass() {
        return serverConnectionClass != null ? serverConnectionClass : (serverConnectionClass = getUnsureClass("{nms}.ServerConnection", "{nms}.server.network.ServerConnection"));
    }

    private static Class<Object> craftPlayerClass;
    public static Class<Object> getCraftPlayerClass() {
        return craftPlayerClass != null ? craftPlayerClass : (craftPlayerClass = getUnsureClass("{obc}.entity.CraftPlayer"));
    }

    private static Class<Object> craftEntityClass;
    public static Class<Object> getCraftEntityClass() {
        return craftEntityClass != null ? craftEntityClass : (craftEntityClass = getUnsureClass("{obc}.entity.CraftEntity"));
    }

    private static Class<Object> chunkCoordIntPairClass;
    public static Class<Object> getChunkCoordIntPairClass() {
        return chunkCoordIntPairClass != null ? chunkCoordIntPairClass : (chunkCoordIntPairClass = getUnsureClass("{nms}.ChunkCoordIntPair"));
    }

    private static Class<Object> worldServerClass;
    public static Class<Object> getWorldServerClass() {
        return worldServerClass != null ? worldServerClass : (worldServerClass = getUnsureClass("{nms}.server.level.WorldServer", "{nms}.WorldServer"));
    }

    private static Class<Object> baseBlockPositionClass;
    public static Class<Object> getBaseBlockPositionClass() {
        return baseBlockPositionClass != null ? baseBlockPositionClass : (baseBlockPositionClass = getUnsureClass("{nms}.BaseBlockPosition", "{nms}.core.BaseBlockPosition"));
    }

    private static Class<Object> blockPositionClass;
    public static Class<Object> getBlockPositionClass() {
        return blockPositionClass != null ? blockPositionClass : (blockPositionClass = getUnsureClass("{nms}.BlockPosition", "{nms}.core.BlockPosition"));
    }

    private static Class<Object> itemStackClass;
    public static Class<Object> getItemStackClass() {
        return itemStackClass != null ? itemStackClass : (itemStackClass = getUnsureClass("{nms}.ItemStack", "{nms}.world.item.ItemStack"));
    }

    private static Class<Object> itemClass;
    public static Class<Object> getItemClass() {
        return itemClass != null ? itemClass : (itemClass = getUnsureClass("{nms}.Item", "{nms}.world.item.Item"));
    }

    private static Class<Object> IMaterialClass;
    public static Class<Object> getIMaterialClass() {
        return IMaterialClass != null ? IMaterialClass : (IMaterialClass = getUnsureClass("{nms}.world.level.IMaterial"));
    }

    private static Class<Object> entityUseActionClass;
    public static Class<Object> getEntityUseActionClass() {
        return entityUseActionClass != null ? entityUseActionClass : (entityUseActionClass = getUnsureClass("{nms}.PacketPlayInUseEntity$EnumEntityUseAction", "{nms}.network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction"));
    }

    private static Class<Object> vec3DClass;
    public static Class<Object> getVec3DClass() {
        return vec3DClass != null ? vec3DClass : (vec3DClass = getUnsureClass("{nms}.Vec3D", "{nms}.world.phys.Vec3D"));
    }

    private static Class<Object> tickTaskClass;
    public static Class<Object> getTickTaskClass() {
        return tickTaskClass != null ? tickTaskClass : (tickTaskClass = getUnsureClass("{nms}.TickTask", "{nms}.server.TickTask"));
    }

    private static Class<Object> nbtTagCompoundClass;
    public static Class<Object> getNBTTagCompoundClass() {
        return nbtTagCompoundClass != null ? nbtTagCompoundClass : (nbtTagCompoundClass = getUnsureClass("{nms}.NBTTagCompound", "{nms}.nbt.NBTTagCompound"));
    }

    private static Class<Object> nbtBaseClass;
    public static Class<Object> getNBTBaseClass() {
        return nbtBaseClass != null ? nbtBaseClass : (nbtBaseClass = getUnsureClass("{nms}.NBTBase", "{nms}.nbt.NBTBase"));
    }

    private static Class<Object> nbtTagListClass;
    public static Class<Object> getNBTTagListClass() {
        return nbtTagListClass != null ? nbtTagListClass : (nbtTagListClass = getUnsureClass("{nms}.NBTTagList", "{nms}.nbt.NBTTagList"));
    }

    private static Class<Object> packetDataSerializerClass;
    public static Class<Object> getPacketDataSerializerClass() {
        return packetDataSerializerClass != null ? packetDataSerializerClass : (packetDataSerializerClass = getUnsureClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer"));
    }

    private static Class<Object> packetDecoderClass;
    public static Class<Object> getPacketDecoderClass() {
        return packetDecoderClass != null ? packetDecoderClass : (packetDecoderClass = getUnsureClass("{nms}.PacketDecoder", "{nms}.network.PacketDecoder"));
    }

    private static Class<Object> packetEncoderClass;
    public static Class<Object> getPacketEncoderClass() {
        return packetEncoderClass != null ? packetEncoderClass : (packetEncoderClass = getUnsureClass("{nms}.PacketEncoder", "{nms}.network.PacketEncoder"));
    }

    private static Class<Enum<?>> enumProtocolDirectionClass;
    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolDirectionClass() {
        return enumProtocolDirectionClass != null ? enumProtocolDirectionClass : (enumProtocolDirectionClass = (Class<Enum<?>>) Reflection.getUnsureClass("{nms}.EnumProtocolDirection", "{nms}.network.protocol.EnumProtocolDirection"));
    }

    private static Class<Enum<?>> enumProtocolClass;
    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolClass() {
        return enumProtocolClass != null ? enumProtocolClass : (enumProtocolClass = (Class<Enum<?>>) Reflection.getUnsureClass("{nms}.EnumProtocol", "{nms}.network.EnumProtocol"));
    }

    private static Class<Object> packetClass;
    public static Class<Object> getPacketClass() {
        return packetClass != null ? packetClass : (packetClass = getUnsureClass("{nms}.Packet", "{nms}.network.protocol.Packet"));
    }

    private static Class<Object> packetListenerClass;
    public static Class<Object> getPacketListenerClass() {
        return packetListenerClass != null ? packetListenerClass : (packetListenerClass = getUnsureClass("{nms}.PacketListener", "{nms}.network.PacketListener"));
    }

    private static Class<Object> suggestionsClass;
    public static Class<Object> getSuggestionsClass() {
        return suggestionsClass != null ? suggestionsClass : (suggestionsClass = getUnsureClass("com.mojang.brigadier.suggestion.Suggestions"));
    }

    private static Class<Object> suggestionClass;
    public static Class<Object> getSuggestionClass() {
        return suggestionClass != null ? suggestionClass : (suggestionClass = getUnsureClass("com.mojang.brigadier.suggestion.Suggestion"));
    }

    private static Class<Object> stringRangeClass;
    public static Class<Object> getStringRangeClass() {
        return stringRangeClass != null ? stringRangeClass : (stringRangeClass = getUnsureClass("com.mojang.brigadier.context.StringRange"));
    }

    private static Class<Object> movingObjectPositionBlockClass;
    public static Class<Object> getMovingObjectPositionBlockClass() {
        return movingObjectPositionBlockClass != null ? movingObjectPositionBlockClass : (movingObjectPositionBlockClass = getUnsureClass("{nms}.world.phys.MovingObjectPositionBlock", "{nms}.MovingObjectPositionBlock"));
    }

    private static Class<Object> IChatBaseComponentClass;
    public static Class<Object> getIChatBaseComponentClass() {
        return IChatBaseComponentClass != null ? IChatBaseComponentClass : (IChatBaseComponentClass = getUnsureClass("{nms}.IChatBaseComponent", "{nms}.network.chat.IChatBaseComponent"));
    }

    private static Class<Object> chatSerializerClass;
    public static Class<Object> getChatSerializerClass() {
        return chatSerializerClass != null ? chatSerializerClass : (chatSerializerClass = getUnsureClass("{nms}.IChatBaseComponent$ChatSerializer", "{nms}.network.chat.IChatBaseComponent$ChatSerializer"));
    }

    private static Class<Object> inventoryClickTypeClass;
    public static Class<Object> getInventoryClickTypeClass() {
        return inventoryClickTypeClass != null ? inventoryClickTypeClass : (inventoryClickTypeClass = getUnsureClass("{nms}.InventoryClickType", "{nms}.world.inventory.InventoryClickType", "{nms}.world.inventory.ClickType"));
    }

    private static Class<Object> clientIntentClass;
    public static Class<Object> getClientIntentClass() {
        return clientIntentClass != null ? clientIntentClass : (clientIntentClass = getUnsureClass("{nms}.network.protocol.handshake.ClientIntent"));
    }

    private static Class<Object> gamePacketTypesClass;
    public static Class<Object> getGamePacketTypesClass() {
        return gamePacketTypesClass != null ? gamePacketTypesClass : (gamePacketTypesClass = getUnsureClass("{play}.GamePacketTypes"));
    }

    private static Class<Object> loginPacketTypesClass;
    public static Class<Object> getLoginPacketTypesClass() {
        return loginPacketTypesClass != null ? loginPacketTypesClass : (loginPacketTypesClass = getUnsureClass("{login}.LoginPacketTypes"));
    }

    private static Class<Object> handshaakePacketTypesClass;
    public static Class<Object> getHandshakePacketTypesClass() {
        return handshaakePacketTypesClass != null ? handshaakePacketTypesClass : (handshaakePacketTypesClass = getUnsureClass("{handshake}.HandshakePacketTypes"));
    }

    private static Class<Object> statusPacketTypesClass;
    public static Class<Object> getStatusPacketTypesClass() {
        return statusPacketTypesClass != null ? statusPacketTypesClass : (statusPacketTypesClass = getUnsureClass("{status}.StatusPacketTypes"));
    }

    private static Class<Object> configurationPacketTypesClass;
    public static Class<Object> getConfigurationPacketTypesClass() {
        return configurationPacketTypesClass != null ? configurationPacketTypesClass : (configurationPacketTypesClass = getUnsureClass("{configuration}.ConfigurationPacketTypes"));
    }

    private static Class<Object> gameProtocolsClass;
    public static Class<Object> getGameProtocolsClass() {
        return gameProtocolsClass != null ? gameProtocolsClass : (gameProtocolsClass = getUnsureClass("{play}.GameProtocols"));
    }

    private static Class<Object> loginProtocolsClass;
    public static Class<Object> getLoginProtocolsClass() {
        return loginProtocolsClass != null ? loginProtocolsClass : (loginProtocolsClass = getUnsureClass("{login}.LoginProtocols"));
    }

    private static Class<Object> handshakeProtocolsClass;
    public static Class<Object> getHandshakeProtocolsClass() {
        return handshakeProtocolsClass != null ? handshakeProtocolsClass : (handshakeProtocolsClass = getUnsureClass("{handshake}.HandshakeProtocols"));
    }

    private static Class<Object> statusProtocolsClass;
    public static Class<Object> getStatusProtocolsClass() {
        return statusProtocolsClass != null ? statusProtocolsClass : (statusProtocolsClass = getUnsureClass("{status}.StatusProtocols"));
    }

    private static Class<Object> configurationProtocolsClass;
    public static Class<Object> getConfigurationProtocolsClass() {
        return configurationProtocolsClass != null ? configurationProtocolsClass : (configurationProtocolsClass = getUnsureClass("{configuration}.ConfigurationProtocols"));
    }

    private static Class<Object> protocolInfoClass;
    public static Class<Object> getProtocolInfoClass() {
        return protocolInfoClass != null ? protocolInfoClass : (protocolInfoClass = getUnsureClass("{nms}.network.ProtocolInfo"));
    }

    private static Class<Object> protocolInfoBinderClass;
    public static Class<Object> getProtocolInfoBinderClass() {
        return protocolInfoBinderClass != null ? protocolInfoBinderClass : Reflection.asObject(Reflection.getSubClass(protocolInfoClass, 0));
    }

    private static Class<Object> packetTypeClass;
    public static Class<Object> getPacketTypeClass() {
        return packetTypeClass != null ? packetTypeClass : (packetTypeClass = getUnsureClass("{nms}.network.protocol.PacketType"));
    }

    private static Class<Object> streamCodecClass;
    public static Class<Object> getStreamCodecClass() {
        return streamCodecClass != null ? streamCodecClass : (streamCodecClass = getUnsureClass("{nms}.network.codec.StreamCodec"));
    }

    private static Class<Object> streamDecoderClass;
    public static Class<Object> getStreamDecoderClass() {
        return streamDecoderClass != null ? streamDecoderClass : (streamDecoderClass = getUnsureClass("{nms}.network.codec.StreamDecoder"));
    }

    private static Class<Object> streamEncoderClass;
    public static Class<Object> getStreamEncoderClass() {
        return streamEncoderClass != null ? streamEncoderClass : (streamEncoderClass = getUnsureClass("{nms}.network.codec.StreamEncoder"));
    }

    private static Class<Object> layeredRegistryAccessClass;
    public static Class<Object> getLayeredRegistryAccessClass() {
        return layeredRegistryAccessClass != null ? layeredRegistryAccessClass : (layeredRegistryAccessClass = getUnsureClass("{nms}.core.LayeredRegistryAccess"));
    }

    private static Class<Object> registryCustomDimensionClass;
    public static Class<Object> getRegistryCustomDimensionClass() {
        return registryCustomDimensionClass != null ? registryCustomDimensionClass : (registryCustomDimensionClass = getUnsureClass("{nms}.core.IRegistryCustom$Dimension"));
    }

    private static Class<Object> registryFriendlyByteBufClass;
    public static Class<Object> getRegistryFriendlyByteBufClass() {
        return registryFriendlyByteBufClass != null ? registryFriendlyByteBufClass: (registryFriendlyByteBufClass = getUnsureClass("{nms}.network.RegistryFriendlyByteBuf"));
    }

    private static Class<Object> minecraftKeyClass;
    public static Class<Object> getMinecraftKeyClass() {
        return minecraftKeyClass != null ? minecraftKeyClass : (minecraftKeyClass = getUnsureClass("{nms}.MinecraftKey", "{nms}.resources.MinecraftKey"));
    }

    private static Class<?> customPacketPayloadClass;
    public static Class<?> getCustomPacketPayload1_20_2Class() {
        return customPacketPayloadClass != null ? customPacketPayloadClass : (customPacketPayloadClass = getUnsureClass("{nms}.network.protocol.common.custom.CustomPacketPayload"));
    }

    private static Class<Object> object2IntMapClass;
    public static Class<Object> getObject2IntMapClass() {
        if (object2IntMapClass != null) {
            return object2IntMapClass;
        }

        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_17_1)) {
            return object2IntMapClass = getUnsureClass("org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2IntMap");
        }

        return object2IntMapClass = (Class<Object>) (Class<?>) Object2IntMap.class;
    }

    public static boolean isFakeChannel(Channel channel) {
        if (channel == null) {
            return true;
        }
        return channel.getClass().getSimpleName().equals("FakeChannel") || channel.getClass().getSimpleName().equals("SpoofedChannel");
    }

    public static boolean isBlockPosition(Object object) {
        return getBaseBlockPositionClass().isInstance(object);
    }
}
