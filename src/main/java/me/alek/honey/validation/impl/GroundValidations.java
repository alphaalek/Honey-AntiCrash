package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInFlying;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.*;

import java.util.Arrays;
import java.util.List;

public class GroundValidations<WP extends WrappedPlayInFlying.Positional<WP>> extends AbstractValidationPacketListener<WP> {

    public GroundValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.FINAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Arrays.asList(
                PacketTypes.Play.Client.PLAYER_POSITION,
                PacketTypes.Play.Client.PLAYER_POS_ROT,
                PacketTypes.Play.Client.PLAYER_MOVEMENT
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new MovementA<>());
    }

    public static class MovementA<WP extends WrappedPlayInFlying.Positional<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private boolean wasOnGround;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                boolean wasOnGround = this.wasOnGround;
                this.wasOnGround = packet.isOnGround();
                if (wasOnGround != this.wasOnGround) {
                    return ValidationViolationContext.Builder.validate().terminate().flag().build();
                }
            }

            return ValidationViolationContext.Builder.validate().terminate().build();
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementA.class);
        }
    }
}
