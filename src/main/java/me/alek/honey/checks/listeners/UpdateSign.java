package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUpdateSign;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class UpdateSign extends AbstractPacketListener<WrappedPlayInUpdateSign> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInUpdateSign> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {

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

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInUpdateSign packet, CheckContextContainer container) {
            for (String line : packet.getLines()) {
                if (line.length() > 47) {
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .punish()
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "SignA";
        }
    }

}
