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
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class NettyListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {


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
        // all the other netty checks (C - H) are in the packet decoder
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
        NettyI.decodeStartMap.remove(player.getUniqueId());
    }

    @Override
    public void onPacketError(Player player, PacketTypeEnum packetType, PacketContainer<WP> packetContainer) {
        if (!Bukkit.getPluginManager().isPluginEnabled(HoneyPlugin.get())) {
            return;
        }
        if (ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyA.class).isEnabled()) {

            if (packetContainer != null) {
                packetContainer.cancel();
            }
            HoneyPlugin.get().getViolationService().punish(player, "Netty.A");
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
        public ListenerPriority getPriority() {
            return ListenerPriority.HIGHEST;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, LinkedBlockingQueue<Long>> decodeStartMap = new HashMap<>();
        private int recentTPS = 20;

        public NettyI() {

            new BukkitRunnable() {

                @Override
                public void run() {
                    recentTPS = HoneyPlugin.get().getNotificationService().getTPS();
                }
            }.runTaskTimerAsynchronously(HoneyPlugin.get(), 0L, 60L);

        }

        public static void registerDecodeFor(Player player) throws Exception {
            decodeStartMap.computeIfAbsent(player.getUniqueId(), (d) -> new LinkedBlockingQueue<>()).put(System.currentTimeMillis());
        }

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            long decodeTime = 0;

            if (player != null && decodeStartMap.containsKey(player.getUniqueId())) {

                // poll the unix time of the matching packet and evaluate the duration of decoding this packet
                Long decodeStart = decodeStartMap.computeIfAbsent(player.getUniqueId(), (d) -> new LinkedBlockingQueue<>()).poll();
                if (decodeStart == null)
                    return ResponseBuilder.SUCCESS.builder().skip(1).build();

                // disable this check if the TPS is really low
                if (recentTPS <= 15)
                    return ResponseBuilder.SUCCESS.builder().skip(1).build();

                // evaluate the duration of decoding this packet
                decodeTime = System.currentTimeMillis() - decodeStart;
                container.putLong(decodeTime);
            }
            if (getSetting().isEnabled() && decodeTime > 0) {
                if (decodeTime > ConfigurationOptions.Netty.NettyI.MAX) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.I";
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

                long decodeTime = container.getLong(0);
                if (decodeTime > 0) {

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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyJ.class);
        }
    }
}
