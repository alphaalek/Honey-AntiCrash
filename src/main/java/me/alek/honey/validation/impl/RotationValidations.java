package me.alek.honey.validation.impl;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInFlying;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import me.alek.honey.validation.*;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;
import org.bukkit.Location;

import java.util.*;

public class RotationValidations<WP extends WrappedPlayInFlying.Rotational<WP>> extends AbstractValidationPacketListener<WP> {

    private final Map<UUID, Float> lastYawMap = new HashMap<>();
    private final Map<UUID, Float> lastPitchMap = new HashMap<>();

    public RotationValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.FINAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Arrays.asList(PacketTypes.Play.Client.PLAYER_ROTATION, PacketTypes.Play.Client.PLAYER_POS_ROT);
    }

    @Override
    public synchronized boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context) {
        float yaw = packet.getCurrentWrapped().getYaw();
        float pitch = packet.getCurrentWrapped().getPitch();

        float deltaYaw = LocationUtils.calculateRotationalDistance(yaw, this.lastYawMap.getOrDefault(player.uuid, yaw));
        float deltaPitch = LocationUtils.calculateRotationalDistance(pitch, this.lastPitchMap.getOrDefault(player.uuid, pitch));

        this.lastYawMap.put(player.uuid, yaw);
        this.lastPitchMap.put(player.uuid, pitch);

        context.getCallback().addAction(Callback.Action.<ViolationKey, ValidationViolationContext>from((keyOuter, responseCallback) -> {
            responseCallback.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                if (onViolation != null) onViolation.run();

                context.getDevelopmentData(keyInner).addPoint("Yaw" , yaw);
                context.getDevelopmentData(keyInner).addPoint("Pitch" , pitch);
                context.getDevelopmentData(keyInner).addPoint("Delta yaw" , deltaYaw);
                context.getDevelopmentData(keyInner).addPoint("Delta pitch" , deltaPitch);
            });
        }));

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new RotationA<>(), new RotationB<>(), new RotationC<>());
    }

    @Override
    public void remove(HoneyPlayer player) {
        this.lastYawMap.remove(player.uuid);
        this.lastPitchMap.remove(player.uuid);
    }

    public static class RotationA<WP extends WrappedPlayInFlying.Rotational<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            float pitch = packet.getPitch();
            if (getSetting().isEnabled() && (pitch < -90 || pitch > 90)) {
                return ValidationViolationContext.FLAG;
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

    public static class RotationB<WP extends WrappedPlayInFlying.Rotational<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static final HoneyKey yawKey = HoneyKey.of(0, "yaw");
        private static final HoneyKey pitchKey = HoneyKey.of(1, "pitch");

        private float lastYaw = -1;
        private float lastPitch = -1;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            float yaw = packet.getYaw();
            float lastYaw = this.lastYaw != -1 ? this.lastYaw : yaw;
            this.lastYaw = yaw;

            float pitch = packet.getPitch();
            float lastPitch = this.lastPitch != -1 ? this.lastPitch : pitch;
            this.lastPitch = pitch;

            if (yaw == 0 || MovementValidations.INSTANCE.isAnyLatelyTeleportationLocationsCloseTo(
                    player, getSetting().threshold,
                    (location) -> (double) LocationUtils.calculateRotationalDistance(location.getYaw(), yaw))) {
                return ValidationViolationContext.TERMINATE;
            }

            if (getSetting().isEnabled()) {
                float deltaYaw = LocationUtils.calculateRotationalDistance(yaw, lastYaw);
                float deltaPitch = LocationUtils.calculateRotationalDistance(pitch, lastPitch);

                return ValidationViolationContext.Builder.validate().add(
                        ValidationViolationContext.Builder.validate().flag(deltaYaw).withViolationKey(yawKey).build(),
                        ValidationViolationContext.Builder.validate().flag(deltaPitch).withViolationKey(pitchKey).build()
                ).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

    public static class RotationC<WP extends WrappedPlayInFlying.Rotational<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static final HoneyKey yawKey = HoneyKey.of(0, "yaw");
        private static final HoneyKey pitchKey = HoneyKey.of(1, "pitch");

        private float lastYaw = -1;
        private float lastPitch = -1;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            float yaw = packet.getYaw();
            float lastYaw = this.lastYaw != -1 ? this.lastYaw : yaw;
            this.lastYaw = yaw;

            float pitch = packet.getPitch();
            float lastPitch = this.lastPitch != -1 ? this.lastPitch : pitch;
            this.lastPitch = pitch;

            if (getSetting().isEnabled()) {
                float deltaYaw = LocationUtils.calculateRotationalDistance(yaw, lastYaw);
                float deltaPitch = LocationUtils.calculateRotationalDistance(pitch, lastPitch);

                return ValidationViolationContext.Builder.validate().add(
                        ValidationViolationContext.Builder.validate().flag(deltaYaw).withViolationKey(yawKey).build(),
                        ValidationViolationContext.Builder.validate().flag(deltaPitch).withViolationKey(pitchKey).build()
                ).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
