package me.alek.honey.netty.utility.reflect;

import io.netty.channel.Channel;
import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class NMSUtils {

    private static final boolean NEW_PROTOCOL = Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_17);;
    private static final Class<?> craftPlayerClass = Reflection.getClass("{obc}.entity.CraftPlayer");
    private static final Class<?> entityPlayerClass = getEntityPlayerClass();
    private static final Class<?> playerConnectionClass = getPlayerConnectionClass();
    private static final Class<?> networkManagerClass = getNetworkManagerClass();
    private static final MethodInvoker getHandle = Reflection.getMethod(craftPlayerClass, "getHandle", entityPlayerClass);
    private static final FieldAccessor<Object> playerConnection = Reflection.getField(
            entityPlayerClass, 0, playerConnectionClass);
    private static final FieldAccessor<Object> networkManager = Reflection.getField(
            playerConnectionClass, 0, networkManagerClass);
    private static final FieldAccessor<Channel> channel = Reflection.getField(
            networkManagerClass, 0, Channel.class);

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

    public static boolean isFakeChannel(Channel channel) {
        if (channel == null) {
            return true;
        }
        return channel.getClass().getSimpleName().equals("FakeChannel") || channel.getClass().getSimpleName().equals("SpoofedChannel");
    }
}
