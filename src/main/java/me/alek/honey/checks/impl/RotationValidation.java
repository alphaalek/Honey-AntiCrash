package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LocationUtils;
import org.bukkit.Location;

import java.util.*;
import java.util.function.Supplier;

public class RotationValidation<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();

    @Override
    public synchronized boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
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

        container.getStorage().put(Validation.class, Validation.inst().flag(deltaYaw));
        container.getStorage().put(Validation.class, Validation.inst().flag(deltaPitch));

        callback.addAction(responseCallback -> {
            responseCallback.builder().onViolation(onViolation -> {
                if (onViolation != null) onViolation.run();

                container.getDefaultDevelopmentData().addPoint("Yaw" , location.getYaw());
                container.getDefaultDevelopmentData().addPoint("Pitch" , location.getPitch());
                container.getDefaultDevelopmentData().addPoint("Delta yaw" , deltaYaw);
                container.getDefaultDevelopmentData().addPoint("Delta pitch" , deltaPitch);
            });
        });
        return true;
    }

    @Override
    public CheckVisitor<WP> getVisitor(HoneyPlayer player) {
        return CheckVisitor.from(player,
                new RotationA<>(), new RotationB<>(), new RotationC<>());
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

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final float pitch = packet.getPitch();

                if (pitch < -90 || pitch > 90) {
                    return Validation.inst().flag().build();
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

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationService.HoneyKey yawKey = ViolationService.HoneyKey.of(0, "yaw");
        private static final ViolationService.HoneyKey pitchKey = ViolationService.HoneyKey.of(1, "pitch");

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (packet.getYaw() == 0 || MovementValidation.isAnyLatelyTeleportationLocationsCloseTo(
                    player, 100,
                    (location) -> (double) LocationUtils.calculateYawDistance(location.getYaw(), packet.getYaw()))) {
                return Validation.PASS.copy().terminate().build();
            }
            if (getSetting().isEnabled()) {
                return Validation.inst().add(
                        container.getStorage().get(Validation.class, 0).withViolationKey(yawKey).build(),
                        container.getStorage().get(Validation.class, 1).withViolationKey(pitchKey).build()
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

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final ViolationService.HoneyKey yawKey = ViolationService.HoneyKey.of(0, "yaw");
        private static final ViolationService.HoneyKey pitchKey = ViolationService.HoneyKey.of(1, "pitch");

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (packet.getYaw() != 0) {
                    return Validation.inst().add(
                            container.getStorage().get(Validation.class, 0).withViolationKey(yawKey).build(),
                            container.getStorage().get(Validation.class, 1).withViolationKey(pitchKey).build()
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
