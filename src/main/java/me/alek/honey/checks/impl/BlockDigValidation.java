package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInBlockDig;
import me.alek.honey.util.CallbackAction;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BlockDigValidation extends AbstractPacketListener<WrappedPlayInBlockDig> implements IPacketListener {

    @Override
    public CheckVisitor<WrappedPlayInBlockDig> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedPlayInBlockDig> visitor = new CheckVisitor<>(player);
        visitor.visit(new BlockInteractE());
        return visitor;
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInBlockDig> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInBlockDig> packetContainer, CheckContextContainer container) {

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
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInBlockDig packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null || blockPosition.getY() == -1) return Validation.PASS;

                return BlockPlaceValidation.checkExceedingMaxDistance(player, blockPosition, getSetting().max, container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return 4;
        }

        @Override
        public String getName() {
            return "BlockInteract.E";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.block.e");
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.BLOCK_DIG_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractE.class);
        }
    }
}
