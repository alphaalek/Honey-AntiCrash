package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketStateTimings;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.LoggerService;
import org.bukkit.ChatColor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;

public class PacketTaskHandler {

    public static final double TO_MILLIS_MULTIPLIER = PacketStateTimings.NANOS_TO_MILLIS_MULTIPLIER;
    private static final Object handleTaskLock = new Object();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    private static WrappedPacketTaskFuture handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new Thread(() -> {
            while (HoneyPlugin.get().isRunning()) {
                synchronized (handleTaskLock) {
                    if (handledTask != null && ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                            && (System.nanoTime() - handledTask.getStarted()) * TO_MILLIS_MULTIPLIER >= ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_THREAD_DUMP) {

                        PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket();
                        if (packet == null || packet.isDumped()) {
                            continue;
                        }
                        packet.dump();

                        if (packet.getType() != PacketTypes.Play.Client.FLYING
                                && packet.getType() != PacketTypes.Play.Client.POSITION
                                && packet.getType() != PacketTypes.Play.Client.POSITION_LOOK
                                && packet.getType() != PacketTypes.Play.Client.GROUND
                                && packet.getType() != PacketTypes.Play.Client.BOAT_MOVE
                                && packet.getType() != PacketTypes.Play.Client.VEHICLE_MOVE) {
                            dumpThread(packet);
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

    public static WrappedPacketTaskFuture getHandledTask() {
        synchronized (handleTaskLock) {
            return handledTask;
        }
    }

    public static PacketContainer<? extends WrappedPacket<?>> getLastHandledPacket() {
        synchronized (handleTaskLock) {
            return lastHandledPacket;
        }
    }

    private static String getThreadDump() {
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(HoneyPlugin.get().getServerThread().getId(), Integer.MAX_VALUE);
        StackTraceElement[] elements = info.getStackTrace();

        String prev = null;
        int combined = 0;
        List<String> strings = new ArrayList<>();

        // make the lines for this thread dump formatting
        for (StackTraceElement element : elements) {
            if (!element.toString().equals(prev)) {
                strings.add(element + "\n\t");
                if (combined > 0) {
                    strings.add("[...] x" + combined + "\n\t");
                }
                combined = 0;
            } else {
                combined++;
            }

            prev = element.toString();
        }

        if (prev != null && combined > 0) {
            strings.add(prev);
        }

        while (strings.size() > 50) {
            strings.remove(strings.size() - 1);
        }

        // finally make the string to be dumped
        StringBuilder builder = new StringBuilder("Thread Dump:\n\t");
        for (String str : strings) {
            builder.append(str);
        }
        return builder.toString();
    }

    public static void dumpThread(PacketContainer<? extends WrappedPacket<?>> packet) {
        String dump = getThreadDump();
        LoggerService loggerService = HoneyPlugin.get().getLoggerService();

        if (!dump.contains("PacketTaskHandler")) {
            String name = packet.getPlayerName();

            if (!PacketTypes.isUnknown(packet.getType())) {
                loggerService.getLogger().severe("Packet: " + packet.getType() + ", Id: " + packet.getType().getHoneyID() + ", State: " + packet.getProtocolState());
                loggerService.getLogger().severe("Sent by: " + name);
                loggerService.getLogger().severe("Properties: " + HoneyPlugin.get().getLoggerService().formatPacketEntries(packet));
            } else {
                loggerService.getLogger().severe("Packet data unknown.");
            }

            loggerService.getLogger().severe(dump);

            HoneyPlugin.get().getNotificationService().notify(HoneyPlugin.get().getNotificationService().getPrefix()
                    + " §cThe server seems to be frozen after packet " + packet.getType() + " was sent by " + name + ". See the log for thread dump and more info."));
        }
    }

    public static PacketContainer<? extends WrappedPacket<?>> getCurrentHandledPacket() {
        if (handledTask == null) {
            return null;
        }

        return handledTask.getPacket();
    }

    public static void registerTask(WrappedPacketTaskFuture task){
        PacketContainer<? extends WrappedPacket<?>> packet = task.getPacket();
        packet.getEventloopStateTimings().startTimings(EventloopState.SERVER_HANDLE);

        synchronized (handleTaskLock) {
            lastHandledPacket = null;
            handledTask = task;
        }
    }

    public static void unregisterTask(PacketContainer<? extends WrappedPacket<?>> packet) {
        packet.getEventloopStateTimings().stopTimings();

        double millis = packet.getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_HANDLE);
        if (ConfigurationOptions.PacketDump.HEAVY_PACKET_DUMP_ENABLED
                && millis > ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_PACKET_DUMP) {
            HoneyPlugin.get().getLoggerService().dumpAutoHeavyPacket(packet);
        }

        if (ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                && packet.isDumped()) {
            HoneyPlugin.get().getNotificationService().notify(HoneyPlugin.get().getNotificationService().getPrefix()
                    + " §cDone handling packet " + packet.getType() + " by " + packet.getPlayerName() + ". (Took " + millis + "ms)");
        }

        // we have to make sure it's not an unmapped packet
        if (!PacketTypes.isUnknown(packet.getType())) {
            monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_SERVER_HANDLE);
        }

        synchronized (handleTaskLock) {
            lastHandledPacket = packet;
            handledTask = null;
        }
    }
}
