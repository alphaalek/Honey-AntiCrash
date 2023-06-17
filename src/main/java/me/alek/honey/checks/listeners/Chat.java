package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.helper.NBTHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Chat extends AbstractPacketListener<WrappedPlayInChat> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInChat> getVisitor() {
        CheckVisitor<WrappedPlayInChat> visitor = new CheckVisitor<>();
        visitor.visit(new ChatA());
        visitor.visit(new CommandsA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

    public static class ChatA implements PacketCheck<WrappedPlayInChat> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInChat packet, CheckContextContainer container) {
            String message = packet.getMessage();
            if (message.startsWith("/")) {
                return ResponseBuilder.SUCCESS;
            }
            if (NBTHelper.checkBadCharacter(message)) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }
    }

    public static class CommandsA implements IntervalCheck, PacketCheck<WrappedPlayInChat> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInChat packet, CheckContextContainer container) {
            String message = packet.getMessage();
            if (!message.startsWith("/")) {
                return ResponseBuilder.SUCCESS;
            }
            String label = message.split(" ")[0].substring(1);
            if (((CraftServer) Bukkit.getServer()).getCommandMap().getCommand(label) == null) {
                return ResponseBuilder.SUCCESS;
            }
            for (String prohibitedCommand : ConfigurationOptions.Commands.PROHIBITED_COMMANDS) {

                if (message.equals(prohibitedCommand)) {
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
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Commands.get();
        }
    }

}
