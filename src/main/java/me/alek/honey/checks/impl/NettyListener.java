package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.*;
import me.alek.honey.tracker.packet.PacketTaskFuture;
import me.alek.honey.tracker.packet.PacketTaskHandler;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.NumberConversions;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Supplier;

public class NettyListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketState.PLAY, PacketBound.CLIENT);
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.HIGHEST;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new NettyA<>());
        visitor.visit(new NettyB<>());
        visitor.visit(new NettyH<>());
        visitor.visit(new NettyI<>());
        visitor.visit(new NettyJ<>());

        return visitor;
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

    }

    public static class NettyA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadHandleTracker();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (tracker.getPacketsInQueueExecution(player.uuid) > 10 && !tracker.ignoredPacketsInQueueExecutionSaver.contains(packet.getType()))
                return ResponseBuilder.inst().flag(1).build();

            Tuple2<PacketTaskFuture<?>, Long> task = PacketTaskHandler.getHandledTask();
            if (task != null && task.getT1().getPlayer().equals(player) && System.currentTimeMillis() - task.getT2() > 25)
                return ResponseBuilder.inst().flag(1).build();

            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.A";
        }

        @Override
        public String getDescription() {
            return "Exceeds limit of packets in queue execution";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyA.class);
        }
    }


    public static class NettyB<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements PacketCheck<WP>, IPacketListener, ViolationCheck {

        @Override
        public CheckVisitor<WP> getVisitor() {
            CheckVisitor<WP> visitor = new CheckVisitor<>();
            visitor.visit(this);
            return visitor;
        }

        @Override
        public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
            return true;
        }

        @Override
        public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

        }

        @Override
        public Supplier<List<PacketTypeEnum>> getPacketTypes() {
            return () -> {
                final List<PacketTypeEnum> packetTypes = new ArrayList<>();

                for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketBound.CLIENT)) {
                    ReflectStructure<Object, ?> structure = ReflectStructureCache.acquireStructure(packetType);

                    if (!structure.withType(double.class).getAccessors().isEmpty()) {
                        packetTypes.add(packetType);
                        continue;
                    }
                    if (!structure.withType(float.class).getAccessors().isEmpty()) {
                        packetTypes.add(packetType);
                    }
                }
                return packetTypes;
            };
        }

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                for (double value : packet.getContainer().getDoubles().readFieldValues()) {
                    if (NumberConversions.isInvalid(value)) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                }
                for (float value : packet.getContainer().getFloats().readFieldValues()) {
                    if (NumberConversions.isInvalid(value)) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.B";
        }

        @Override
        public String getDescription() {
            return "Packet structure with illegal attributes";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createLenientFilter(1);
        }

        @Override
        public ListenerPriority getPriority() {
            return ListenerPriority.HIGHEST;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyB.class);
        }
    }

    public static class NettyH<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            // evaluate the duration of decoding this packet
            double decodeTime = packet.getContainer().getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_DECODE);

            if (getSetting().isEnabled() && decodeTime > 0) {
                container.getDevelopmentData(this).addPoint("Decode Time", NumberConversions.format(decodeTime) + " ms");
                return ResponseBuilder.inst().flag(decodeTime).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.H";
        }

        @Override
        public String getDescription() {
            return "Too long decode time";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_SINGLETON_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyH.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double decodeTime = packet.getContainer().getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_DECODE);
                if (decodeTime > 0) {

                    container.getDevelopmentData(this).addPoint("Decode Time", NumberConversions.format(decodeTime) + " ms");
                    return ResponseBuilder.inst().flag(decodeTime).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.I";
        }

        @Override
        public String getDescription() {
            return "Buffer overflow of decode time";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyI.class);
        }
    }

    public static class NettyJ<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getServerThreadHandleTracker();
        private final ResponseContainer success = ResponseBuilder.inst().add(
                ResponseBuilder.inst().flag(0).withViolationKey(new ViolationService.CheckKey("hsq")).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(new ViolationService.CheckKey("ta")).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(new ViolationService.CheckKey("tt")).build()
        ).build();
        private final double[] timespanMultipliers = new double[]{1, 0.85, 0.75, 0.65};

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double hsq = tracker.getHalfServerThreadQuotient(player, Timespan.TEN_SECOND);
                if (hsq > 0.25) {
                    int trackerSize = tracker.getTrackerSize();

                    int i = 0;
                    long time = System.currentTimeMillis();
                    for (Timespan timespan : Timespan.values()) {
                        tracker.clearExceedingNodes(player, timespan, time);

                        double multiplier = (i >= timespanMultipliers.length)
                                        ? timespanMultipliers[timespanMultipliers.length - 1]
                                        : timespanMultipliers[i++];

                        hsq = tracker.getHalfServerThreadQuotient(player, timespan);
                        if ((trackerSize > 25 && hsq > 0.45 * multiplier) || (trackerSize > 10 && hsq > 0.55 * multiplier) || hsq > 0.6 * multiplier) {
                            return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("hsq")).flag(1).build();
                        }

                        double ta = tracker.getTimeToAverageWithoutQuotient(player, timespan);
                        if ((trackerSize > 15 && ta > 15 * multiplier) || (trackerSize > 25 && ta > 12.5 * multiplier) || (trackerSize > 40 && ta > 10 * multiplier)) {
                            return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("ta")).flag(1).build();
                        }

                        double tt = tracker.getTimeToTotalQuotient(player, timespan);
                        if ((trackerSize > 15 && tt > 0.6 * multiplier) || (trackerSize > 25 && tt > 0.5 * multiplier)) {
                            return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("tt")).flag(1).build();
                        }
                    }
                }
                return success;
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.J";
        }

        @Override
        public String getDescription() {
            return "Too high usage of server thread";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyJ.class);
        }
    }
}
