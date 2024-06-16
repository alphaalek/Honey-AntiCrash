package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketStateTimings;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInLook;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPosition;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPositionLook;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tickloop.ExecutionQueueProxy;
import me.alek.honey.tickloop.packet.PacketTaskHandler;
import me.alek.honey.tickloop.packet.WrappedPacketTaskFuture;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.NumberUtils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class NettyValidation<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> PacketTypes.getTypesAvailableFor(PacketState.PLAY, PacketBound.SERVER);
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.HIGHEST;
    }

    @Override
    public CheckVisitor<WP> getVisitor(HoneyPlayer player) {
        return CheckVisitor.from(player,
                /* new NettyA<>(), */ new NettyB<>(), /*  new NettyC<>(), */ new NettyH<>(), new NettyI<>() /*, new NettyJ<>() */);
    }

    public static class NettyA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ExecutionQueueProxy tracker = HoneyPlugin.get().getServerThreadTracker();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                if (tracker.getPacketsInQueueExecution(player.uuid) > ConfigurationOptions.Netty.NettyA.MAX_PACKETS
                        && !tracker.ignoredPacketsInQueueExecutionSaver.hasID(packet.getType().getHoneyID())) {
                    return Validation.inst().flag(1).build();
                }

                WrappedPacketTaskFuture<?> task = PacketTaskHandler.getHandledTask();
                if (task != null && task.getPlayer().equals(player)
                        && (System.nanoTime() - task.getStarted()) * PacketTaskHandler.TO_MILLIS_MULTIPLIER > ConfigurationOptions.Netty.NettyA.MAX_HANDLE
                        && !tracker.ignoredPacketsInQueueExecutionSaver.hasID(task.getPacket().getType().getHoneyID())) {
                    return Validation.inst().flag(1).build();
                }
            }

            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyA.class);
        }
    }


    public static class NettyB<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements PacketCheck<WP>, IPacketListener, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final List<PacketType> IGNORE = Collections.singletonList(PacketTypes.Play.Client.FLYING);
        private static final Array<PacketReader<? extends WrappedPacket<?>>> READERS = new Array<>();

        static {
            READERS.set(PacketTypes.Play.Client.POSITION.getHoneyID(), new PacketReader<WrappedPlayInPosition>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInPosition> packetContainer) {
                    return new Object[]{
                            packetContainer.getPacket().getX(),
                            packetContainer.getPacket().getY(),
                            packetContainer.getPacket().getZ()};
                }
            });
            READERS.set(PacketTypes.Play.Client.POSITION_LOOK.getHoneyID(), new PacketReader<WrappedPlayInPositionLook>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInPositionLook> packetContainer) {
                    return new Object[]{
                            packetContainer.getPacket().getX(),
                            packetContainer.getPacket().getY(),
                            packetContainer.getPacket().getZ(),
                            packetContainer.getPacket().getYaw(),
                            packetContainer.getPacket().getPitch()};
                }
            });
            READERS.set(PacketTypes.Play.Client.LOOK.getHoneyID(), new PacketReader<WrappedPlayInLook>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInLook> packetContainer) {
                    return new Object[]{
                            packetContainer.getPacket().getYaw(),
                            packetContainer.getPacket().getPitch()};
                }
            });
        }

        @Override
        public CheckVisitor<WP> getVisitor(HoneyPlayer player) {
            return CheckVisitor.from(player, this);
        }

        @Override
        public Supplier<List<PacketType>> getPacketTypes() {
            return () -> {
                List<PacketType> packetTypes = new ArrayList<>();

                BiPredicate<ReflectStructure<?, ?>, Class<?>> structureClass = (structure, clazz) ->
                        !structure.withType(clazz).withoutStatic().getAccessors().isEmpty();
                for (PacketType packetType : PacketTypes.getTypesAvailableForBound(PacketBound.SERVER)) {
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

                PacketReader<WP> reader = (PacketReader<WP>) READERS.get(packet.getType().getHoneyID());
                if (reader != null) {
                    Object[] values = reader.read(packet.getContainer());
                    for (Object value : values) {
                        if (value instanceof Float && NumberUtils.isInvalid((float) value)) {
                            return Validation.inst().cancel().flag().build();
                        }
                        if (value instanceof Double && NumberUtils.isInfinite((double) value)) {
                            return Validation.inst().cancel().flag().build();
                        }
                    }

                    return Validation.PASS;
                }

                for (double value : packet.getContainer().getDoubles().readFieldValues()) {
                    if (NumberUtils.isInvalid(value)) {
                        return Validation.inst().cancel().flag().build();
                    }
                }
                for (float value : packet.getContainer().getFloats().readFieldValues()) {
                    if (NumberUtils.isInvalid(value)) {
                        return Validation.inst().cancel().flag().build();
                    }
                }
            }

            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.b");
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

        private static final int id = IDRegistry.CHECKS.acquire();
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
                    return Validation.PASS;
                }

                if (playerRate5S > RATE_5S
                        || tracker.getPlayerRate(player, Timespan.TEN_SECOND, true) > RATE_10S) {
                    return Validation.inst().flag(1).onViolation((check, onViolation) -> {
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
                    return Validation.inst().flag(1).onViolation((check, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        container.getDevelopmentData(this).addPoint("Accel quotient (5s)",
                                NumberUtils.format(tracker.getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false, true) * 100, 2) + "%");
                        container.getDevelopmentData(this).addPoint("Avg. accel quotient (5s)",
                                NumberUtils.format(tracker.getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, true, true) * 100, 2) + "%");
                    }).build();
                }

                if (tracker.getPlayerRateAcceleration(player, false, false) > RATE_ACCEL) {
                    return Validation.inst().flag(1).onViolation((check, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        container.getDevelopmentData(this).addPoint("Accel",
                                toMillis(tracker.getPlayerRateAcceleration(player, false, true)));
                        container.getDevelopmentData(this).addPoint("Avg. accel",
                                toMillis(tracker.getPlayerRateAcceleration(player, true, true)));
                    }).build();
                }

                return Validation.inst().flag(0).build();
            }
            return Validation.PASS;
        }

        private double toMillis(double amount) {
            return amount * PacketStateTimings.NANOS_TO_MILLIS_MULTIPLIER;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyH<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        public static <WP extends WrappedPacket<WP>> ResponseContainer check0(WP packet, ConfigurationOptions.VlSetting setting, Supplier<CheckContextContainer.DevelopmentData> developmentDataSupplier) throws Exception {
            // evaluate the duration of decoding this packet
            double decodeTime = packet.getContainer().getEventloopStateTimings().getTimeForMillis(EventloopState.DECODE);

            if (setting.isEnabled() && decodeTime > 0) {
                return Validation.inst().flag(decodeTime)
                        .onViolation((check, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            developmentDataSupplier.get().addPoint("Decode Time", NumberUtils.format(decodeTime) + " ms");
                        })
                        .build();
            }
            return Validation.PASS;
        }

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            return check0(packet, getSetting(), () -> container.getDevelopmentData(this));
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.H";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.h");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyH.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            return NettyH.check0(packet, getSetting(), () -> container.getDevelopmentData(this));
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.I";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.i");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyI.class);
        }
    }

    public static class NettyJ<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getServerThreadTracker();
        private static final double[] timespanMultipliers = new double[]{1, 0.85, 0.75, 0.65};
        private static final double MIN_HSQ_ENTER = 0.1;

        private final ViolationService.HoneyKey HSQKey = ViolationService.HoneyKey.of(0, "hsq");
        private final ViolationService.HoneyKey TAKey = ViolationService.HoneyKey.of(1,"ta");
        private final ViolationService.HoneyKey TTKey = ViolationService.HoneyKey.of(2,"tt");
        private final ResponseContainer success = Validation.inst().add(
                Validation.inst().flag(0).withViolationKey(HSQKey).build(),
                Validation.inst().flag(0).withViolationKey(TAKey).build(),
                Validation.inst().flag(0).withViolationKey(TTKey).build()
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
                            return Validation.inst().cancel().withViolationKey(HSQKey).flag(1)
                                    .onViolation((check, onViolation) -> {
                                        if (onViolation != null) onViolation.run();

                                        applyCommonDevelopmentData(container.getDevelopmentData(this), multiplier, hsq);
                                    })
                                    .build();
                        }

                        double ta = tracker.getTimeToAverageWithoutQuotient(player, timespan, true);
                        if ((trackerSize > 15 && ta > 15 * multiplier) || (trackerSize > 25 && ta > 12.5 * multiplier) || (trackerSize > 40 && ta > 10 * multiplier)) {
                            return Validation.inst().cancel().withViolationKey(TAKey).flag(1)
                                    .onViolation((check, onViolation) -> {
                                        if (onViolation != null) onViolation.run();

                                        applyCommonDevelopmentData(container.getDevelopmentData(this), multiplier, hsq);
                                        container.getDevelopmentData(this).addPoint("t.a.",
                                                NumberUtils.format(
                                                        tracker.getTimeToAverageWithoutQuotient(player, timespan, true) * 100, 2) + "%");
                                    })
                                    .build();
                        }

                        double tt = tracker.getTimeToTotalQuotient(player, timespan, true);
                        if ((trackerSize > 15 && tt > 0.6 * multiplier) || (trackerSize > 25 && tt > 0.5 * multiplier)) {
                            return Validation.inst().cancel().withViolationKey(TTKey).flag(1)
                                    .onViolation((check, onViolation) -> {
                                        if (onViolation != null) onViolation.run();

                                        applyCommonDevelopmentData(container.getDevelopmentData(this), multiplier, hsq);
                                        container.getDevelopmentData(this).addPoint("t.t.",
                                                NumberUtils.format(
                                                        tracker.getTimeToTotalQuotient(player, timespan, true) * 100, 2) + "%");
                                    })
                                    .build();
                        }
                    }
                }
                return success;
            }
            return Validation.PASS;
        }

        private void applyCommonDevelopmentData(CheckContextContainer.DevelopmentData developmentData, double multiplier, double hsq) {
            developmentData.addPoint("Multiplier", multiplier);
            developmentData.addPoint("h.s.q.", NumberUtils.format(hsq * 100, 2) + "%");
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyJ.class);
        }
    }
}
