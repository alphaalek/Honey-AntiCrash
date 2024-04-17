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

    public static Class<Object> getGameProfileClass() {
        return getFuzzyClass("com.mojang.authlib.GameProfile");
    }

    public static Class<Object> getEntityPlayerClass() {
        return getFuzzyClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer");
    }

    public static Class<Object> getEntityClass() {
        return getFuzzyClass("{nms}.Entity", "{nms}.world.entity.Entity");
    }

    public static Class<Object> getPlayerConnectionClass() {
        return getFuzzyClass("{nms}.PlayerConnection", "{nms}.server.network.PlayerConnection");
    }

    public static Class<Object> getNetworkManagerClass() {
        return getFuzzyClass("{nms}.NetworkManager", "{nms}.network.NetworkManager");
    }

    public static Class<Object> getMinecraftServerClass() {
        return getFuzzyClass("{nms}.MinecraftServer", "{nms}.server.MinecraftServer");
    }

    public static Class<Object> getCraftServerClass() {
        return getFuzzyClass("{obc}.CraftServer");
    }

    public static Class<Object> getIAsyncTaskHandlerClass() {
        return getFuzzyClass("{nms}.IAsyncTaskHandler", "{nms}.util.thread.IAsyncTaskHandler");
    }

    public static Class<Object> getServerConnectionClass() {
        return getFuzzyClass("{nms}.ServerConnection", "{nms}.server.network.ServerConnection");
    }

    public static Class<Object> getCraftPlayerClass() {
        return getFuzzyClass("{obc}.entity.CraftPlayer");
    }

    public static Class<Object> getCraftEntityClass() {
        return getFuzzyClass("{obc}.entity.CraftEntity");
    }

    public static Class<Object> getCraftWorldClass() {
        return getFuzzyClass("{obc}.CraftWorld");
    }

    public static Class<Object> getCraftChunkClass() {
        return getFuzzyClass("{obc}.CraftChunk");
    }

    public static Class<Object> getChunkClass() {
        return getFuzzyClass("{nms}.Chunk");
    }

    public static Class<Object> getChunkCoordIntPairClass() {
        return getFuzzyClass("{nms}.ChunkCoordIntPair");
    }

    public static Class<Object> getWorldServerClass() {
        return getFuzzyClass("{nms}.server.level.WorldServer", "{nms}.WorldServer");
    }

    public static Class<Object> getBaseBlockPositionClass() {
        return getFuzzyClass("{nms}.BaseBlockPosition", "{nms}.core.BaseBlockPosition");
    }

    public static Class<Object> getBlockPositionClass() {
        return getFuzzyClass("{nms}.BlockPosition", "{nms}.core.BlockPosition");
    }

    public static Class<Object> getItemStackClass() {
        return getFuzzyClass("{nms}.ItemStack", "{nms}.world.item.ItemStack");
    }

    public static Class<Object> getItemClass() {
        return getFuzzyClass("{nms}.Item", "{nms}.world.item.Item");
    }

    public static Class<Object> getEntityUseActionClass() {
        return getFuzzyClass("{nms}.PacketPlayInUseEntity$EnumEntityUseAction", "{nms}.network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction");
    }

    public static Class<Object> getVec3DClass() {
        return getFuzzyClass("{nms}.Vec3D", "{nms}.world.phys.Vec3D");
    }

    public static Class<Object> getTickTaskClass() {
        return getFuzzyClass("{nms}.TickTask", "{nms}.server.TickTask");
    }

    public static Class<Object> getNBTTagCompoundClass() {
        return getFuzzyClass("{nms}.NBTTagCompound", "{nms}.nbt.NBTTagCompound");
    }

    public static Class<Object> getNBTBaseClass() {
        return getFuzzyClass("{nms}.NBTBase", "{nms}.nbt.NBTBase");
    }

    public static Class<Object> getNBTTagListClass() {
        return getFuzzyClass("{nms}.NBTTagList", "{nms}.nbt.NBTTagList");
    }

    public static Class<Object> getPacketDataSerializerClass() {
        return getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    }

    public static Class<Object> getPacketDecoderClass() {
        return getFuzzyClass("{nms}.PacketDecoder", "{nms}.network.PacketDecoder");
    }

    public static Class<Object> getPacketEncoderClass() {
        return getFuzzyClass("{nms}.PacketEncoder", "{nms}.network.PacketEncoder");
    }

    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolDirectionClass() {
        return (Class<Enum<?>>) Reflection.getFuzzyClass("{nms}.EnumProtocolDirection", "{nms}.network.protocol.EnumProtocolDirection");
    }

    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolClass() {
        return (Class<Enum<?>>) Reflection.getFuzzyClass("{nms}.EnumProtocol", "{nms}.network.EnumProtocol");
    }

    public static Class<Object> getPacketClass() {
        return getFuzzyClass("{nms}.Packet", "{nms}.network.protocol.Packet");
    }

    public static Class<Object> getSuggestionsClass() {
        return getFuzzyClass("com.mojang.brigadier.suggestion.Suggestions");
    }

    public static Class<Object> getSuggestionClass() {
        return getFuzzyClass("com.mojang.brigadier.suggestion.Suggestion");
    }

    public static Class<Object> getStringRangeClass() {
        return getFuzzyClass("com.mojang.brigadier.context.StringRange");
    }

    public static Class<Object> getMovingObjectPositionBlockClass() {
        return getFuzzyClass("{nms}.world.phys.MovingObjectPositionBlock", "{nms}.MovingObjectPositionBlock");
    }

    public static Class<Object> getIChatBaseComponentClass() {
        return getFuzzyClass("{nms}.IChatBaseComponent");
    }

    public static Class<Object> getChatSerializerClass() {
        return getFuzzyClass("{nms}.IChatBaseComponent$ChatSerializer");
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
