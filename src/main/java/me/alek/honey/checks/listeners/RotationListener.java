package me.alek.honey.checks.listeners;

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
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.utility.Locations;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class RotationListener<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return false;
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
    }

    public static class RotationA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                if (Locations.calculateYawDistance(player.getLocation().getYaw(), packet.getYaw()) > ConfigurationOptions.Rotation.RotationA.MAX) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Rotation.A";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationA.class);
        }
    }

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                float pitch = packet.getPitch();
                if (pitch < -90 || pitch > 90) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Rotation.B";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationB.class);
        }
    }

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (ConfigurationOptions.getVlSetting(ConfigurationOptions.Rotation.RotationC.class).isEnabled()) {

                if (packet.getYaw() > ConfigurationOptions.Rotation.RotationC.MAX && ConfigurationOptions.Rotation.RotationC.MAX > 360) {
                    packet.setYaw(packet.getYaw() % 360);
                }
                else if (packet.getYaw() < ConfigurationOptions.Rotation.RotationC.MIN && ConfigurationOptions.Rotation.RotationC.MIN < -360) {
                    packet.setYaw(360 - (Math.max(packet.getYaw(), packet.getYaw() * (-1)) % 360));
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Rotation.C";
        }
    }
}
