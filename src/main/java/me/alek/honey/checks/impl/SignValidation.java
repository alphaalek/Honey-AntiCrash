package me.alek.honey.checks.impl;

import com.google.gson.JsonObject;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInUpdateSign;
import me.alek.honey.util.IDRegistry;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class SignValidation extends AbstractPacketListener<WrappedPlayInUpdateSign> implements IPacketListener {

    @Override
    public CheckVisitor<WrappedPlayInUpdateSign> getVisitor(HoneyPlayer player) {
        return CheckVisitor.from(player, new SignA());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Play.Client.UPDATE_SIGN);
    }

    public static class SignA implements PacketCheck<WrappedPlayInUpdateSign> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\\\"translation.test.invalid\\\"}",
                "{translate:\\\"translation.test.invalid2\\\"}"
        };

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInUpdateSign packet, CheckContextContainer container) {
            List<JsonObject> legacyChatComponentPages = packet.getLegacyJsonPages();
            for (JsonObject page : legacyChatComponentPages) {
                if (!page.has("text")) {
                    return Validation.inst().cancel().punish().build();
                }
            }

            for (String line : packet.getLines()) {
                if (line.length() > 47) {
                    return Validation.inst().cancel().punish().build();
                }

                for (String string : ILLEGAL_COMPONENT_STRINGS) {
                    if (line.toLowerCase().replace("\"", "").trim().contains(string)) {
                        return Validation.inst().cancel().punish().build();
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
