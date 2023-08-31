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
import me.alek.honey.netty.injector.PacketHandleTimeTracker;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Supplier;

public class NettyListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final DecimalFormat formatter = new DecimalFormat("#.####");

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol());
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
        NettyI.packetDecoderMap.remove(player.getUniqueId());
    }

    public static class NettyA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                PacketHandleTimeTracker packetHandleTimeTracker = HoneyPlugin.get().getPacketHandleTimeTracker();

                // only use the quotient tracker check if there is more than 10 different records being tracked, just to make
                // sure it's a more correct average being evaluated and compared in this check
                if (packetHandleTimeTracker.getTrackSize() > 10) {

                    double timeToAverageQuotient = HoneyPlugin.get().getPacketHandleTimeTracker().getTimeToAverageQuotient(player);
                    if (timeToAverageQuotient > ConfigurationOptions.Netty.NettyA.MAX_QUOTIENT) {
                        return ResponseBuilder.inst().flag().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.A";
        }

        @Override
        public String getDescription() {
            return "Too high netty thread quotient";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyA.class);
        }
    }

    public static class NettyB<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

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
        public String getViolationName() {
            return "Netty.B";
        }

        @Override
        public String getDescription() {
            return "Sending packet without being online";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyB.class);
        }
    }


    public static class NettyC<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements PacketCheck<WP>, IPacketListener, ViolationCheck {

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

                for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(Protocol.getProtocol(), PacketBound.CLIENT)) {
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
        public String getViolationName() {
            return "Netty.C";
        }

        @Override
        public String getDescription() {
            return "Structure with illegal attributes";
        }

        @Override
        public ListenerPriority getPriority() {
            return ListenerPriority.HIGHEST;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, InboundPacketDecoder> packetDecoderMap = new HashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            double decodeTime = 0;

            if (player != null) {

                InboundPacketDecoder packetDecoder = packetDecoderMap.computeIfAbsent(player.getUniqueId(), (d) -> (InboundPacketDecoder) PacketLibrary.get().getInjector().getDecoder(NMSUtils.getChannel(player)));
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
                container.getDevelopmentData(this).addPoint("Decode Time", formatter.format(decodeTime / 1_000) + " ms");
                return ResponseBuilder.inst().flag(decodeTime).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.I";
        }

        @Override
        public String getDescription() {
            return "Too long decode time";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyI.class);
        }
    }

    public static class NettyJ<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double decodeTime = container.getStorage().getDouble(0);
                if (decodeTime > 0) {

                    container.getDevelopmentData(this).addPoint("Decode Time", formatter.format(decodeTime / 1_000) + " ms");
                    return ResponseBuilder.inst().flag(decodeTime).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.J";
        }

        @Override
        public String getDescription() {
            return "Buffer of decode time exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyJ.class);
        }
    }
}
