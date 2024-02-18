package me.alek.honey.checks.impl;

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
import me.alek.honey.netty.packet.PacketBundleFilter;
import me.alek.honey.netty.packet.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Supplier;

public class RotationListener<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();

    private static final ViolationService.CheckKey yawKey = new ViolationService.CheckKey("yaw");
    private static final ViolationService.CheckKey pitchKey = new ViolationService.CheckKey("pitch");

    @Override
    public synchronized boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        Location location = packetContainer.getPacket().getLocation();

        if (location.getPitch() == 0 && location.getYaw() == 0) {
            lastYawMap.put(player.getUniqueId(), 0f);
            lastPitchMap.put(player.getUniqueId(), 0f);
            return false;
        }
        float deltaYaw = Locations.calculateYawDistance(location.getYaw(), getLastYawFor(player, location.getYaw()));
        float deltaPitch = Locations.calculateYawDistance(location.getPitch(), getLastPitchFor(player, location.getPitch()));

        ResponseContainer response = ResponseBuilder.inst().add(
                ResponseBuilder.inst().flag(deltaYaw).withViolationKey(yawKey).build(),
                ResponseBuilder.inst().flag(deltaPitch).withViolationKey(pitchKey).build()
        ).build();
        container.getStorage().put(ResponseContainer.class, response);

        lastYawMap.put(player.getUniqueId(), location.getYaw());
        lastPitchMap.put(player.getUniqueId(), location.getPitch());

        container.getDefaultDevelopmentData().addPoint("Delta yaw" , deltaYaw);
        container.getDefaultDevelopmentData().addPoint("Delta pitch" , deltaPitch);
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new RotationA<>());
        visitor.visit(new RotationB<>());
        visitor.visit(new RotationC<>());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public void cleanup(Player player) {
        lastYawMap.remove(player.getUniqueId());
        lastPitchMap.remove(player.getUniqueId());
    }

    public static float getLastYawFor(Player player, float yaw) {
        return lastYawMap.getOrDefault(player.getUniqueId(), yaw);
    }

    public static float getLastPitchFor(Player player, float pitch) {
        return lastPitchMap.getOrDefault(player.getUniqueId(), pitch);
    }

    public static class RotationA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final float pitch = packet.getPitch();

                if (pitch < -90 || pitch > 90) {
                    return ResponseBuilder.inst().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Rotation.A";
        }

        @Override
        public String getDescription() {
            return "Illegal pitch rotation";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.SHORT_ROTATION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationA.class);
        }
    }

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (packet.getYaw() == 0 || PositionListener.isAnyLatelyTeleportationLocationsCloseTo(
                    player, 100,
                    (location) -> (double) Locations.calculateYawDistance(location.getYaw(), packet.getYaw()))) {
                return ResponseBuilder.SUCCESS.builder().terminate().build();
            }
            if (getSetting().isEnabled()) {
                return container.getStorage().get(ResponseContainer.class, 0);
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Rotation.B";
        }

        @Override
        public String getDescription() {
            return "Too high delta rotation";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.SHORT_ROTATION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationB.class);
        }
    }

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (packet.getYaw() != 0) {
                    return container.getStorage().get(ResponseContainer.class, 0);
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Rotation.C";
        }

        @Override
        public String getDescription() {
            return "Buffer of rotation exceeded";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LONG_ROTATION_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationC.class);
        }
    }
}
