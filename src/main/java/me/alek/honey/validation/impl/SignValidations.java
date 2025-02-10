package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInUpdateSign;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.ViolationValidation;

import java.util.Collections;
import java.util.List;

public class SignValidations extends AbstractValidationPacketListener<WrappedPlayInUpdateSign> {

    public SignValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.UPDATE_SIGN);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInUpdateSign>> createValidationPipeline() {
        return ValidationPipeline.from(new SignA());
    }

    public static class SignA implements PacketValidation<WrappedPlayInUpdateSign>, ViolationValidation<WrappedPlayInUpdateSign> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\\\"translation.test.invalid\\\"}",
                "{translate:\\\"translation.test.invalid2\\\"}"
        };

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInUpdateSign packet, ValidationPipelineContext context) {
            for (String line : packet.getText()) {
                for (String string : ILLEGAL_COMPONENT_STRINGS) {
                    if (line.toLowerCase().contains(string)) {
                        return ValidationViolationContext.FLAG;
                    }
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Sign.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.sign.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Sign.SignA.class);
        }
    }
}
