package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUpdateSign;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class UpdateSignListener extends AbstractPacketListener<WrappedPlayInUpdateSign> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInUpdateSign> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInUpdateSign> getVisitor() {
        CheckVisitor<WrappedPlayInUpdateSign> visitor = new CheckVisitor<>();
        visitor.visit(new SignA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.UPDATE_SIGN);
    }

    public static class SignA implements PacketCheck<WrappedPlayInUpdateSign> {

        private final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\\\"translation.test.invalid\\\"}",
                "{translate:\\\"translation.test.invalid2\\\"}"
        };

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInUpdateSign packet, CheckContextContainer container) {
            for (String line : packet.getLines()) {
                if (line.length() > 47) {
                    return ResponseBuilder.inst().cancel().punish().build();
                }
                for (String string : ILLEGAL_COMPONENT_STRINGS) {
                    if (line.toLowerCase().replace("\"", "").trim().contains(string)) {
                        return ResponseBuilder.inst().cancel().punish().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Sign.A";
        }

        @Override
        public String getDescription() {
            return "Illegal sign text";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.UPDATE_SIGN_FILTER;
        }
    }
}
