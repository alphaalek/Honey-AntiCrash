package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Location;

import java.util.*;
import java.util.function.Supplier;

public class RotationListener<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();

    @Override
    public synchronized boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        Location location = packetContainer.getPacket().getLocation();

        if (location.getPitch() == 0 && location.getYaw() == 0) return false;

        float lastYaw = getLastYawFor(player, location.getYaw());
        float lastPitch = getLastPitchFor(player, location.getPitch());

        float deltaYaw = Locations.calculateYawDistance(location.getYaw(), lastYaw);
        float deltaPitch = Locations.calculateYawDistance(location.getPitch(), lastPitch);

        lastYawMap.put(player.uuid, location.getYaw());
        lastPitchMap.put(player.uuid, location.getPitch());

        if (location.getYaw() == lastYaw && location.getPitch() == lastPitch) return false;

        container.getStorage().put(ResponseBuilder.class, ResponseBuilder.inst().flag(deltaYaw));
        container.getStorage().put(ResponseBuilder.class, ResponseBuilder.inst().flag(deltaPitch));

        callback.addAction(responseCallback -> {
            responseCallback.builder().onViolation(onViolation -> {
                if (onViolation != null) onViolation.run();

                container.getDefaultDevelopmentData().addPoint("Delta yaw" , deltaYaw);
                container.getDefaultDevelopmentData().addPoint("Delta pitch" , deltaPitch);
            });
        });
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WP> getVisitor(HoneyPlayer player) {
        CheckVisitor<WP> visitor = new CheckVisitor<>(player);
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
    public void cleanup(HoneyPlayer player) {
        lastYawMap.remove(player.uuid);
        lastPitchMap.remove(player.uuid);
    }

    public static float getLastYawFor(HoneyPlayer player, float yaw) {
        return lastYawMap.getOrDefault(player.uuid, yaw);
    }

    public static float getLastPitchFor(HoneyPlayer player, float pitch) {
        return lastPitchMap.getOrDefault(player.uuid, pitch);
    }

    public static class RotationA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final float pitch = packet.getPitch();

                if (pitch < -90 || pitch > 90) {
                    return ResponseBuilder.inst().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 51;
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
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationA.class);
        }
    }

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private final ViolationService.CheckKey yawKey = new ViolationService.CheckKey(this, "yaw");
        private final ViolationService.CheckKey pitchKey = new ViolationService.CheckKey(this, "pitch");

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (packet.getYaw() == 0 || PositionListener.isAnyLatelyTeleportationLocationsCloseTo(
                    player, 100,
                    (location) -> (double) Locations.calculateYawDistance(location.getYaw(), packet.getYaw()))) {
                return ResponseBuilder.SUCCESS.builderCopy().terminate().build();
            }
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().add(
                        container.getStorage().get(ResponseBuilder.class, 0).withViolationKey(yawKey).build(),
                        container.getStorage().get(ResponseBuilder.class, 1).withViolationKey(pitchKey).build()
                ).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 52;
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
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationB.class);
        }
    }

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private final ViolationService.CheckKey yawKey = new ViolationService.CheckKey(this, "yaw");
        private final ViolationService.CheckKey pitchKey = new ViolationService.CheckKey(this, "pitch");

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (packet.getYaw() != 0) {
                    return ResponseBuilder.inst().add(
                            container.getStorage().get(ResponseBuilder.class, 0).withViolationKey(yawKey).build(),
                            container.getStorage().get(ResponseBuilder.class, 1).withViolationKey(pitchKey).build()
                    ).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 53;
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
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationC.class);
        }
    }
}
