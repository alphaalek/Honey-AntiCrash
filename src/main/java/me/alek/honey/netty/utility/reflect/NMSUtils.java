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

    public static Class<?> getEntityPlayerClass() {
        return Reflection.getFuzzyClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer");
    }

    public static Class<?> getPlayerConnectionClass() {
        return Reflection.getFuzzyClass("{nms}.PlayerConnection", "{nms}.server.network.PlayerConnection");
    }

    public static Class<?> getNetworkManagerClass() {
        return Reflection.getFuzzyClass("{nms}.NetworkManager", "{nms}.network.NetworkManager");
    }

    public static Class<?> getMinecraftServerClass() {
        return Reflection.getFuzzyClass("{nms}.MinecraftServer", "{nms}.server.MinecraftServer");
    }

    public static Class<?> getServerConnectionClass() {
        return Reflection.getFuzzyClass("{nms}.ServerConnection", "{nms}.server.network.ServerConnection");
    }

    public static Class<?> getCraftPlayerClass() {
        return Reflection.getClass("{obc}.entity.CraftPlayer");
    }

    public static Class<?> getBaseBlockPositionClass() {
        return Reflection.getFuzzyClass("{nms}.BaseBlockPosition", "{nms}.core.BaseBlockPosition");
    }

    public static Class<Object> getBlockPositionClass() {
        return Reflection.getFuzzyObjectClass("{nms}.BlockPosition", "{nms}.core.BlockPosition");
    }

    public static Class<Object> getItemStackClass() {
        return Reflection.getFuzzyObjectClass("{nms}.ItemStack", "{nms}.world.item.ItemStack");
    }

    public static Class<Object> getNBTTagCompoundClass() {
        return Reflection.getFuzzyObjectClass("{nms}.NBTTagCompound", "{nms}.nbt.NBTTagCompound");
    }

    public static Class<Object> getNBTBaseClass() {
        return Reflection.getFuzzyObjectClass("{nms}.NBTBase", "{nms}.nbt.NBTBase");
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
