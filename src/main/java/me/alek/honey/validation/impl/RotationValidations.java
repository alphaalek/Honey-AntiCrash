package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.response.ResponseContainer;
import me.alek.honey.validation.response.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LocationUtils;
import org.bukkit.Location;

import java.util.*;
import java.util.function.Supplier;

public class RotationValidations<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> {

    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();

    @Override
    public synchronized boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, ValidationContextContainer container, CallbackAction<ResponseContainer> callback) {
        Location location = packetContainer.getPacket().getLocation();
        if (location.getPitch() == 0 && location.getYaw() == 0) {
            return false;
        }

        float lastYaw = getLastYawFor(player, location.getYaw());
        float lastPitch = getLastPitchFor(player, location.getPitch());

        float deltaYaw = LocationUtils.calculateYawDistance(location.getYaw(), lastYaw);
        float deltaPitch = LocationUtils.calculateYawDistance(location.getPitch(), lastPitch);

        lastYawMap.put(player.uuid, location.getYaw());
        lastPitchMap.put(player.uuid, location.getPitch());

        if (location.getYaw() == lastYaw && location.getPitch() == lastPitch) return false;

        container.put(Validation.class, Validation.validate().flag(deltaYaw));
        container.put(Validation.class, Validation.validate().flag(deltaPitch));

        callback.addAction(responseCallback -> {
            responseCallback.builder().onViolation((check, onViolation) -> {
                if (onViolation != null) onViolation.run();

                container.getDevelopmentData(check).addPoint("Yaw" , location.getYaw());
                container.getDevelopmentData(check).addPoint("Pitch" , location.getPitch());
                container.getDevelopmentData(check).addPoint("Delta yaw" , deltaYaw);
                container.getDevelopmentData(check).addPoint("Delta pitch" , deltaPitch);
            });
        });
        return true;
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> getNewPipeline() {
        return ValidationPipeline.from(new RotationA<>(), new RotationB<>(), new RotationC<>());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketTypes.Play.Client.LOOK,
                PacketTypes.Play.Client.POSITION_LOOK
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

    public static class RotationA<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {
                final float pitch = packet.getPitch();

                if (pitch < -90 || pitch > 90) {
                    return Validation.validate().flag().build();
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
            return "Rotation.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.rotation.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationA.class);
        }
    }

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationService.HoneyKey yawKey = ViolationService.HoneyKey.of(0, "yaw");
        private static final ViolationService.HoneyKey pitchKey = ViolationService.HoneyKey.of(1, "pitch");

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, ValidationContextContainer container) {
            if (packet.getYaw() == 0 || MovementValidations.isAnyLatelyTeleportationLocationsCloseTo(
                    player, 100,
                    (location) -> (double) LocationUtils.calculateYawDistance(location.getYaw(), packet.getYaw()))) {
                return Validation.PASS.copy().terminate().build();
            }
            if (getSetting().isEnabled()) {
                return Validation.validate().add(
                        container.get(Validation.class, 0).withViolationKey(yawKey).build(),
                        container.get(Validation.class, 1).withViolationKey(pitchKey).build()
                ).build();
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Rotation.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.rotation.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationB.class);
        }
    }

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationService.HoneyKey yawKey = ViolationService.HoneyKey.of(0, "yaw");
        private static final ViolationService.HoneyKey pitchKey = ViolationService.HoneyKey.of(1, "pitch");

        @Override
        public ResponseContainer validate(HoneyPlayer player, WP packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                if (packet.getYaw() != 0) {
                    return Validation.validate().add(
                            container.get(Validation.class, 0).withViolationKey(yawKey).build(),
                            container.get(Validation.class, 1).withViolationKey(pitchKey).build()
                    ).build();
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
            return "Rotation.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.rotation.c");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationC.class);
        }
    }
}
