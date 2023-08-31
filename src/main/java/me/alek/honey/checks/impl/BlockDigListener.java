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
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockDig;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BlockDigListener extends AbstractPacketListener<WrappedPlayInBlockDig> implements IPacketListener {

    @Override
    public CheckVisitor<WrappedPlayInBlockDig> getVisitor() {
        CheckVisitor<WrappedPlayInBlockDig> visitor = new CheckVisitor<>();
        visitor.visit(new BlockInteractE());
        return visitor;
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInBlockDig> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInBlockDig> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.BLOCK_DIG);
    }

    public static class BlockInteractE implements PacketCheck<WrappedPlayInBlockDig>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInBlockDig packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null || blockPosition.getY() == -1) return ResponseBuilder.SUCCESS;

                return BlockPlaceListener.checkExceedingMaxDistance(player, blockPosition, ConfigurationOptions.BlockInteract.BlockInteractE.MAX, container.getDevelopmentData(this));
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "BlockInteract.E";
        }

        @Override
        public String getDescription() {
            return "Interacting with block too far away";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractE.class);
        }
    }
}
