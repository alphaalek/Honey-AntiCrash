package me.alek.honey.tracker.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import me.alek.honey.utility.Tuple2;
import org.bukkit.ChatColor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Map;

public class PacketTaskHandler {

    public static final Object lock = new Object();
    private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadHandleTracker();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    private static Tuple2<PacketTaskFuture<?>, Long> handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new Thread(() -> {
            while (HoneyPlugin.get().isRunning()) {
                synchronized (lock) {
                    if (handledTask != null && ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                            && System.currentTimeMillis() - handledTask.getT2() >= ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_THREAD_DUMP) {

                        PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket(false);
                        if (packet == null || packet.isDumped()) continue;

                        dumpThread(packet.getPlayer());
                        packet.dump();
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "Honey Packet Task Handler").start();
    }

    public static Tuple2<PacketTaskFuture<?>, Long> getHandledTask() {
        return handledTask;
    }

    public static PacketContainer<? extends WrappedPacket<?>> getLastHandledPacket() {
        return lastHandledPacket;
    }

    public static void dumpThread(HoneyPlayer player) {
        HoneyPlugin.get().getNotificationService().notify(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX
                + " §cThe server seems to be frozen after a packet was sent by " + player.name + ". See the log for thread dump and more info."));
        HoneyPlugin.get().getLoggerService().getLogger().severe("Auto thread dump for packet sent by " + player.name + ":");

        PacketContainer<? extends WrappedPacket<?>> packet = player.packetBundleSaver.getOldest(false);
        if (packet != null) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Packet: " + packet.getType() + ", Id: " + packet.getType().getPacketId() + ", State: " + packet.getState());

            Map<String, Object> fields = packet.getAll().readFieldEntries();
            fields.replaceAll((k, v) -> (v instanceof Object[]) ? Arrays.toString((Object[]) v) : v);

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

        PacketContainer<? extends WrappedPacket<?>> packet = task.getPlayer().packetBundleSaver.getOldest(false);
        if (packet != null)
            packet.getEventloopStateTimings().startTimings(EventloopState.SERVER_HANDLE);

        handledTask = new Tuple2<>(task, System.currentTimeMillis());
    }

    public static PacketContainer<? extends WrappedPacket<?>> getCurrentHandledPacket(boolean remove) {
        if (handledTask == null) return null;

        return handledTask.getT1().getPlayer().packetBundleSaver.getOldest(remove);
    }

    public static void unregisterTask() {
        synchronized (lock) {
            if (handledTask != null) {
                tracker.decrementPacketsInQueueExecution(handledTask.getT1().getPlayer().uuid);

                PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket(true);
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
}
