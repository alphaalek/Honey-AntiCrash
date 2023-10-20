package me.alek.honey.netty.utility.reflect;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructureCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.UUID;

public class NMSUtils {

    private static final MethodInvoker<Object> getHandle = Reflection.getMethod(
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

    private static final HashMap<UUID, Channel> lookupChannels = new HashMap<>();

    public static synchronized UUID getUUIDForChannel(Channel channel) {
        for (UUID uuid : lookupChannels.keySet()) {
            if (lookupChannels.get(uuid) == channel) {
                return uuid;
            }
        }
        return null;
    }

    public static synchronized Channel getChannel(Player player) {
        UUID uuid = player.getUniqueId();

        if (!lookupChannels.containsKey(uuid)) {

            Channel playerChannel = channel.get(getNetworkManager(player));
            lookupChannels.put(uuid, playerChannel);
        }
        return lookupChannels.get(uuid);
    }

    public static void removeChannelLookup(UUID uuid) {
        lookupChannels.remove(uuid);
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

    public static Object getWorld(Player player) {
        IStructureModifier<Object> craftWorldModifier = ReflectStructureCache.acquireStructure(getCraftWorldClass())
                .withType(NMSUtils.getWorldServerClass()).withTarget(player.getWorld());

        return craftWorldModifier.invokeMethod(0);
    }

    public static Object getMinecraftServer() {
        IStructureModifier<Object> craftServerModifier = ReflectStructureCache.acquireStructure(NMSUtils.getCraftServerClass())
                .withType(NMSUtils.getMinecraftServerClass()).withTarget(Bukkit.getServer());

        return craftServerModifier.invokeMethod("getServer", 0);
    }

    public static Object getServerConnection() {
        Object minecraftServer = getMinecraftServer();

        IStructureModifier<Object> minecraftServerModifier = ReflectStructureCache.acquireStructure(NMSUtils.getMinecraftServerClass())
                .withType(NMSUtils.getServerConnectionClass())
                .withTarget(minecraftServer);

        return minecraftServerModifier.invokeMethod("getServerConnection", 0);
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

    public static Class<Object> getPacketListenerClass() {
        return getFuzzyClass("{nms}.PacketListener");
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

    public static Class<Object> getServerConnectionClass() {
        return getFuzzyClass("{nms}.ServerConnection", "{nms}.server.network.ServerConnection");
    }

    public static Class<Object> getCraftPlayerClass() {
        return getFuzzyClass("{obc}.entity.CraftPlayer");
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

    public static Class<Object> getIChunkLoaderClass() {
        return getFuzzyClass("{nms}.IChunkLoader");
    }

    public static Class<Object> getChunkRegionLoaderClass() {
        return getFuzzyClass("{nms}.ChunkRegionLoader");
    }

    public static Class<Object> getChunkCoordIntPairClass() {
        return getFuzzyClass("{nms}.ChunkCoordIntPair");
    }

    public static Class<Object> getWorldServerClass() {
        return getFuzzyClass("{nms}.server.level.WorldServer", "{nms}.WorldServer");
    }
    public static Class<Object> getWorldClass() {
        return getFuzzyClass("{nms}.world.level.World", "{nms}.World");
    }

    public static Class<Object> getChunkProviderServerClass() {
        return getFuzzyClass("{nms}.ChunkProviderServer");
    }

    public static Class<Object> getLongObjectHashMapClass() {
        return getFuzzyClass("{obc}.util.LongObjectHashMap");
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

    public static Class<Object> getEntityUseActionClass() {
        return getFuzzyClass("{nms}.PacketPlayInUseEntity$EnumEntityUseAction", "{nms}.network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction");
    }

    public static Class<Object> getVec3DClass() {
        return getFuzzyClass("{nms}.Vec3D", "{nms}.world.phys.Vec3D");
    }

    public static Class<Object> getItemClass() {
        return getFuzzyClass("{nms}.Item", "{nms}.world.item.Item");
    }

    public static Class<Object> getRegistryMaterialsClass() {
        return getFuzzyClass("{nms}.RegistryMaterials");
    }

    public static Class<Object> getNBTTagCompoundClass() {
        return getFuzzyClass("{nms}.NBTTagCompound", "{nms}.nbt.NBTTagCompound");
    }

    public static Class<Object> getNBTCompressedToolsClass() {
        return getFuzzyClass("{nms}.NBTCompressedTools", "{nms}.nbt.NBTCompressedTools");
    }

    public static Class<Object> getPacketDataSerializerClass() {
        return getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    }

    public static Class<Object> getEnumProtocolClass() {
        return getFuzzyClass("{nms}.EnumProtocol", "{nms}.network.EnumProtocol");
    }

    @SuppressWarnings("unchecked")
    public static Class<Enum<?>> getEnumProtocolDirectionClass() {
        return (Class<Enum<?>>) Reflection.getFuzzyClass("{nms}.EnumProtocolDirection", "{nms}.network.protocol.EnumProtocolDirection");
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

    public static Class<Object> getMovingObjectPositionClass() {
        return getFuzzyClass("{nms}.world.phys.MovingObjectPosition", "{nms}.MovingObjectPosition");
    }

    public static Class<Object> getMovingObjectPositionBlockClass() {
        return getFuzzyClass("{nms}.world.phys.MovingObjectPositionBlock", "{nms}.MovingObjectPositionBlock");
    }

    public static Class<Object> getIBlockAccessClass() {
        return getFuzzyClass("{nms}.world.level.IBlockAccess", "{nms}.IBlockAccess");
    }

    public static Class<Object> getRayTraceClass() {
        return getFuzzyClass("{nms}.world.level.RayTrace", "{nms}.RayTrace");
    }

    public static Class<Object> getIChatBaseComponentClass() {
        return getFuzzyClass("{nms}.IChatBaseComponent");
    }

    public static Class<Object> getChatSerializerClass() {
        return getFuzzyClass("{nms}.IChatBaseComponent$ChatSerializer");
    }

    public static Class<Object> getEntityClass() {
        return getFuzzyClass("{nms}.world.entity.Entity", "{nms}.Entity");
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
