package me.alek.honey.tracker.packet;

import io.netty.util.concurrent.DefaultThreadFactory;
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

    public static final Object lock = new Object();
    private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadHandleTracker();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();
    private static final Map<UUID, PacketBundleSaver> packetBundleSavers = DuplexChannelHandler.getPacketBundleSavers();

    private static Tuple2<PacketTaskFuture<?>, Long> handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new DefaultThreadFactory("Honey Packet Task Handler").newThread(() -> {
            while (HoneyPlugin.get().isRunning()) {
                synchronized (lock) {
                    if (handledTask != null && ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                            && System.currentTimeMillis() - handledTask.getT2() >= ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_THREAD_DUMP) {

                        PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket();
                        if (packet == null || packet.isDumped()) continue;

                        dumpThread(packet.getPlayer().getUniqueId());
                        packet.dump();
                    }
                }
                try {
                    Thread.sleep(50);
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
        HoneyPlugin.get().getLoggerService().getLogger().severe("Auto thread dump for heavy packet sent by " + name + ":");

        PacketContainer<? extends WrappedPacket<?>> packet = packetBundleSavers.get(uuid).getOldest(false);
        if (packet != null) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Packet: " + packet.getType() + ", Id: " + packet.getType().getPacketId() + ", State: " + packet.getState());

            Map<String, Object> fields = packet.getAll().readFieldEntries();
            for (Map.Entry<String,? > field : fields.entrySet()) {
                fields.put(field.getKey(), (field.getValue() instanceof Object[]) ? Arrays.toString((Object[]) field.getValue()) : field.getValue());
            }
            HoneyPlugin.get().getLoggerService().getLogger().severe("Properties: " + fields);

        } else HoneyPlugin.get().getLoggerService().getLogger().severe("Packet data unknown.");

        HoneyPlugin.get().getLoggerService().getLogger().severe("");
        HoneyPlugin.get().getLoggerService().getLogger().severe("Thread dump:");
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
                HoneyPlugin.get().getLoggerService().getLogger().severe(element.toString());
                notMatch = true;
            }
            prevElement = element.toString();

            if (notMatch) {
                if (combinedTotal > 0) {
                    HoneyPlugin.get().getLoggerService().getLogger().severe("[...] x" + combinedTotal);
                }
                combinedTotal = 0;
            }
        }
        if (prevElement != null)
            HoneyPlugin.get().getLoggerService().getLogger().severe(prevElement);
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

    public static PacketContainer<? extends WrappedPacket<?>> getCurrentHandledPacket() {
        PacketBundleSaver saver = packetBundleSavers.get(handledTask.getT1().getUUID());
        if (saver != null)
            return saver.getOldest(true);
        else
            return null;
    }

    public static void unregisterTask() {
        synchronized (lock) {
            if (handledTask != null) {
                tracker.decrementPacketsInQueueExecution(handledTask.getT1().getUUID());

                PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket();
                if (packet != null) {
                    packet.getEventloopStateTimings().stopTimings();

                    if (ConfigurationOptions.PacketDump.HEAVY_PACKET_DUMP_ENABLED
                            && packet.getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_HANDLE) > ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_PACKET_DUMP) {
                        HoneyPlugin.get().getLoggerService().dumpAutoHeavyPacket(packet);
                    }

                    if (packet.getPlayer() != null) // we have to make sure it's not an unmodifiable packet
                        monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_SERVER_HANDLE);
                }
                lastHandledPacket = packet;
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
