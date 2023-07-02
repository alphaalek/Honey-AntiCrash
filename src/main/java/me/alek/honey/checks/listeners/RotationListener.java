package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
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

    public static class RotationA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (Locations.calculateYawDistance(player.getLocation().getYaw(), packet.getYaw()) > ConfigurationOptions.Position.Yaw.MAX_YAW_DISTANCE) {
                return ResponseBuilder.inst().cancel().punish().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Rotation.A";
        }
    }

    public static class RotationB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            float pitch = packet.getPitch();
            if (pitch < -90 || pitch > 90) {
                return ResponseBuilder.inst().cancel().punish().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Rotation.B";
        }
    }

    public static class RotationC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (packet.getYaw() > ConfigurationOptions.Position.Yaw.MAX_YAW && ConfigurationOptions.Position.Yaw.MAX_YAW > 360) {
                packet.setYaw(packet.getYaw() % 360);
            }
            else if (packet.getYaw() < ConfigurationOptions.Position.Yaw.MIN_YAW && ConfigurationOptions.Position.Yaw.MIN_YAW < -360) {
                packet.setYaw(360 - (Math.max(packet.getYaw(), packet.getYaw() * (-1)) % 360));
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Rotation.C";
        }
    }
}
