package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.monitor.MonitorSendAt;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketLifecycleTimings;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.HoneyLogger;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.statics.NumberUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskLifecycleHandler {

    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    private static WrappedPacketTask handledTask;
    private static PacketContainer<? extends WrappedPacket<?>> lastHandledPacket;

    static {
        new Thread(() -> {
            while (HoneyPlugin.get().isRunning()) {
                run();

                try {
                    Thread.sleep(5);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "honey-task-lifecycle-handler").start();
    }

    private static synchronized void run() {
        if (handledTask != null
                && !handledTask.isDone()
                && ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                && (System.nanoTime() - handledTask.getStarted()) * PacketLifecycleTimings.NANOS_TO_MILLIS_MULTIPLIER >= ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_THREAD_DUMP) {

            PacketContainer<? extends WrappedPacket<?>> packet = getCurrentHandledPacket();
            if (packet == null
                    || packet.isDumped()
                    || packet.getPlayer().bypassServerThreadTracking()) {
                return;
            }

            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13_1)
                    || packet.getType() != PacketTypes.Play.Client.PLAYER_MOVEMENT
                    && packet.getType() != PacketTypes.Play.Client.PLAYER_POSITION
                    && packet.getType() != PacketTypes.Play.Client.PLAYER_POS_ROT
                    && packet.getType() != PacketTypes.Play.Client.GROUND
                    && packet.getType() != PacketTypes.Play.Client.BOAT_MOVE
                    && packet.getType() != PacketTypes.Play.Client.VEHICLE_MOVE) {
                dump(packet);
            }
        }
    }

    public static synchronized WrappedPacketTask getHandledTask() {
        return handledTask;
    }

    public static synchronized PacketContainer<? extends WrappedPacket<?>> getCurrentHandledPacket() {
        return Optional.ofNullable(handledTask).map(WrappedPacketTask::getPacket).orElse(null);
    }

    public static PacketContainer<? extends WrappedPacket<?>> getLastHandledPacket() {
        return lastHandledPacket;
    }

    private static String formatThreadDump() {
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
                    strings.add("[...] recursive " + combined + "x\n\t");
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

    public static void dump(PacketContainer<? extends WrappedPacket<?>> packet) {
        String dump = formatThreadDump();
        HoneyLogger loggerService = HoneyPlugin.get().getLogger();

        if (!dump.contains("PacketTaskHandler")) {
            packet.dump();

            if (!PacketTypes.isUnknown(packet.getType())) {
                loggerService.getLogger().severe("Packet: " + packet.getType() + ", Id: " + packet.getType().getMinecraftID().get() + ", State: " + packet.getProtocolState());
                loggerService.getLogger().severe("Properties: " + HoneyPlugin.get().getLogger().formatPacketEntries(packet));
            } else {
                loggerService.getLogger().severe("Packet data unknown.");
            }
            loggerService.getLogger().severe("Sent by: " + packet.getPlayerName());

            loggerService.getLogger().severe(dump);

            HoneyPlugin.get().getNotificationService().notify(
                    HoneyPlugin.get().getNotificationService().getPrefix()
                    + " §cThe server seems to be frozen while processing packet "
                    + Optional.of(packet.getType()).orElse(PacketType.UNKNOWN)
                    + " sent by " + packet.getPlayerName()
                    + ". See the log for thread dump and more info."
            );
        }
    }

    public static synchronized void registerTask(WrappedPacketTask task){
        PacketContainer<? extends WrappedPacket<?>> packet = task.getPacket();
        packet.getEventloopStateTimings().startTimings(LifecycleState.SERVER_HANDLE);

        handledTask = task;
    }

    public static synchronized void unregisterTask(PacketContainer<? extends WrappedPacket<?>> packet) {
        packet.getEventloopStateTimings().stopTimings();

        if (!packet.getPlayer().bypassServerThreadTracking()) {
            double millis = packet.getEventloopStateTimings().getTimeForMillis(LifecycleState.SERVER_HANDLE);
            if (ConfigurationOptions.PacketDump.HEAVY_PACKET_DUMP_ENABLED
                    && millis > ConfigurationOptions.PacketDump.MIN_SERVER_HANDLE_PACKET_DUMP) {
                HoneyPlugin.get().getLogger().dumpAutoHeavyPacket(packet);
            }

            if (ConfigurationOptions.PacketDump.HEAVY_SERVER_THREAD_DUMP_ENABLED
                    && packet.isDumped()) {
                HoneyPlugin.get().getNotificationService().notify(HoneyPlugin.get().getNotificationService().getPrefix()
                        + " §cThe server (server thread) is done processing packet " + packet.getType() + " by " + packet.getPlayerName() + ". (Took " + NumberUtils.format(millis, 2) + "ms)");
            }
        }

        if (!PacketTypes.isUnknown(packet.getType())) {
            monitorManager.sendPacketInfo(packet, MonitorSendAt.AFTER_SERVER_HANDLE);
        }

        lastHandledPacket = packet;
        handledTask = null;
    }
}
