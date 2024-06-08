package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInChat;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class ChatValidation extends AbstractPacketListener<WrappedPlayInChat> implements IPacketListener {


    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInChat> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedPlayInChat> visitor = new CheckVisitor<>(player);
        visitor.visit(new ChatA());
        visitor.visit(new ChatB());
        visitor.visit(new CommandsA());
        return visitor;
    }

    @Override
    public void load() {
       CommandsA.prohibitedCommandsLiteral = getRegisteredCommands(ConfigurationOptions.Commands.PROHIBITED_COMMANDS_LITERAL);
       CommandsA.prohibitedCommandsRegex = ConfigurationOptions.Commands.PROHIBITED_COMMANDS_REGEX.toArray(new String[0]);
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

    public static class ChatA implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInChat packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                String message = packet.getMessage();
                if (message.isEmpty()) {
                    return Validation.inst().cancel().flag().terminate().build();
                }
                for (char character : message.toCharArray()) {
                    if (character == Character.MIN_VALUE) {
                        return Validation.inst().cancel().flag().terminate().build();
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
            return "Chat.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.chat.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Chat.ChatA.class);
        }
    }

    public static class ChatB implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInChat packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                int messageLength = packet.getMessage().length();

                if (messageLength > (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_11) ? 100 : 256)) {
                    return Validation.inst().cancel().flag()
                            .onViolation(onViolation -> {
                                if (onViolation != null) onViolation.run();

                                container.getDevelopmentData(this).addPoint("Message length", messageLength);
                            })
                            .build();
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
            return "Chat.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.chat.b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Chat.ChatB.class);
        }
    }

    private static String getLabel(String command) {
        String label = command.split( " ")[0];

        return label.startsWith("/") ? getNameFormat(label) : label;
    }

    private static String getNameFormat(String command) {
        if (command.startsWith("/")) {
            return command.substring(1).toLowerCase();
        }

        return command.toLowerCase();
    }

    public static String[] getRegisteredCommands(List<String> commands) {
        IStructureModifier<CommandMap> craftServerModifier = ReflectStructureCache.acquireStructure(NMS.getCraftServerClass())
                .withType(CommandMap.class)
                .withTarget(Bukkit.getServer());
        CommandMap commandMap = craftServerModifier.readField(0);
        List<String> knownCommands = new ArrayList<>();

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
        commandCheck = commandCheck.toLowerCase();
        String format = getNameFormat(commandSent);

        String[] args = commandSent.split(" ");
        if (args.length == 1) {
            return format.equalsIgnoreCase(commandCheck);
        } else {
            return format.startsWith(commandCheck);
        }
    }

    public static boolean matchRegex(String regexCheck, String commandSent) {
        return Pattern.compile(regexCheck).matcher(commandSent).matches();
    }

    public static class CommandsA implements ViolationCheck, PacketCheck<WrappedPlayInChat> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static String[] prohibitedCommandsLiteral;
        private static String[] prohibitedCommandsRegex;

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedPlayInChat packet, CheckContextContainer container) {
            String message = packet.getMessage();
            if (!message.startsWith("/")) {
                return Validation.PASS;
            }

            Consumer<Runnable> onViolationConsumer = onViolation -> {
                if (onViolation != null) onViolation.run();

                String messageFormatted = message.length() > 25 ? message.substring(0, 25) + "[...]" : message;
                container.getDevelopmentData(this).addPoint("Command", messageFormatted);
            };

            for (String prohibitedCommand : prohibitedCommandsLiteral) {
                if (match(prohibitedCommand, message)) {
                    return Validation.inst().cancel().flag()
                            .onViolation(onViolationConsumer)
                            .build();
                }
            }

            for (String prohibitedCommand : prohibitedCommandsRegex) {
                if (matchRegex(prohibitedCommand, message)) {
                    return Validation.inst().cancel().flag()
                            .onViolation(onViolationConsumer)
                            .build();
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
            return "Command.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.command.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Commands.class);
        }
    }
}
