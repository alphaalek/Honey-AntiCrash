package me.alek.honey.tracker.task;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.*;
import java.util.concurrent.FutureTask;

public class PacketTaskHandler {

    private static final int MAX_HANDLE_TIME_BEFORE_INTERRUPT = 1000;
    private static final int MAX_HANDLE_TIME_BEFORE_DUMP = 10000;

    private static final Map<UUID, Queue<PacketContainer<? extends WrappedPacket<?>>>> lastPacketMap;

    private static final Queue<Tuple2<PacketTaskFuture<?>, Long>> tasksInserted;

    static {
        tasksInserted = new LinkedList<>();
        lastPacketMap = DuplexChannelHandler.getLastPacketMap();

        new Thread(() -> {
            while (true) {
                Tuple2<PacketTaskFuture<?>, Long> task;
                while ((task = tasksInserted.peek()) != null && System.currentTimeMillis() - task.getT2() > MAX_HANDLE_TIME_BEFORE_INTERRUPT) {
                    task.getT1().cancel(true);

                    if (System.currentTimeMillis() - task.getT2() > MAX_HANDLE_TIME_BEFORE_DUMP) {

                        UUID uuid = Optional.ofNullable(task.getT1()).map(PacketTaskFuture::getUUID).orElse(null);
                        dumpThread(uuid);
                        unregisterTask(uuid);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    public static void dumpThread(UUID uuid) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX + " §cThe server seems to be a lot of ticks behind after a packet being sent. Could it be a crash exploit? See the log for thread dump and more info."));
        Bukkit.getLogger().severe("*******************************************************************************************");
        Bukkit.getLogger().severe("Packet sent by " + Optional.ofNullable(Bukkit.getPlayer(uuid)).map(Player::getName).orElse("Unknown") + " (" + uuid + ")");

        PacketContainer<? extends WrappedPacket<?>> packet = lastPacketMap.get(uuid).poll();
        if (packet != null) {
            Bukkit.getLogger().severe("  Packet: " + packet.getType() + ", Id: " + packet.getType().getPacketId() + ", State: " + packet.getState());
            Bukkit.getLogger().severe("  Fields: ");
            for (FieldAccessor<?> accessor : packet.getAll().getAccessors()) {
                Object value = accessor.get(packet.getHandle());
                if (value instanceof Object[]) {
                    value = Arrays.toString((Object[]) value);
                }
                Bukkit.getLogger().severe("   \"" + accessor.getName() + "\": " + accessor.getType().getName() + ": " + value);
            }
        } else Bukkit.getLogger().severe("  Packet data unknown.");

        Bukkit.getLogger().severe("");
        Bukkit.getLogger().severe("  Thread dump (Server Thread):");
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(HoneyPlugin.get().getServerThread().getId(), 50);

        StackTraceElement[] elements = info.getStackTrace();
        String prevElement = null;
        int combinedTotal = 0;
        for (StackTraceElement element : elements) {
            boolean notMatch = false;
            if (element.toString().equals(prevElement)) {
                combinedTotal++;
            }
            else {
                Bukkit.getLogger().severe("\t" + element);
                notMatch = true;
            }
            prevElement = element.toString();

            if (notMatch) {
                if (combinedTotal > 0) {
                    Bukkit.getLogger().severe("\t" + "[...] x" + combinedTotal);
                }
                combinedTotal = 0;
            }
        }
        if (prevElement != null)
            Bukkit.getLogger().severe("\t" + prevElement);
        Bukkit.getLogger().severe("End of thread dump");

        Bukkit.getLogger().severe("*******************************************************************************************");
    }

    public static <V> void registerTask(PacketTaskFuture<V> task ){
        tasksInserted.add(new Tuple2<>(task, System.currentTimeMillis()));
    }

    public static void unregisterTask(UUID uuid) {
        Tuple2<PacketTaskFuture<?>, Long> task;
        if ((task = tasksInserted.peek()) != null && task.getT1().getUUID() == uuid) {
            tasksInserted.poll();
            lastPacketMap.get(uuid).poll();
        }
    }

    public static <V> PacketTaskFuture<V> wrapTask(Object task, UUID uuid) {
        if (task instanceof FutureTask) {
            return new PacketTaskFuture<>((FutureTask<?>) task, uuid);
        }
        if (task instanceof Runnable) {
            return new PacketTaskFuture<>((Runnable) task, uuid);
        }
        return null;
    }
}
