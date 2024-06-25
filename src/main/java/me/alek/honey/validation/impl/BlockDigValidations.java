package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.response.ResponseContainer;
import me.alek.honey.validation.response.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInBlockDig;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BlockDigValidations extends AbstractPacketListener<WrappedPlayInBlockDig> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInBlockDig>> getNewPipeline() {
        return ValidationPipeline.from(new BlockInteractE());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Play.Client.BLOCK_DIG);
    }

    public static class BlockInteractE implements PacketValidation<WrappedPlayInBlockDig>, ViolationValidation<WrappedPlayInBlockDig> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInBlockDig packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null || blockPosition.getY() == -1) return Validation.PASS;

                return BlockPlaceValidations.checkExceedingMaxDistance(player, blockPosition, getSetting().max, container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractE.class);
        }
    }
}
