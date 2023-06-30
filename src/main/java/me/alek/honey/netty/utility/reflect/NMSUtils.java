package me.alek.honey.netty.utility.reflect;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.UUID;

public class NMSUtils {

    private static final MethodInvoker getHandle = Reflection.getMethod(
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

    public static UUID getUUIDForChannel(Channel channel) {
        for (UUID uuid : lookupChannels.keySet()) {
            if (lookupChannels.get(uuid) == channel) {
                return uuid;
            }
        }
        return null;
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

    public static Channel getChannel(Player player) {
        if (!lookupChannels.containsKey(player.getUniqueId())) {
            Channel playerChannel = channel.get(getNetworkManager(player));
            lookupChannels.put(player.getUniqueId(), playerChannel);
        }
        return lookupChannels.get(player.getUniqueId());
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

    public static Class<Object> getEntityPlayerClass() {
        return getFuzzyClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer");
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
