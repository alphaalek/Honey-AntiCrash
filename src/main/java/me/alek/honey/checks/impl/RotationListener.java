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
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.utility.Locations;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Supplier;

public class RotationListener<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    private static final Map<UUID, Float> lastPitchMap = new HashMap<>();

    @Override
    public synchronized boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        Location location = packetContainer.getPacket().getLocation();

        if (location.getPitch() == 0 && location.getYaw() == 0) {
            return false;
        }
        float lastYaw = Locations.calculateYawDistance(location.getYaw(), getLastYawFor(player));
        container.getStorage().putFloat(lastYaw);

        lastYawMap.put(player.getUniqueId(), location.getYaw());
        lastPitchMap.put(player.getUniqueId(), location.getPitch());
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

    public static Vector getLastDirectionVectorFor(Player player) {
        final Vector vector = new Vector();
        final double rotY = getLastPitchFor(player);

        vector.setY(-Math.sin(Math.toRadians(rotY)));

        final double xz = Math.cos(Math.toRadians(rotY));
        final double rotX = getLastYawFor(player);
        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));

        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

        return vector;
    }

    public static float getLastYawFor(Player player) {
        return lastYawMap.getOrDefault(player.getUniqueId(), 0f);
    }

    public static float getLastPitchFor(Player player) {
        return lastPitchMap.getOrDefault(player.getUniqueId(), 0f);
    }

    public static class RotationA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final float pitch = packet.getPitch();

                if (pitch < -90 || pitch > 90) {
                    container.getDevelopmentData(this).addPoint("Pitch", pitch);
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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationA.class);
        }
    }

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (PositionListener.isAnyLatelyTeleportationLocationsCloseTo(
                    player,
                    (location) -> (double) Locations.calculateYawDistance(location.getYaw(), packet.getYaw()),
                    15)) {
                return ResponseBuilder.SUCCESS.builder().terminate().build();
            }
            if (getSetting().isEnabled()) {
                final double deltaYaw = container.getStorage().getFloat(0);

                container.getDevelopmentData(this).addPoint("Delta Yaw", deltaYaw);
                return ResponseBuilder.inst().flag(deltaYaw).build();
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
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationB.class);
        }
    }

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final float deltaYaw = container.getStorage().getFloat(0);

                if (deltaYaw != 0) {
                    container.getDevelopmentData(this).addPoint("Delta Yaw", deltaYaw);
                    return ResponseBuilder.inst().flag(deltaYaw).build();
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
            return "Buffer of yaw rotation exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationC.class);
        }
    }
}
