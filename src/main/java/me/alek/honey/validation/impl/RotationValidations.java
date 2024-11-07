package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.MovementPacketWrapper;
import me.alek.honey.validation.ViolationManagement;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import me.alek.honey.validation.*;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import org.bukkit.Location;

import java.util.*;

public class RotationValidations<WP extends MovementPacketWrapper<WP>> extends AbstractValidationPacketListener<WP> {

    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();

    public RotationValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Arrays.asList(PacketTypes.Play.Client.PLAYER_ROTATION, PacketTypes.Play.Client.PLAYER_POS_AND_ROT);
    }

    @Override
    public synchronized boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context, Callback callback) {
        Location location = packet.getAnyWrapped().getLocation();
        if (location.getPitch() == 0 && location.getYaw() == 0) {
            return false;
        }

        float lastYaw = getLastYawFor(player, location.getYaw());
        float lastPitch = getLastPitchFor(player, location.getPitch());

        float deltaYaw = LocationUtils.calculateYawDistance(location.getYaw(), lastYaw);
        float deltaPitch = LocationUtils.calculateYawDistance(location.getPitch(), lastPitch);

        lastYawMap.put(player.uuid, location.getYaw());
        lastPitchMap.put(player.uuid, location.getPitch());

        if (location.getYaw() == lastYaw
                && location.getPitch() == lastPitch) {
            return false;
        }

        context.put(ValidationViolationContext.Builder.class,
                ValidationViolationContext.Builder.validate().flag(deltaYaw));
        context.put(ValidationViolationContext.Builder.class,
                ValidationViolationContext.Builder.validate().flag(deltaPitch));

        callback.addAction(Callback.Action.<ViolationKey, ValidationViolationContext>from((keyOuter, responseCallback) -> {
            responseCallback.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                if (onViolation != null) onViolation.run();

                context.getDevelopmentData(keyInner).addPoint("Yaw" , location.getYaw());
                context.getDevelopmentData(keyInner).addPoint("Pitch" , location.getPitch());
                context.getDevelopmentData(keyInner).addPoint("Delta yaw" , deltaYaw);
                context.getDevelopmentData(keyInner).addPoint("Delta pitch" , deltaPitch);
            });
        }));

        return true;
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new RotationA<>(), new RotationB<>(), new RotationC<>());
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

    public static class RotationA<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                float pitch = packet.getPitch();
                if (pitch < -90 || pitch > 90) {
                    return ValidationViolationContext.FLAG;
                }
            }

            return ValidationViolationContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationA.class);
        }
    }

    public static class RotationB<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationManagement.HoneyKey yawKey = ViolationManagement.HoneyKey.of(0, "yaw");
        private static final ViolationManagement.HoneyKey pitchKey = ViolationManagement.HoneyKey.of(1, "pitch");

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (packet.getYaw() == 0 || MovementValidations.isAnyLatelyTeleportationLocationsCloseTo(
                    player, 100,
                    (location) -> (double) LocationUtils.calculateYawDistance(location.getYaw(), packet.getYaw()))) {
                return ValidationViolationContext.PASS.copy().terminate().build();
            }

            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().add(
                        context.get(ValidationViolationContext.Builder.class, 0).withViolationKey(yawKey).build(),
                        context.get(ValidationViolationContext.Builder.class, 1).withViolationKey(pitchKey).build()
                ).build();
            }

            return ValidationViolationContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationB.class);
        }
    }

    public static class RotationC<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationManagement.HoneyKey yawKey = ViolationManagement.HoneyKey.of(0, "yaw");
        private static final ViolationManagement.HoneyKey pitchKey = ViolationManagement.HoneyKey.of(1, "pitch");

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                if (packet.getYaw() != 0) {
                    return ValidationViolationContext.Builder.validate().add(
                            context.get(ValidationViolationContext.Builder.class, 0).withViolationKey(yawKey).build(),
                            context.get(ValidationViolationContext.Builder.class, 1).withViolationKey(pitchKey).build()
                    ).build();
                }
            }

            return ValidationViolationContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Rotation.RotationC.class);
        }
    }
}
