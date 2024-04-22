package me.alek.honey.netty.util;

import io.netty.channel.Channel;
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

    public static synchronized Channel getChannelIfSet(String name) {
        return getChannel(name);
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

    public static Class<Object> getFuzzyClass(String... params) {
        return Reflection.getFuzzyObjectClass(params);
    }

    private static Class<Object> gameProfileClass;
    public static Class<Object> getGameProfileClass() {
        return gameProfileClass != null ? gameProfileClass : (gameProfileClass = getFuzzyClass("com.mojang.authlib.GameProfile"));
    }

    private static Class<Object> entityPlayerClass;
    public static Class<Object> getEntityPlayerClass() {
        return entityPlayerClass != null ? entityPlayerClass : (entityPlayerClass = getFuzzyClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer"));
    }

    private static Class<Object> entityClass;
    public static Class<Object> getEntityClass() {
        return entityClass != null ? entityClass : (entityClass = getFuzzyClass("{nms}.Entity", "{nms}.world.entity.Entity"));
    }

    private static Class<Object> playerConnectionClass;
    public static Class<Object> getPlayerConnectionClass() {
        return playerConnectionClass != null ? playerConnectionClass : (playerConnectionClass = getFuzzyClass("{nms}.PlayerConnection", "{nms}.server.network.PlayerConnection"));
    }

    private static Class<Object> networkManagerClass;
    public static Class<Object> getNetworkManagerClass() {
        return networkManagerClass != null ? networkManagerClass : (networkManagerClass = getFuzzyClass("{nms}.NetworkManager", "{nms}.network.NetworkManager"));
    }

    private static Class<Object> minecraftServerClass;
    public static Class<Object> getMinecraftServerClass() {
        return minecraftServerClass != null ? minecraftServerClass : (minecraftServerClass = getFuzzyClass("{nms}.MinecraftServer", "{nms}.server.MinecraftServer"));
    }

    private static Class<Object> craftServerClass;
    public static Class<Object> getCraftServerClass() {
        return craftServerClass != null ? craftServerClass : (craftServerClass = getFuzzyClass("{obc}.CraftServer"));
    }

    private static Class<Object> IAsyncTaskHandlerClass;
    public static Class<Object> getIAsyncTaskHandlerClass() {
        return IAsyncTaskHandlerClass != null ? IAsyncTaskHandlerClass : (IAsyncTaskHandlerClass =getFuzzyClass("{nms}.IAsyncTaskHandler", "{nms}.util.thread.IAsyncTaskHandler"));
    }

    private static Class<Object> serverConnectionClass;
    public static Class<Object> getServerConnectionClass() {
        return serverConnectionClass != null ? serverConnectionClass : (serverConnectionClass = getFuzzyClass("{nms}.ServerConnection", "{nms}.server.network.ServerConnection"));
    }

    private static Class<Object> craftPlayerClass;
    public static Class<Object> getCraftPlayerClass() {
        return craftPlayerClass != null ? craftPlayerClass : (craftPlayerClass = getFuzzyClass("{obc}.entity.CraftPlayer"));
    }

    private static Class<Object> craftEntityClass;
    public static Class<Object> getCraftEntityClass() {
        return craftEntityClass != null ? craftEntityClass : (craftEntityClass = getFuzzyClass("{obc}.entity.CraftEntity"));
    }

    private static Class<Object> chunkCoordIntPairClass;
    public static Class<Object> getChunkCoordIntPairClass() {
        return chunkCoordIntPairClass != null ? chunkCoordIntPairClass : (chunkCoordIntPairClass = getFuzzyClass("{nms}.ChunkCoordIntPair"));
    }

    private static Class<Object> worldServerClass;
    public static Class<Object> getWorldServerClass() {
        return worldServerClass != null ? worldServerClass : (worldServerClass = getFuzzyClass("{nms}.server.level.WorldServer", "{nms}.WorldServer"));
    }

    private static Class<Object> baseBlockPositionClass;
    public static Class<Object> getBaseBlockPositionClass() {
        return baseBlockPositionClass != null ? baseBlockPositionClass : (baseBlockPositionClass = getFuzzyClass("{nms}.BaseBlockPosition", "{nms}.core.BaseBlockPosition"));
    }

    private static Class<Object> blockPositionClass;
    public static Class<Object> getBlockPositionClass() {
        return blockPositionClass != null ? blockPositionClass : (blockPositionClass = getFuzzyClass("{nms}.BlockPosition", "{nms}.core.BlockPosition"));
    }

    private static Class<Object> itemStackClass;
    public static Class<Object> getItemStackClass() {
        return itemStackClass != null ? itemStackClass : (itemStackClass = getFuzzyClass("{nms}.ItemStack", "{nms}.world.item.ItemStack"));
    }

    private static Class<Object> itemClass;
    public static Class<Object> getItemClass() {
        return itemClass != null ? itemClass : (itemClass = getFuzzyClass("{nms}.Item", "{nms}.world.item.Item"));
    }

    private static Class<Object> IMaterialClass;
    public static Class<Object> getIMaterialClass() {
        return IMaterialClass != null ? IMaterialClass : (IMaterialClass = getFuzzyClass("{nms}.world.level.IMaterial"));
    }

    private static Class<Object> entityUseActionClass;
    public static Class<Object> getEntityUseActionClass() {
        return entityUseActionClass != null ? entityUseActionClass : (entityUseActionClass = getFuzzyClass("{nms}.PacketPlayInUseEntity$EnumEntityUseAction", "{nms}.network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction"));
    }

    private static Class<Object> vec3DClass;
    public static Class<Object> getVec3DClass() {
        return vec3DClass != null ? vec3DClass : (vec3DClass = getFuzzyClass("{nms}.Vec3D", "{nms}.world.phys.Vec3D"));
    }

    private static Class<Object> tickTaskClass;
    public static Class<Object> getTickTaskClass() {
        return tickTaskClass != null ? tickTaskClass : (tickTaskClass = getFuzzyClass("{nms}.TickTask", "{nms}.server.TickTask"));
    }

    private static Class<Object> nbtTagCompoundClass;
    public static Class<Object> getNBTTagCompoundClass() {
        return nbtTagCompoundClass != null ? nbtTagCompoundClass : (nbtTagCompoundClass = getFuzzyClass("{nms}.NBTTagCompound", "{nms}.nbt.NBTTagCompound"));
    }

    private static Class<Object> nbtBaseClass;
    public static Class<Object> getNBTBaseClass() {
        return nbtBaseClass != null ? nbtBaseClass : (nbtBaseClass = getFuzzyClass("{nms}.NBTBase", "{nms}.nbt.NBTBase"));
    }

    private static Class<Object> nbtTagListClass;
    public static Class<Object> getNBTTagListClass() {
        return nbtTagListClass != null ? nbtTagListClass : (nbtTagListClass = getFuzzyClass("{nms}.NBTTagList", "{nms}.nbt.NBTTagList"));
    }

    private static Class<Object> packetDataSerializerClass;
    public static Class<Object> getPacketDataSerializerClass() {
        return packetDataSerializerClass != null ? packetDataSerializerClass : (packetDataSerializerClass = getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer"));
    }

    private static Class<Object> packetDecoderClass;
    public static Class<Object> getPacketDecoderClass() {
        return packetDecoderClass != null ? packetDecoderClass : (packetDecoderClass = getFuzzyClass("{nms}.PacketDecoder", "{nms}.network.PacketDecoder"));
    }

    private static Class<Object> packetEncoderClass;
    public static Class<Object> getPacketEncoderClass() {
        return packetEncoderClass != null ? packetEncoderClass : (packetEncoderClass = getFuzzyClass("{nms}.PacketEncoder", "{nms}.network.PacketEncoder"));
    }

    private static Class<Enum<?>> enumProtocolDirectionClass;
    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolDirectionClass() {
        return enumProtocolDirectionClass != null ? enumProtocolDirectionClass : (enumProtocolDirectionClass = (Class<Enum<?>>) Reflection.getFuzzyClass("{nms}.EnumProtocolDirection", "{nms}.network.protocol.EnumProtocolDirection"));
    }

    private static Class<Enum<?>> enumProtocolClass;
    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolClass() {
        return enumProtocolClass != null ? enumProtocolClass : (enumProtocolClass = (Class<Enum<?>>) Reflection.getFuzzyClass("{nms}.EnumProtocol", "{nms}.network.EnumProtocol"));
    }

    private static Class<Object> packetClass;
    public static Class<Object> getPacketClass() {
        return packetClass != null ? packetClass : (packetClass = getFuzzyClass("{nms}.Packet", "{nms}.network.protocol.Packet"));
    }

    private static Class<Object> suggestionsClass;
    public static Class<Object> getSuggestionsClass() {
        return suggestionsClass != null ? suggestionsClass : (suggestionsClass = getFuzzyClass("com.mojang.brigadier.suggestion.Suggestions"));
    }

    private static Class<Object> suggestionClass;
    public static Class<Object> getSuggestionClass() {
        return suggestionClass != null ? suggestionClass : (suggestionClass = getFuzzyClass("com.mojang.brigadier.suggestion.Suggestion"));
    }

    private static Class<Object> stringRangeClass;
    public static Class<Object> getStringRangeClass() {
        return stringRangeClass != null ? stringRangeClass : (stringRangeClass = getFuzzyClass("com.mojang.brigadier.context.StringRange"));
    }

    private static Class<Object> movingObjectPositionBlockClass;
    public static Class<Object> getMovingObjectPositionBlockClass() {
        return movingObjectPositionBlockClass != null ? movingObjectPositionBlockClass : (movingObjectPositionBlockClass = getFuzzyClass("{nms}.world.phys.MovingObjectPositionBlock", "{nms}.MovingObjectPositionBlock"));
    }

    private static Class<Object> IChatBaseComponentClass;
    public static Class<Object> getIChatBaseComponentClass() {
        return IChatBaseComponentClass != null ? IChatBaseComponentClass : (IChatBaseComponentClass = getFuzzyClass("{nms}.IChatBaseComponent", "{nms}.network.chat.IChatBaseComponent"));
    }

    private static Class<Object> chatSerializerClass;
    public static Class<Object> getChatSerializerClass() {
        return chatSerializerClass != null ? chatSerializerClass : (chatSerializerClass = getFuzzyClass("{nms}.IChatBaseComponent$ChatSerializer"));
    }

    private static Class<Object> inventoryClickTypeClass;
    public static Class<Object> getInventoryClickTypeClass() {
        return inventoryClickTypeClass != null ? inventoryClickTypeClass : (inventoryClickTypeClass = getFuzzyClass("{nms}.InventoryClickType", "{nms}.world.inventory.InventoryClickType", "{nms}.world.inventory.ClickType"));
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
