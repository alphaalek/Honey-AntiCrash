package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.Settings;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInChat;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class CommandValidation extends AbstractPacketListener<WrappedPlayInChat> implements PacketCheck<WrappedPlayInChat>, ViolationCheck {

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

    private static final int id = IDRegistry.CHECKS.acquire();
    private static String[] prohibitedCommandsLiteral;
    private static String[] prohibitedCommandsRegex;

    @Override
    public void load() {
        prohibitedCommandsLiteral = getRegisteredCommands(ConfigurationOptions.Commands.PROHIBITED_COMMANDS_LITERAL);
        prohibitedCommandsRegex = ConfigurationOptions.Commands.PROHIBITED_COMMANDS_REGEX.toArray(new String[0]);
    }

    @Override
    public ResponseContainer check(HoneyPlayer player, WrappedPlayInChat packet, CheckContextContainer container) throws Exception {
        String message = packet.getMessage();
        if (!message.startsWith("/")) {
            return Validation.PASS;
        }

        Consumer<Runnable> onViolationConsumer = onViolation -> {
            if (onViolation != null) onViolation.run();

            String messageFormatted = "\"" + (message.length() > 25 ? message.substring(0, 25) + "[...]" : message) + "\"";
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
    public CheckVisitor<WrappedPlayInChat> getVisitor(HoneyPlayer player) {
        return CheckVisitor.from(player, this);
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> {
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_19)) {
                return Collections.singletonList(PacketTypes.Play.Client.CHAT);
            } else {
                return Collections.singletonList(PacketTypes.Play.Client.CHAT_COMMAND);
            }
        };
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getName() {
        return "Commands";
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
