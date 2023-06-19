package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInTabComplete;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class TabComplete extends AbstractPacketListener<WrappedPlayInTabComplete> implements IPacketListener {


    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInTabComplete> packetContainer, CheckContextContainer container) {
        return packetContainer.getPacket().getMessage().startsWith("/");
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInTabComplete> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInTabComplete> getVisitor() {
        CheckVisitor<WrappedPlayInTabComplete> visitor = new CheckVisitor<>();
        visitor.visit(new TabA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.TAB_COMPLETE);
    }

    public static class TabA implements PacketCheck<WrappedPlayInTabComplete>, IntervalCheck {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInTabComplete packet, CheckContextContainer container) {
            String label = packet.getMessage().split(" ")[0];

            for (String prohibitedCommand : ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS) {
                if (label.equals(prohibitedCommand)) {
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .flag()
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "TabA";
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Commands.get();
        }
    }
}
