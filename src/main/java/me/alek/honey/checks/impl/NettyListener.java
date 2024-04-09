package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketEventloopStateTimings;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInLook;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPosition;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPositionLook;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.*;
import me.alek.honey.tracker.packet.WrappedPacketTaskFuture;
import me.alek.honey.tracker.packet.PacketTaskHandler;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.NumberConversions;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiPredicate;
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
    public CheckVisitor<WP> getVisitor(HoneyPlayer player) {
        CheckVisitor<WP> visitor = new CheckVisitor<>(player);
        // visitor.visit(new NettyA<>());
        // visitor.visit(new NettyC<>());
        visitor.visit(new NettyH<>());
        visitor.visit(new NettyI<>());
        // visitor.visit(new NettyJ<>());
        return visitor;
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

    }

    public static class NettyA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadTracker();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                if (tracker.getPacketsInQueueExecution(player.uuid) > ConfigurationOptions.Netty.NettyA.MAX_PACKETS
                        && !tracker.ignoredPacketsInQueueExecutionSaver.hasID(packet.getType().getPacketId())) {
                    return ResponseBuilder.inst().flag(1).build();
                }

                WrappedPacketTaskFuture<?> task = PacketTaskHandler.getHandledTask();
                if (task != null && task.getPlayer().equals(player)
                        && (System.nanoTime() - task.getSubmitted()) * PacketTaskHandler.TO_MILLIS_MULTIPLIER > ConfigurationOptions.Netty.NettyA.MAX_HANDLE
                        && !tracker.ignoredPacketsInQueueExecutionSaver.hasID(task.getPacket().getType().getPacketId())) {
                    return ResponseBuilder.inst().flag(1).build();
                }
            }

            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 26;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyA.class);
        }
    }


    public static class NettyB<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements PacketCheck<WP>, IPacketListener, ViolationCheck {


        private static final List<PacketTypeEnum> IGNORE = Arrays.asList(PacketType.Play.Client.FLYING);
        private static final Array<PacketReader<? extends WrappedPacket<?>>> READERS = new Array<>();

        static {
            READERS.set(PacketType.Play.Client.POSITION.getPacketId(), new PacketReader<WrappedPlayInPosition>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInPosition> packetContainer) {
                    IStructureModifier<Double> doubles = packetContainer.getDoubles();
                    return new Object[]{doubles.readField(0), doubles.readField(1), doubles.readField(2)};
                }
            });
            READERS.set(PacketType.Play.Client.POSITION_LOOK.getPacketId(), new PacketReader<WrappedPlayInPositionLook>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInPositionLook> packetContainer) {
                    IStructureModifier<Double> doubles = packetContainer.getDoubles();
                    IStructureModifier<Float> floats = packetContainer.getFloats();
                    return new Object[]{doubles.readField(0), doubles.readField(1), doubles.readField(2), floats.readField(0), floats.readField(1)};
                }
            });
            READERS.set(PacketType.Play.Client.LOOK.getPacketId(), new PacketReader<WrappedPlayInLook>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInLook> packetContainer) {
                    IStructureModifier<Float> floats = packetContainer.getFloats();
                    return new Object[]{floats.readField(0), floats.readField(1)};
                }
            });
        }

        @Override
        public CheckVisitor<WP> getVisitor(HoneyPlayer player) {
            CheckVisitor<WP> visitor = new CheckVisitor<>(player);
            visitor.visit(this);
            return visitor;
        }

        @Override
        public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
            return true;
        }

        @Override
        public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

        }

        @Override
        public Supplier<List<PacketTypeEnum>> getPacketTypes() {
            return () -> {
                final List<PacketTypeEnum> packetTypes = new ArrayList<>();

                BiPredicate<ReflectStructure<?, ?>, Class<?>> structureClass = (structure, clazz) ->
                        structure.withType(clazz).getAccessors().stream().anyMatch(accessor -> !Modifier.isStatic(accessor.getField().getModifiers()));
                for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketBound.CLIENT)) {
                    ReflectStructure<Object, ?> structure = ReflectStructureCache.acquireStructure(packetType);

                    if (structureClass.test(structure, double.class) || structureClass.test(structure, float.class)) {
                        packetTypes.add(packetType);
                    }
                }

                packetTypes.removeAll(IGNORE);

                return packetTypes;
            };
        }

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                PacketReader<WP> reader = (PacketReader<WP>) READERS.get(packet.getType().getPacketId());
                if (reader != null) {
                    Object[] values = reader.read(packet.getContainer());

                    for (Object value : values) {
                        if (value instanceof Float && NumberConversions.isInvalid((float) value)) {
                            return ResponseBuilder.inst().cancel().flag().build();
                        }
                        if (value instanceof Double && NumberConversions.isInfinite((double) value)) {
                            return ResponseBuilder.inst().cancel().flag().build();
                        }
                    }
                    return ResponseBuilder.SUCCESS;
                }

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
        public int getID() {
            return 27;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyB.class);
        }
    }

    public static class NettyC<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getServerThreadTracker();

        private static final double IGNORE_RATE_5S = /* 7.5 */ 50 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private static final double RATE_5S = /* 85*/ 250 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private static final double RATE_10S = /* 62.5 */ 200 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private static final double RATE_ACCEL_5S = /* 32.5 */ 125 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private static final double QUOTIENT_ACCEL_5S = /* 12.5 */ 85;
        private static final double RATE_ACCEL = /*115 */ 350 * Timespan.MILLIS_TO_NANO_MULTIPLIER;

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double playerRate5S = tracker.getPlayerRate(player, Timespan.FIVE_SECOND, false);
                if (playerRate5S < IGNORE_RATE_5S || (playerRate5S = tracker.getPlayerRate(player, Timespan.FIVE_SECOND, true)) < IGNORE_RATE_5S) {
                    return ResponseBuilder.SUCCESS;
                }

                if (playerRate5S > RATE_5S
                        || tracker.getPlayerRate(player, Timespan.TEN_SECOND, true) > RATE_10S) {
                    return ResponseBuilder.inst().flag(1).onViolation(onViolation -> {
                        if (onViolation != null) onViolation.run();

                        container.getDevelopmentData(this).addPoint("Rate (5s)",
                                toMillis(tracker.getPlayerRate(player, Timespan.FIVE_SECOND, true)));
                        container.getDevelopmentData(this).addPoint("Rate (10s)",
                                toMillis(tracker.getPlayerRate(player, Timespan.TEN_SECOND, true)));
                    }).build();
                }

                if (playerRate5S > RATE_ACCEL_5S
                        && tracker.hasCollected(player, Timespan.FIVE_SECOND)
                        && tracker.getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false, true) > QUOTIENT_ACCEL_5S) {
                    return ResponseBuilder.inst().flag(1).onViolation(onViolation -> {
                        if (onViolation != null) onViolation.run();

                        container.getDevelopmentData(this).addPoint("Accel quotient (5s)",
                                NumberConversions.format(tracker.getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false, true) * 100, 2) + "%");
                        container.getDevelopmentData(this).addPoint("Avg. accel quotient (5s)",
                                NumberConversions.format(tracker.getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, true, true) * 100, 2) + "%");
                    }).build();
                }

                if (tracker.getPlayerRateAcceleration(player, false, false) > RATE_ACCEL) {
                    return ResponseBuilder.inst().flag(1).onViolation(onViolation -> {
                        if (onViolation != null) onViolation.run();

                        container.getDevelopmentData(this).addPoint("Accel",
                                toMillis(tracker.getPlayerRateAcceleration(player, false, true)));
                        container.getDevelopmentData(this).addPoint("Avg. accel",
                                toMillis(tracker.getPlayerRateAcceleration(player, true, true)));
                    }).build();
                }

                return ResponseBuilder.inst().flag(0).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        private double toMillis(double amount) {
            return amount * PacketEventloopStateTimings.NANOS_TO_MILLIS_MULTIPLIER;
        }

        @Override
        public int getID() {
            return 28;
        }

        @Override
        public String getName() {
            return "Netty.C (experimental)";
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyH<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        public static <WP extends WrappedPacket<WP>> ResponseContainer check0(WP packet, ConfigurationOptions.VlSetting setting, Supplier<CheckContextContainer.DevelopmentData> developmentDataSupplier) throws Exception {
            // evaluate the duration of decoding this packet
            double decodeTime = packet.getContainer().getEventloopStateTimings().getTimeForMillis(EventloopState.SERVER_DECODE);

            if (setting.isEnabled() && decodeTime > 0) {
                return ResponseBuilder.inst().flag(decodeTime)
                        .onViolation(onViolation -> {
                            if (onViolation != null) onViolation.run();

                            developmentDataSupplier.get().addPoint("Decode Time", NumberConversions.format(decodeTime) + " ms");
                        })
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            return check0(packet, getSetting(), () -> container.getDevelopmentData(this));
        }

        @Override
        public int getID() {
            return 33;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyH.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            return NettyH.check0(packet, getSetting(), () -> container.getDevelopmentData(this));
        }

        @Override
        public int getID() {
            return 34;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyI.class);
        }
    }

    public static class NettyJ<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getServerThreadTracker();
        private static final double[] timespanMultipliers = new double[]{1, 0.85, 0.75, 0.65};
        private static final double MIN_HSQ_ENTER = 0.1;

        private final ViolationService.HoneyKey HSQKey = ViolationService.HoneyKey.of(0, "hsq");
        private final ViolationService.HoneyKey TAKey = ViolationService.HoneyKey.of(1,"ta");
        private final ViolationService.HoneyKey TTKey = ViolationService.HoneyKey.of(2,"tt");
        private final ResponseContainer success = ResponseBuilder.inst().add(
                ResponseBuilder.inst().flag(0).withViolationKey(HSQKey).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(TAKey).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(TTKey).build()
        ).build();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double preHsq = tracker.getHalfServerThreadQuotient(player, Timespan.FIVE_SECOND, false);
                if (preHsq > MIN_HSQ_ENTER) {
                    int trackerSize = tracker.getTrackerSize();
                    int i = 0;
                    for (Timespan timespan : Timespan.Used.USED_FOR_TRACKERS) {
                        double multiplier = (i >= timespanMultipliers.length)
                                ? timespanMultipliers[timespanMultipliers.length - 1]
                                : timespanMultipliers[i++];

                        double hsq = tracker.getHalfServerThreadQuotient(player, timespan, true);
                        if ((trackerSize > 25 && hsq > 0.4 * multiplier) || (trackerSize > 10 && hsq > 0.5 * multiplier) || hsq > 0.55 * multiplier) {
                            return ResponseBuilder.inst().cancel().withViolationKey(HSQKey).flag(1)
                                    .onViolation(onViolation -> {
                                        if (onViolation != null) onViolation.run();

                                        applyCommonDevelopmentData(container.getDevelopmentData(this), multiplier, hsq);
                                    })
                                    .build();
                        }

                        double ta = tracker.getTimeToAverageWithoutQuotient(player, timespan, true);
                        if ((trackerSize > 15 && ta > 15 * multiplier) || (trackerSize > 25 && ta > 12.5 * multiplier) || (trackerSize > 40 && ta > 10 * multiplier)) {
                            return ResponseBuilder.inst().cancel().withViolationKey(TAKey).flag(1)
                                    .onViolation(onViolation -> {
                                        if (onViolation != null) onViolation.run();

                                        applyCommonDevelopmentData(container.getDevelopmentData(this), multiplier, hsq);
                                        container.getDevelopmentData(this).addPoint("t.a.",
                                                NumberConversions.format(
                                                        tracker.getTimeToAverageWithoutQuotient(player, timespan, true) * 100, 2) + "%");
                                    })
                                    .build();
                        }

                        double tt = tracker.getTimeToTotalQuotient(player, timespan, true);
                        if ((trackerSize > 15 && tt > 0.6 * multiplier) || (trackerSize > 25 && tt > 0.5 * multiplier)) {
                            return ResponseBuilder.inst().cancel().withViolationKey(TTKey).flag(1)
                                    .onViolation(onViolation -> {
                                        if (onViolation != null) onViolation.run();

                                        applyCommonDevelopmentData(container.getDevelopmentData(this), multiplier, hsq);
                                        container.getDevelopmentData(this).addPoint("t.t.",
                                                NumberConversions.format(
                                                        tracker.getTimeToTotalQuotient(player, timespan, true) * 100, 2) + "%");
                                    })
                                    .build();
                        }
                    }
                }
                return success;
            }
            return ResponseBuilder.SUCCESS;
        }

        private void applyCommonDevelopmentData(CheckContextContainer.DevelopmentData developmentData, double multiplier, double hsq) {
            developmentData.addPoint("Multiplier", multiplier);
            developmentData.addPoint("h.s.q.", NumberConversions.format(hsq * 100, 2) + "%");
        }

        @Override
        public int getID() {
            return 35;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyJ.class);
        }
    }
}
