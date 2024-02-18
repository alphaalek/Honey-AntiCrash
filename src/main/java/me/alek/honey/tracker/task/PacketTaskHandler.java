package me.alek.honey.tracker.task;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.handler.DuplexChannelHandler;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketBundleSaver;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.FieldAccessor;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.*;

public class PacketTaskHandler {

    private static final int MAX_HANDLE_TIME_BEFORE_DUMP = 10000;

    public static final Object lock = new Object();
    private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadHandleTracker();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();
    private static final Map<UUID, PacketBundleSaver> packetBundleSavers = DuplexChannelHandler.getPacketBundleSavers();

    private static Tuple2<PacketTaskFuture<?>, Long> handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    while (handledTask != null && System.currentTimeMillis() - handledTask.getT2() > MAX_HANDLE_TIME_BEFORE_DUMP) {
                        UUID uuid = Optional.ofNullable(handledTask.getT1()).map(PacketTaskFuture::getUUID).orElse(null);
                        dumpThread(uuid);
                        unregisterTask();
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

    public static Tuple2<PacketTaskFuture<?>, Long> getHandledTask() {
        return handledTask;
    }

    public static PacketContainer<? extends WrappedPacket<?>> getLastHandledPacket() {
        return lastHandledPacket;
    }

    public static void dumpThread(UUID uuid) {
        String name = Optional.ofNullable(Bukkit.getPlayer(uuid)).map(Player::getName).orElse("Unknown");
        HoneyPlugin.get().getNotificationService().notify(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX + " §cThe server seems to be frozen after a packet was sent by " + name + ". See the log for thread dump and more info."));
        HoneyPlugin.get().getLoggerService().getLogger().severe("*******************************************************************************************");
        HoneyPlugin.get().getLoggerService().getLogger().severe("Packet sent by " + name + " (" + uuid + ")");

        PacketContainer<? extends WrappedPacket<?>> packet = packetBundleSavers.get(uuid).getOldest(false);
        if (packet != null) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("  Packet: " + packet.getType() + ", Id: " + packet.getType().getPacketId() + ", State: " + packet.getState());
            HoneyPlugin.get().getLoggerService().getLogger().severe("  Fields: ");
            for (FieldAccessor<?> accessor : packet.getAll().getAccessors()) {
                Object value = accessor.get(packet.getHandle());
                if (value instanceof Object[]) {
                    value = Arrays.toString((Object[]) value);
                }
                HoneyPlugin.get().getLoggerService().getLogger().severe("   \"" + accessor.getName() + "\": " + accessor.getType().getName() + ": " + value);
            }
        } else HoneyPlugin.get().getLoggerService().getLogger().severe("  Packet data unknown.");

        HoneyPlugin.get().getLoggerService().getLogger().severe("");
        HoneyPlugin.get().getLoggerService().getLogger().severe("  Thread dump (Server Thread):");
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
                HoneyPlugin.get().getLoggerService().getLogger().severe("\t" + element);
                notMatch = true;
            }
            prevElement = element.toString();

            if (notMatch) {
                if (combinedTotal > 0) {
                    HoneyPlugin.get().getLoggerService().getLogger().severe("\t" + "[...] x" + combinedTotal);
                }
                combinedTotal = 0;
            }
        }
        if (prevElement != null)
            HoneyPlugin.get().getLoggerService().getLogger().severe("\t" + prevElement);
        HoneyPlugin.get().getLoggerService().getLogger().severe("End of thread dump");

        HoneyPlugin.get().getLoggerService().getLogger().severe("*******************************************************************************************");
    }

    public static <V> void registerTask(PacketTaskFuture<V> task){
        lastHandledPacket = null;

        PacketBundleSaver bundleSaver = DuplexChannelHandler.getPacketBundleSavers().get(task.getUUID());
        if (bundleSaver != null) {
            PacketContainer<? extends WrappedPacket<?>> packet = bundleSaver.getOldest(false);
            if (packet != null)
                packet.getEventloopStateTimings().startTimings(EventloopState.SERVER_HANDLE);
        }
        handledTask = new Tuple2<>(task, System.currentTimeMillis());
    }

    public static void unregisterTask() {
        synchronized (lock) {
            if (handledTask != null) {
                tracker.decrementPacketsInQueueExecution(handledTask.getT1().getUUID());

                PacketBundleSaver saver = packetBundleSavers.get(handledTask.getT1().getUUID());
                if (saver != null) {
                    PacketContainer<? extends WrappedPacket<?>> packet = saver.getOldest(true);
                    if (packet != null) {
                        packet.getEventloopStateTimings().stopTimings();

                        if (packet.getPlayer() != null) // we have to make sure it's not an unmodifiable packet
                            monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_SERVER_HANDLE);
                    }
                    lastHandledPacket = packet;
                }
                handledTask = null;
            }
        }
    }

    public static <V> PacketTaskFuture<V> wrapTask(Object task, UUID uuid) {
        if (task instanceof Runnable) {
            return new PacketTaskFuture<>((Runnable) task, uuid);
        }
        return null;
    }
}
