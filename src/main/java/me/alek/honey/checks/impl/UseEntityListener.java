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
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUseEntity;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class UseEntityListener extends AbstractPacketListener<WrappedPlayInUseEntity> implements IPacketListener {

    @Override
    public CheckVisitor<WrappedPlayInUseEntity> getVisitor() {
        CheckVisitor<WrappedPlayInUseEntity> visitor = new CheckVisitor<>();
        visitor.visit(new UseEntityA());
        visitor.visit(new UseEntityB());
        return visitor;
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInUseEntity> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInUseEntity> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.USE_ENTITY);
    }

    public static class UseEntityA implements PacketCheck<WrappedPlayInUseEntity>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInUseEntity packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (packet.getAction() != WrappedPlayInUseEntity.EnumEntityUseAction.INTERACT_AT && packet.getTarget().isPresent()) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "UseEntity.A";
        }

        @Override
        public String getDescription() {
            return "Illegal action enum presence";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.UseEntity.UseEntityA.class);
        }
    }

    public static class UseEntityB implements PacketCheck<WrappedPlayInUseEntity>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInUseEntity packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                Optional<Vector> targetVectorOptional = packet.getTarget();
                if (targetVectorOptional.isPresent()) {

                    Vector targetVector = targetVectorOptional.get();
                    double x = targetVector.getX();
                    double y = targetVector.getY();
                    double z = targetVector.getZ();

                    CheckContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);

                    developmentData.addPoint("Vector X", x);
                    developmentData.addPoint("Vector Y", y);
                    developmentData.addPoint("Vector Z", z);

                    if (NumberConversions.isInvalid(new double[]{x, y, z})) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }

                    double maxX = ConfigurationOptions.UseEntity.UseEntityB.MAX_X / 2;
                    double maxY = ConfigurationOptions.UseEntity.UseEntityB.MAX_Y;
                    double maxZ = ConfigurationOptions.UseEntity.UseEntityB.MAX_Z / 2;

                    if (Math.abs(y) > maxY) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                    if (Math.abs(x) > maxX) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                    if (Math.abs(z) > maxZ) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "UseEntity.B";
        }

        @Override
        public String getDescription() {
            return "Illegal entity target vector";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.UseEntity.UseEntityB.class);
        }
    }
}
