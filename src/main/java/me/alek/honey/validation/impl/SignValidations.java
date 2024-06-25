package me.alek.honey.validation.impl;

import com.google.gson.JsonObject;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.AbstractPacketListener;
import me.alek.honey.validation.ValidationContextContainer;
import me.alek.honey.validation.ValidationPipeline;
import me.alek.honey.validation.ResponseContainer;
import me.alek.honey.validation.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInUpdateSign;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class SignValidations extends AbstractPacketListener<WrappedPlayInUpdateSign> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInUpdateSign>> getNewPipeline() {
        return ValidationPipeline.from(new SignA());
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.UPDATE_SIGN);
    }

    public static class SignA implements PacketValidation<WrappedPlayInUpdateSign> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\\\"translation.test.invalid\\\"}",
                "{translate:\\\"translation.test.invalid2\\\"}"
        };

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInUpdateSign packet, ValidationContextContainer container) {
            List<JsonObject> legacyChatComponentPages = packet.getLegacyJsonPages();
            for (JsonObject page : legacyChatComponentPages) {
                if (!page.has("text")) {
                    return Validation.validate().cancel().punish().build();
                }
            }

            for (String line : packet.getLines()) {
                if (line.length() > 47) {
                    return Validation.validate().cancel().punish().build();
                }

                for (String string : ILLEGAL_COMPONENT_STRINGS) {
                    if (line.toLowerCase().replace("\"", "").trim().contains(string)) {
                        return Validation.validate().cancel().punish().build();
                    }
                }
            }

            return Validation.PASS;
        }

        @Override
        public int getID() {
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
    }
}
