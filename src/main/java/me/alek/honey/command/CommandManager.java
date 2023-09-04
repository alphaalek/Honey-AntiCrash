package me.alek.honey.command;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandManager implements CommandExecutor {

    private final String PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §7";
    private final JavaPlugin plugin;
    private final Map<String, CommandImpl> commands = new HashMap<>();
    private final CooldownManager cooldownManager;

    public CommandManager(JavaPlugin plugin) {
        plugin.getCommand("honey").setExecutor(this);
        this.plugin = plugin;
        cooldownManager = new CooldownManager();
    }

    public void registerCommand(CommandImpl command) {
        commands.put(command.getName(), command);
    }

    public Collection<CommandImpl> getCommands() {
        return commands.values();
    }

    public void showInfo(CommandSender sender) {
        String outdatedVersion = HoneyPlugin.get().isUsingOldVersion() ? " §c(Outdated)" : "";
        String version = HoneyPlugin.get().getPlugin().getDescription().getVersion();

        sender.sendMessage("§6Honey v" + version + ". §aMade by Alek05." + outdatedVersion);
        sender.sendMessage("");
        sender.sendMessage("§6* §8Discord: §7https://discord.gg/FJuKAUDX4C");
        sender.sendMessage("§6* §8Sourcecode: §7https://github.com/alphaalek/Honey-AntiCrash");
        sender.sendMessage("");
        sender.sendMessage("§7Type §e/honey help §7for help on command usage.");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showInfo(commandSender);
            return true;
        }
        final String firstArgument = args[0];
        Optional<Map.Entry<String, CommandImpl>> commandImplEntryOptional = this.commands
                .entrySet()
                .stream()
                .filter(commandLabel -> commandLabel.getKey().equals(firstArgument))
                .findFirst();

        String[] implArgs = new String[args.length - 1];
        if (implArgs.length > 0) System.arraycopy(args, 1, implArgs, 0, implArgs.length);

        try {
            final Map.Entry<String, CommandImpl> commandImplEntry = commandImplEntryOptional.orElseThrow(() -> new CommandNotFoundException(firstArgument));
            final CommandImpl commandImpl = commandImplEntry.getValue();

            if (!commandImpl.isExecutableByConsole() && commandSender instanceof ConsoleCommandSender) {
                commandSender.sendMessage(PREFIX + "This command can not be used by the console.");
                return true;
            }
            if (        commandImpl.getPermission() != null
                    && !commandImpl.getPermission().equals("")
                    && !commandSender.hasPermission(commandImpl.getPermission())) {
                commandSender.sendMessage(PREFIX + "You do not have permission to use this command!");
                return true;
            }
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                if (this.cooldownManager.hasCooldown(player, commandImpl)) {
                    commandSender.sendMessage(PREFIX + "You have cooldown on this command. Wait "
                            + this.cooldownManager.getFormattedCooldown(player, commandImpl) + ".");
                    return true;
                }
                this.cooldownManager.setCooldown(player, commandImpl);
            }
            commandImpl.perform(commandSender, implArgs);
        } catch (CommandNotFoundException ex) {
            showInfo(commandSender);
        }
        return true;
    }
}
