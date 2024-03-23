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
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInLook;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPosition;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPositionLook;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.*;
import me.alek.honey.tracker.packet.PacketTaskFuture;
import me.alek.honey.tracker.packet.PacketTaskHandler;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.utility.Array;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;

import java.lang.reflect.Modifier;
import java.nio.file.FileVisitOption;
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
        visitor.visit(new NettyA<>());
        visitor.visit(new NettyC<>());
        visitor.visit(new NettyH<>());
        visitor.visit(new NettyI<>());
        visitor.visit(new NettyJ<>());

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

        private static final ServerThreadHandleTracker tracker = (ServerThreadHandleTracker) HoneyPlugin.get().getServerThreadHandleTracker();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (tracker.getPacketsInQueueExecution(player.uuid) > ConfigurationOptions.Netty.NettyA.MAX_PACKETS
                    && !tracker.ignoredPacketsInQueueExecutionSaver.contains(packet.getType()))
                return ResponseBuilder.inst().flag(1).build();

            PacketTaskFuture<?> task = PacketTaskHandler.getHandledTask();
            if (task != null && task.getPlayer().equals(player)
                    && (System.nanoTime() - task.getSubmitted()) * PacketTaskHandler.TO_MILLIS_MULTIPLIER > ConfigurationOptions.Netty.NettyA.MAX_HANDLE)
                return ResponseBuilder.inst().flag(1).build();

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

        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getServerThreadHandleTracker();

        private static final double RATE_5S = 100 * Timespan.MILLIS_TO_NANO_MULTIPLIER;
        private static final double RATE_15S = 70 * Timespan.MILLIS_TO_NANO_MULTIPLIER;

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {

            Bukkit.broadcastMessage(player.name + " (stq): rate: " + format(player, Timespan.FIVE_SECOND) + "ms/s, accel: "
                    + NumberConversions.format(tracker.getPlayerRate(player, Timespan.TWO_SECOND, true) * PacketEventloopStateTimings.TO_MILLIS_MULTIPLIER, 2) + "ms " + NumberConversions.format(tracker.getPlayerRateAccelerationQuotient(player, Timespan.FIVE_SECOND, false, true) * 100, 2) + "%");

            if (tracker.getPlayerRate(player, Timespan.FIVE_SECOND, true) > RATE_5S
                    || tracker.getPlayerRate(player, Timespan.FIFTEEN_SECOND, true) > RATE_15S) {
                return ResponseBuilder.inst().flag(1).build();
            }

            return ResponseBuilder.SUCCESS;
        }

        private double format(HoneyPlayer player, Timespan timespan) {
            return NumberConversions.format(
                    PacketEventloopStateTimings.TO_MILLIS_MULTIPLIER * tracker.getPlayerRate(player, timespan, true), 2);
        }

        @Override
        public int getID() {
            return 28;
        }

        @Override
        public String getName() {
            return "Netty.C";
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

        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getServerThreadHandleTracker();
        private static final double[] timespanMultipliers = new double[]{1, 0.85, 0.75, 0.65};
        private static final double MIN_HSQ_ENTER = 0.1;

        private final ViolationService.HoneyKey HSQKey = ViolationService.HoneyKey.of(this, "hsq");
        private final ViolationService.HoneyKey TAKey = ViolationService.HoneyKey.of(this,"ta");
        private final ViolationService.HoneyKey TTKey = ViolationService.HoneyKey.of(this,"tt");
        private final ResponseContainer success = ResponseBuilder.inst().add(
                ResponseBuilder.inst().flag(0).withViolationKey(HSQKey).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(TAKey).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(TTKey).build()
        ).build();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double hsq = tracker.getHalfServerThreadQuotient(player, Timespan.FIVE_SECOND, false);
                if (hsq > MIN_HSQ_ENTER) {
                    int trackerSize = tracker.getTrackerSize();
                    int i = 0;
                    long time = System.nanoTime();
                    for (Timespan timespan : Timespan.Used.USED) {
                        tracker.clearExceedingNodes(player, timespan, time);

                        double multiplier = (i >= timespanMultipliers.length)
                                        ? timespanMultipliers[timespanMultipliers.length - 1]
                                        : timespanMultipliers[i++];

                        hsq = tracker.getHalfServerThreadQuotient(player, timespan, true);
                        if ((trackerSize > 25 && hsq > 0.5 * multiplier) || (trackerSize > 10 && hsq > 0.55 * multiplier) || hsq > 0.6 * multiplier) {
                            return ResponseBuilder.inst().cancel().withViolationKey(HSQKey).flag(1).build();
                        }

                        double ta = tracker.getTimeToAverageWithoutQuotient(player, timespan, true);
                        if ((trackerSize > 15 && ta > 15 * multiplier) || (trackerSize > 25 && ta > 12.5 * multiplier) || (trackerSize > 40 && ta > 10 * multiplier)) {
                            return ResponseBuilder.inst().cancel().withViolationKey(TAKey).flag(1).build();
                        }

                        double tt = tracker.getTimeToTotalQuotient(player, timespan, true);
                        if ((trackerSize > 15 && tt > 0.6 * multiplier) || (trackerSize > 25 && tt > 0.5 * multiplier)) {
                            return ResponseBuilder.inst().cancel().withViolationKey(TTKey).flag(1).build();
                        }
                    }
                }
                return success;
            }
            return ResponseBuilder.SUCCESS;
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
