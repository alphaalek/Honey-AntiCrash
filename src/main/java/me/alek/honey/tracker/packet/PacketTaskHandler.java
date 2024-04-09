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
import me.alek.honey.services.LoggerService;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.*;

public class PacketTaskHandler {

    public static final double TO_MILLIS_MULTIPLIER = PacketEventloopStateTimings.NANOS_TO_MILLIS_MULTIPLIER;
    public static final Object ignoreDecrementLock = new Object();
    private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadTracker();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    private static WrappedPacketTaskFuture<?> handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new Thread(() -> {
            while (HoneyPlugin.get().isRunning()) {
                synchronized (ignoreDecrementLock) {
                    if (handledTask != null && ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                            && (System.nanoTime() - handledTask.getSubmitted()) * TO_MILLIS_MULTIPLIER >= ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_THREAD_DUMP) {

                        PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket();
                        if (packet == null || packet.isDumped()) {
                            continue;
                        }
                        packet.dump();

                        if (packet.getType() != PacketType.Play.Client.FLYING
                                && packet.getType() != PacketType.Play.Client.POSITION
                                && packet.getType() != PacketType.Play.Client.POSITION_LOOK
                                && packet.getType() != PacketType.Play.Client.GROUND
                                && packet.getType() != PacketType.Play.Client.VEHICLE_MOVE) {
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

    public static WrappedPacketTaskFuture<?> getHandledTask() {
        return handledTask;
    }

    public static PacketContainer<? extends WrappedPacket<?>> getLastHandledPacket() {
        return lastHandledPacket;
    }

    private static Object[] reverse(Object[] array) {
        Object[] newArray = new Object[array.length];
        int j = 0;
        for (int i = newArray.length - 1; i >= 0; i--) {
            newArray[j++] = array[i];
        }
        return newArray;
    }

    private static <T> T[] castArray(Object[] array, T[] in) {
        for (int i = 0; i < array.length; i++) {
            in[i] = (T) array[i];
        }
        return in;
    }

    private static String getThreadDump() {
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(HoneyPlugin.get().getServerThread().getId(), Integer.MAX_VALUE);
        StackTraceElement[] elements = info.getStackTrace();

        String prevElement = null;
        int combined = 0;
        List<String> strings = new ArrayList<>();
        for (StackTraceElement element : castArray(reverse(elements), new StackTraceElement[elements.length])) {
            boolean notMatch = false;
            if (!element.toString().equals(prevElement)) {
                strings.add(element + "\n\t");
                notMatch = true;
            }
            else combined++;

            prevElement = element.toString();

            if (notMatch) {
                if (combined > 0) strings.add("[...] x" + combined + "\n\t");

                combined = 0;
            }
        }

        if (prevElement != null) {
            strings.add(prevElement);
        }

        while (strings.size() > 50) {
            strings.remove(strings.size() - 1);
        }
        Collections.reverse(strings);

        StringBuilder builder = new StringBuilder("Thread Dump:\n\t");
        for (String str : strings) {
            builder.append(str);
        }
        return builder.toString();
    }

    public static void dumpThread(HoneyPlayer player, PacketContainer<? extends WrappedPacket<?>> packet) {
        String dump = getThreadDump();
        LoggerService loggerService = HoneyPlugin.get().getLoggerService();

        if (!dump.contains("PacketTaskHandler.unregisterTask")) {
            if (packet != null && !PacketType.isUnknown(packet.getType())) {
                loggerService.getLogger().severe("Packet: " + packet.getType() + ", Id: " + packet.getType().getPacketId() + ", State: " + packet.getState());
                loggerService.getLogger().severe("Properties: " + HoneyPlugin.get().getLoggerService().getPacketEntries(packet));
            } else {
                loggerService.getLogger().severe("Packet data unknown.");
            }
            loggerService.getLogger().severe(dump);

            HoneyPlugin.get().getNotificationService().notify(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX
                    + " §cThe server seems to be frozen after a packet was sent by " + player.name + ". See the log for thread dump and more info."));
        }
    }

    public static <V> void registerTask(WrappedPacketTaskFuture<V> task){
        lastHandledPacket = null;

        PacketContainer<? extends WrappedPacket<?>> packet = task.getPacket();
        packet.getEventloopStateTimings().startTimings(EventloopState.SERVER_HANDLE);

        handledTask = task;
    }

    public static PacketContainer<? extends WrappedPacket<?>> getCurrentHandledPacket() {
        if (handledTask == null) return null;

        return handledTask.getPacket();
    }

    public static void unregisterTask(PacketContainer<? extends WrappedPacket<?>> packet) {
        synchronized (ignoreDecrementLock) {
            tracker.decrementPacketsInQueueExecution(packet.getPlayer().uuid);
        }

        packet.getEventloopStateTimings().stopTimings();

        if (ConfigurationOptions.PacketDump.HEAVY_PACKET_DUMP_ENABLED
                && packet.getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_HANDLE) > ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_PACKET_DUMP) {
            HoneyPlugin.get().getLoggerService().dumpAutoHeavyPacket(packet);
        }

        // we have to make sure it's not an unmodifiable packet (made for some packet types in higher versions where packet mapping is not yet made)
        if (packet.getPlayer() != null) {
            monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_SERVER_HANDLE);
        }

        lastHandledPacket = packet;
        handledTask = null;
    }
}
