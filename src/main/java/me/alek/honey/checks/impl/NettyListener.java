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
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.InboundPacketDecoder;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.protocol.NetProtocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.StatisticHandleTracker;
import me.alek.honey.tracker.StatisticProvider;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NettyListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final DecimalFormat formatter = new DecimalFormat("#.####");

    private static final StatisticHandleTracker<Double, Player>[] trackers = (StatisticHandleTracker<Double, Player>[])
            new StatisticHandleTracker[]{
                    HoneyPlugin.get().getServerThreadHandleTracker(),
                    HoneyPlugin.get().getNettyPacketHandleTracker()
            };
    private static final String[] keys = new String[]{"stq", "ntq"};

    private static double[] getStatisticsAfterFirst(Player player, StatisticHandleTracker<Double, Player> statisticHandleTracker) {
        final StatisticProvider statisticProvider = statisticHandleTracker.getStatisticProvider(player);

        final double[] statistics = new double[statisticProvider.size() - 1];
        for (int i = 1; i < statistics.length + 1; i++) {
            statistics[i - 1] = statisticHandleTracker.getStatistic(player, i);

            if (statisticProvider.getTimeSinceLastUpdate(i) > statisticProvider.getUpdateTime(i) * 1_000L) {
                statistics[i - 1] = Math.max(statistics[i - 1], statisticProvider.getStatisticDirectly(i));
            }
        }
        return statistics;
    }

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
        visitor.visit(new NettyK<>());

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
        NettyJ.lastFlagMap.remove(player.getUniqueId());
    }

    public static class NettyA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (player == null || !player.isOnline()) {
                    return ResponseBuilder.inst().cancel().flag().terminate().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.A";
        }

        @Override
        public String getDescription() {
            return "Sending packet without being online";
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

                    if (structure.withType(double.class).getAccessors().size() != 0) {
                        packetTypes.add(packetType);
                        continue;
                    }
                    if (structure.withType(float.class).getAccessors().size() != 0) {
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
                        (d) -> (InboundPacketDecoder) PacketLibrary.get().getInjector().getDecoder(NMSUtils.getChannel(player)));
                if (packetDecoder == null) return ResponseBuilder.SUCCESS;

                // disable this check if the TPS is really low or ping of the player is too high
                if (HoneyPlugin.get().getRecentTPS() < ConfigurationOptions.General.Exempt.MIN_TPS
                        || HoneyPlugin.get().getRecentPing(player) > ConfigurationOptions.General.Exempt.MAX_PING)
                    return ResponseBuilder.SUCCESS.builder().terminate().build();

                // evaluate the duration of decoding this packet
                decodeTime = packetDecoder.getRecentDeltaDecode();
                container.getStorage().putDouble(decodeTime);
            }
            if (getSetting().isEnabled() && decodeTime > 0) {
                container.getDevelopmentData(this).addPoint("Decode Time", formatter.format(decodeTime) + " ms");
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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyH.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double decodeTime = container.getStorage().getDouble(0);
                if (decodeTime > 0) {

                    container.getDevelopmentData(this).addPoint("Decode Time", formatter.format(decodeTime) + " ms");
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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyI.class);
        }
    }

    public static class NettyJ<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Double> lastFlagMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled() && player != null) {

                // only use the quotient tracker check if there is more than 5 different records being tracked, just to make
                // sure it's a more correct average being evaluated and compared in this check
                if (trackers[0].getActiveTrackerSize() > 5) {

                    final ResponseBuilder builder = ResponseBuilder.inst();
                    int index = 0;
                    for (final StatisticHandleTracker<Double, Player> tracker : trackers) {

                        // ignore if the player has recently joined or the occupied space is lower than 5% of the total interval time
                        if (tracker.getTotal() / (((float)tracker.getUpdateInterval()) / 20 * 1000) < 0.05
                            || System.currentTimeMillis() - PlayerJoinListener.getInstantJoined(player) < 5000) {
                            continue;
                        }
                        final double timeToAverageQuotient = tracker.getStatistic(player, 0);

                        if (timeToAverageQuotient > getSetting().max) {
                            if (lastFlagMap.getOrDefault(player.getUniqueId(), -1d) == timeToAverageQuotient)
                                packet.getContainer().cancel();
                            else {
                                builder.add(ResponseBuilder.inst().flag()
                                        .withViolationKey(new ViolationService.CheckKey(keys[index]))
                                        .build());
                                lastFlagMap.put(player.getUniqueId(), timeToAverageQuotient);
                                break;
                            }
                        }
                        index++;
                    }
                    final ResponseContainer responseContainer = builder.build();
                    if (!responseContainer.SUB_CONTAINERS.isEmpty())
                        return responseContainer;
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.J (exp.)";
        }

        @Override
        public String getDescription() {
            return "Too high stq/ntq upper-end tracker stat";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyJ.class);
        }
    }

    public static class NettyK<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                // only use the quotient tracker check if there is more than 5 different records being tracked, just to make
                // sure it's a more correct average being evaluated and compared in this check
                if (trackers[0].getActiveTrackerSize() > 5) {

                    final ResponseBuilder builder = ResponseBuilder.inst();
                    int index = 0;
                    for (final StatisticHandleTracker<Double, Player> tracker : trackers) {

                        // ignore if the occupied space is lower than 5% of the total interval time
                        if (tracker.getTotal() / (((float)tracker.getUpdateInterval()) / 20 * 1000) < 0.05) {
                            continue;
                        }
                        if (tracker.getStatisticProvider(player).getTimeSinceLastUpdate(0) > 500) {
                            builder.add(
                                    ResponseBuilder.inst()
                                            .flag(tracker.getStatistic(player, 0))
                                            .withViolationKey(new ViolationService.CheckKey(keys[index++]))
                                            .build());
                        }
                    }
                    return builder.build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.K (exp.)";
        }

        @Override
        public String getDescription() {
            return "Interval throttle for ntq/stq tracker stat";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyK.class);
        }
    }
}
