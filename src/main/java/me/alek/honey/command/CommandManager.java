package me.alek.honey.command;

import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CommandManager implements CommandExecutor {

    private final String PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §7";
    private final JavaPlugin plugin;
    private final Map<String, CommandImpl> commands = new HashMap<>();
    private final CooldownManager cooldownManager;
    private CommandImpl helpCommand;


    public CommandManager(JavaPlugin plugin) {
        plugin.getCommand("honey").setExecutor(this);
        this.plugin = plugin;
        cooldownManager = new CooldownManager();
    }

    public void registerCommand(CommandImpl command, boolean isHelpCommand) {
        if (isHelpCommand) {
            helpCommand = command;
        }
        commands.put(command.getName(), command);
    }
    public Collection<CommandImpl> getCommands() {
        return commands.values();
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (this.helpCommand != null) {
                this.helpCommand.perform(commandSender);
            }
            return true;
        }
        final String firstArgument = args[0];
        Optional<Map.Entry<String, CommandImpl>> commandImplEntryOptional = this.commands
                .entrySet()
                .stream()
                .filter(commandLabel -> commandLabel.getKey().equals(firstArgument))
                .findFirst();

        try {
            final Map.Entry<String, CommandImpl> commandImplEntry = commandImplEntryOptional.orElseThrow(() -> new CommandNotFoundException(firstArgument));
            final CommandImpl commandImpl = commandImplEntry.getValue();

            if (!commandImpl.isExecutableByConsole() && commandSender instanceof ConsoleCommandSender) {
                commandSender.sendMessage(PREFIX + "Denne kommando kan ikke bruges af console.");
                return true;
            }
            if (        commandImpl.getPermission() != null
                    && !commandImpl.getPermission().equals("")
                    && !commandSender.hasPermission(commandImpl.getPermission())) {
                commandSender.sendMessage(PREFIX + "Du har ikke adgang til denne kommando!");
                return true;
            }
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                if (this.cooldownManager.hasCooldown(player, commandImpl)) {
                    commandSender.sendMessage(PREFIX + "Du har cooldown på denne kommando. Vent "
                            + this.cooldownManager.getFormattedCooldown(player, commandImpl));
                    return true;
                }
                this.cooldownManager.setCooldown(player, commandImpl);
            }
            commandImpl.perform(commandSender);
        } catch (CommandNotFoundException ex) {
            if (this.helpCommand == null) {
                commandSender.sendMessage(PREFIX + "Kunne ikke finde nogen kommando med navnet §e" + ex.getMessage()
                        + "§7. Skriv §e/honey help §7 for at se hvilke kommandoer du kan bruge.");
            }
            else {
                this.helpCommand.perform(commandSender);
            }
        }
        return true;
    }
}
