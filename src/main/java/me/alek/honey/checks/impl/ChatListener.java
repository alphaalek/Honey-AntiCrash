package me.alek.honey.checks.impl;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ChatListener extends AbstractPacketListener<WrappedPlayInChat> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInChat> getVisitor() {
        CheckVisitor<WrappedPlayInChat> visitor = new CheckVisitor<>();
        visitor.visit(new ChatA());
        visitor.visit(new ChatB());
        visitor.visit(new CommandsA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

    public static class ChatA implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInChat packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String message = packet.getMessage();
                if (message == null || message.isEmpty()) {
                    return ResponseBuilder.inst().cancel().flag().terminate().build();
                }
                for (char character : message.toCharArray()) {
                    if (character == Character.MIN_VALUE) {
                        return ResponseBuilder.inst().cancel().flag().terminate().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Chat.A";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Chat.ChatA.class);
        }
    }

    public static class ChatB implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

        private final boolean below_1_11 = Protocol.getProtocol().isOlderThan(Protocol.v1_11);

        @Override
        public ResponseContainer check(Player player, WrappedPlayInChat packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                int maxLength = below_1_11 ? 100 : 256;
                if (packet.getMessage().length() > maxLength) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Chat.B";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Chat.ChatB.class);
        }
    }

    private static String getLabel(String command) {
        String label = command.split( " ")[0];
        boolean isCommandFormat = label.startsWith("/");
        return isCommandFormat ? getNameFormat(label) : label;
    }

    private static String getNameFormat(String command) {
        if (command.startsWith("/")) {
            return command.substring(1);
        }
        return command;
    }

    public static String[] getStatisfiedList(List<String> commands) {
        IStructureModifier<CommandMap> craftServerModifier = ReflectStructureCache.acquireStructure(NMSUtils.getCraftServerClass())
                .withType(CommandMap.class)
                .withTarget(Bukkit.getServer());
        final CommandMap commandMap = craftServerModifier.readField(0);
        final List<String> knownCommands = new ArrayList<>();

        for (String command : commands) {
            String label = getLabel(command);
            String nameFormat = getNameFormat(command);

            for (String fallbackPrefix : ConfigurationOptions.Commands.FALLBACK_PREFIXES) {
                if (commandMap.getCommand(fallbackPrefix + ":" + label) != null) {
                    knownCommands.add(fallbackPrefix + ":" + nameFormat);
                }
            }
            if (commandMap.getCommand(label) != null) {
                knownCommands.add(nameFormat);
            }
        }
        String[] prohibitedCommands = new String[knownCommands.size()];
        int index = 0;
        for (String command : knownCommands) {
            prohibitedCommands[index] = command;
            index++;
        }
        return prohibitedCommands;
    }

    public static boolean match(String commandCheck, String commandSent) {
        String[] args = commandSent.split(" ");
        if (args.length == 1) {
            return commandCheck.equalsIgnoreCase(getNameFormat(commandSent));
        }
        return getNameFormat(commandSent).startsWith(commandCheck);
    }

    public static class CommandsA implements ViolationCheck, PacketCheck<WrappedPlayInChat> {

        private final String[] prohibitedCommands = getStatisfiedList(ConfigurationOptions.Commands.PROHIBITED_COMMANDS);

        @Override
        public ResponseContainer check(Player player, WrappedPlayInChat packet, CheckContextContainer container) {

            String message = packet.getMessage();
            if (!message.startsWith("/")) {
                return ResponseBuilder.SUCCESS;
            }
            for (String prohibitedCommand : this.prohibitedCommands) {

                if (match(prohibitedCommand, message)) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Command.A";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Commands.class);
        }
    }

}
