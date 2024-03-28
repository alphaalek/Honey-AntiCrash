package me.alek.honey.tracker.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketEventloopStateTimings;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Map;

public class PacketTaskHandler {

    public static final double TO_MILLIS_MULTIPLIER = PacketEventloopStateTimings.NANOS_TO_MILLIS_MULTIPLIER;
    public static final Object ignoreDecrementLock = new Object();
    private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadTracker();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    private static PacketTaskFuture<?> handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new Thread(() -> {
            while (HoneyPlugin.get().isRunning()) {
                synchronized (ignoreDecrementLock) {
                    if (handledTask != null && ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                            && (System.nanoTime() - handledTask.getSubmitted()) * TO_MILLIS_MULTIPLIER >= ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_THREAD_DUMP) {

                        PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket();
                        if (packet == null || packet.isDumped()) continue;

                        packet.dump();
                        if (packet.getType() != PacketType.Play.Client.FLYING
                                && packet.getType() != PacketType.Play.Client.POSITION
                                && packet.getType() != PacketType.Play.Client.POSITION_LOOK
                                && packet.getType() != PacketType.Play.Client.GROUND) {
                            dumpThread(packet.getPlayer(), packet);
                        }
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "honey-packet-task-handler").start();
    }

    public static PacketTaskFuture<?> getHandledTask() {
        return handledTask;
    }

    public static PacketContainer<? extends WrappedPacket<?>> getLastHandledPacket() {
        return lastHandledPacket;
    }

    public static void dumpThread(HoneyPlayer player, PacketContainer<? extends WrappedPacket<?>> packet) {
        HoneyPlugin.get().getNotificationService().notify(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX
                + " §cThe server seems to be frozen after a packet was sent by " + player.name + ". See the log for thread dump and more info."));
        HoneyPlugin.get().getLoggerService().getLogger().severe("Auto thread dump for packet sent by " + player.name + ":");

        if (packet != null) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Packet: " + packet.getType() + ", Id: " + packet.getType().getPacketId() + ", State: " + packet.getState());

            Map<String, Object> fields = packet.getAll().readFieldEntries();
            fields.replaceAll((k, v) -> (v instanceof Object[]) ? Arrays.toString((Object[]) v) : v);

            HoneyPlugin.get().getLoggerService().getLogger().severe("Properties: " + fields);

        } else HoneyPlugin.get().getLoggerService().getLogger().severe("Packet data unknown.");

        HoneyPlugin.get().getLoggerService().getLogger().severe("");

        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(HoneyPlugin.get().getServerThread().getId(), 50);
        StackTraceElement[] elements = info.getStackTrace();
        String prevElement = null;
        int combinedTotal = 0;
        StringBuilder builder = new StringBuilder("Thread Dump:\n\t");
        for (StackTraceElement element : elements) {
            boolean notMatch = false;
            if (!element.toString().equals(prevElement)) {
                builder.append(element + "\n\t");
                notMatch = true;
            }
            else {
                combinedTotal++;
            }
            prevElement = element.toString();

            if (notMatch) {
                if (combinedTotal > 0) {
                    builder.append("[...] x" + combinedTotal + "\n\t");
                }
                combinedTotal = 0;
            }
        }
        if (prevElement != null)
            builder.append(prevElement);

        HoneyPlugin.get().getLoggerService().getLogger().severe(builder.toString());
    }

    public static <V> void registerTask(PacketTaskFuture<V> task){
        lastHandledPacket = null;

        PacketContainer<? extends WrappedPacket<?>> packet = task.getPacket();
        packet.getEventloopStateTimings().startTimings(EventloopState.SERVER_HANDLE);

        handledTask = task;
    }

    public static PacketContainer<? extends WrappedPacket<?>> getCurrentHandledPacket() {
        if (handledTask == null) return null;

        return handledTask.getPacket();
    }

    public static void unregisterTask() {
        synchronized (ignoreDecrementLock) {
            if (handledTask != null) {
                tracker.decrementPacketsInQueueExecution(handledTask.getPlayer().uuid);

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
}
