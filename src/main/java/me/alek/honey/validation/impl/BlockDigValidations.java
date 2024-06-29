package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationServiceContext;
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

public class BlockDigValidations extends AbstractPacketListener<WrappedPlayInBlockDig> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInBlockDig>> getNewPipeline() {
        return ValidationPipeline.from(new BlockInteractE());
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.PLAYER_DIGGING);
    }

    public static class BlockInteractE implements PacketValidation<WrappedPlayInBlockDig>, ViolationValidation<WrappedPlayInBlockDig> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInBlockDig packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null || blockPosition.getY() == -1) {
                    return ValidationServiceContext.PASS;
                }

                return BlockPlaceValidations.checkExceedingMaxDistance(player, ViolationKey.of(this), blockPosition, getSetting().max, context);
            }

            return ValidationServiceContext.PASS;
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
