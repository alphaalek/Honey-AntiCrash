package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationViolationContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInBlockDig;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;

public class BlockDigValidations extends AbstractValidationPacketListener<WrappedPlayInBlockDig> {

    public BlockDigValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.PLAYER_DIGGING);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInBlockDig>> createValidationPipeline() {
        return ValidationPipeline.from(new BlockInteractC());
    }

    public static class BlockInteractC implements PacketValidation<WrappedPlayInBlockDig>, ViolationValidation<WrappedPlayInBlockDig> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInBlockDig packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null || blockPosition.getY() == -1) {
                    return ValidationViolationContext.PASS;
                }

                return BlockPlaceValidations.checkExceedingMaxDistance(player, ViolationKey.of(this), blockPosition, getSetting().max, context);
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "BlockInteract.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.block.c");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractC.class);
        }
    }
}
