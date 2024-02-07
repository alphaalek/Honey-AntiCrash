package me.alek.honey.checks.impl;

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
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.InboundPacketDecoder;
import me.alek.honey.netty.packet.PacketBundleFilter;
import me.alek.honey.netty.packet.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.PacketBundleFilters;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
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
import me.alek.honey.tracker.task.PacketTaskFuture;
import me.alek.honey.tracker.task.PacketTaskHandler;
import me.alek.honey.utility.NumberConversions;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Supplier;

public class NettyListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, NetProtocol.getProtocol());
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
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public void cleanup(Player player) {
        NettyH.packetDecoderMap.remove(player.getUniqueId());
    }

    public static class NettyA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final PlayerServerThreadHandleTracker tracker = (PlayerServerThreadHandleTracker) HoneyPlugin.get().getServerThreadHandleTracker();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (tracker.getPacketsInQueueExecution(player.getUniqueId()) > 5)
                return ResponseBuilder.inst().flag(1).build();

            Tuple2<PacketTaskFuture<?>, Long> task = PacketTaskHandler.getHandledTask();
            if (task != null && task.getT1().getUUID().equals(player.getUniqueId()) && System.currentTimeMillis() - task.getT2() > 50)
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
            return PacketBundleFilterFactory.createEmptyFilter();
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
        public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
            return true;
        }

        @Override
        public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

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
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
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

        public static final Map<UUID, InboundPacketDecoder> packetDecoderMap = new HashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            double decodeTime = 0;

            if (player != null) {
                InboundPacketDecoder packetDecoder = packetDecoderMap.computeIfAbsent(player.getUniqueId(),
                        (d) -> (InboundPacketDecoder) PacketLibrary.get().getInjector().getDecoder(packet.getContainer().getChannel()));
                if (packetDecoder == null) return ResponseBuilder.SUCCESS;

                // disable this check if the TPS is really low or ping of the player is too high
                boolean exempt = HoneyPlugin.get().shouldExempt(player);
                container.getStorage().putBoolean(exempt);
                if (exempt)
                    return ResponseBuilder.SUCCESS;

                // evaluate the duration of decoding this packet
                decodeTime = packetDecoder.getRecentDeltaDecode();
                container.getStorage().putDouble(decodeTime);
            }
            if (getSetting().isEnabled() && decodeTime > 0) {
                container.getDevelopmentData(this).addPoint("Decode Time", NumberConversions.format(decodeTime) + " ms");
                final double decodeTimeFinal = decodeTime;
                return ResponseBuilder.inst().flag(decodeTime).onViolation(() -> HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(player, decodeTimeFinal)).build();
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
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            Boolean exempt = false;
            if (getSetting().isEnabled() && (exempt = container.getStorage().poll(boolean.class)) == null || !exempt) {

                double decodeTime = container.getStorage().getDouble(0);
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
        // using double instead of float, because otherwise development data would show other values after being formatted
        private final double[] timespanMultipliers = new double[]{1, 0.8f, 0.75f, 0.65f};

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double hsq = tracker.getHalfServerThreadQuotient(player, Timespan.FIVE_SECOND);
                if (hsq > 0.1) {
                    int trackerSize = tracker.getTrackerSize();
                    container.getDevelopmentData(this).addPoint("Tracker Size", trackerSize);
                    container.getDevelopmentData(this).addEmpty();
                    int developmentDataSize = container.getDevelopmentData(this).size();

                    int i = 0;
                    for (Timespan timespan : Timespan.values()) {
                        tracker.clearExceedingNodes(player, timespan);

                        double multiplier = (i >= timespanMultipliers.length)
                                        ? timespanMultipliers[timespanMultipliers.length - 1]
                                        : timespanMultipliers[i++];
                        container.getDevelopmentData(this).setPoint(developmentDataSize - 1, "Multiplier", multiplier);

                        hsq = tracker.getHalfServerThreadQuotient(player, timespan);
                        if ((trackerSize > 25 && hsq > 0.45 * multiplier) || (trackerSize > 10 && hsq > 0.55 * multiplier) || hsq > 0.65 * multiplier) {
                            container.getDevelopmentData(this).addPoint("h.s.q.", NumberConversions.format(hsq * 100) + "%");
                            return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("hsq")).flag(1).build();
                        }

                        double ta = tracker.getTimeToAverageWithoutQuotient(player, timespan);
                        if ((trackerSize > 15 && ta > 17.5 * multiplier) || (trackerSize > 25 && ta > 15 * multiplier) || (trackerSize > 40 && ta > 12.5 * multiplier)) {
                            container.getDevelopmentData(this).addPoint("t.a.", NumberConversions.format(ta * 100) + "%");
                            return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("ta")).flag(1).build();
                        }

                        double tt = tracker.getTimeToTotalQuotient(player, timespan);
                        if ((trackerSize > 15 && tt > 0.75 * multiplier) || (trackerSize > 25 && tt > 0.55 * multiplier)) {
                            container.getDevelopmentData(this).addPoint("t.t.", NumberConversions.format(tt * 100) + "%");
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
