package me.alek.honey.command;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.impl.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandManager implements CommandExecutor {

    private final Map<String, CommandImpl> commands = new HashMap<>();
    private final CooldownManager cooldownManager = new CooldownManager();

    public CommandManager(Plugin plugin) {
        if (!(plugin instanceof JavaPlugin)) {
            throw new UnsupportedOperationException("Command manager registered with plugin not instanceof JavaPlugin!");
        }

        ((JavaPlugin)plugin).getCommand("honey").setExecutor(this);

        registerCommands();
    }

    public void registerCommands() {
        registerCommand(new HelpCommand());
        registerCommand(new NotifyCommand());
        registerCommand(new ReloadCommand());
        registerCommand(new PasteLogsCommand());
        registerCommand(new TrackCommand());
        registerCommand(new MonitorCommand());
        registerCommand(new PlayerInfoCommand());
        registerCommand(new PacketMonitorCommand());
        registerCommand(new PipelineDebugCommand());
    }

    public void registerCommand(CommandImpl command) {
        commands.put(command.getName(), command);
    }

    public Collection<CommandImpl> getCommands() {
        return commands.values();
    }

    public void showInfo(CommandSender sender) {
        String outdatedVersion = HoneyPlugin.get().isUsingOldVersion() ? Lang.getFormatted("command.outdated", HoneyPlugin.get().getLatestVersion()) : "";
        String version = HoneyPlugin.get().getPlugin().getDescription().getVersion();

        sender.sendMessage(Lang.getFormatted("command.byline", version, outdatedVersion));
        sender.sendMessage("§8* §7" + HoneyPlugin.get().getPlugin().getDescription().getDescription());
        sender.sendMessage("§8* §7Discord: §8https://discord.gg/veAtFHG65u");
        sender.sendMessage("");

        if (ConfigurationOptions.Messages.BASE_COMMAND_FEATURE_INFOS) {
            StringBuilder disabledFeatures = new StringBuilder();
            if (!ConfigurationOptions.Postpone.ENABLED) {
                disabledFeatures.append("Tick Postpone, ");
            }
            if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED) {
                disabledFeatures.append("No-Bypass Permission, ");
            }
            if (!ConfigurationOptions.ItemAnalysis.General.ENABLED) {
                disabledFeatures.append("Item Analysis, ");
            }

            int disabledNettyChecks = 0;
            for (Class<?> clazz : ConfigurationOptions.Netty.class.getDeclaredClasses()) {
                if (ConfigurationOptions.ViolationSetting.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(ConfigurationOptions.Setting.class)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends ConfigurationOptions.ViolationSetting> vlSettingClass = (Class<? extends ConfigurationOptions.ViolationSetting>) clazz;
                    ConfigurationOptions.ViolationSetting setting = ConfigurationOptions.getVlSetting(vlSettingClass);
                    if (setting != null && !setting.isEnabled()) {
                        disabledNettyChecks++;
                    }
                }
            }
            if (disabledNettyChecks > 0) {
                disabledFeatures.append(disabledNettyChecks).append(" Netty Checks, ");
            }

            if (disabledFeatures.length() > 0) {
                sender.sendMessage("§cNote! Disabled features: " + disabledFeatures.substring(0, disabledFeatures.length() - 2));
                sender.sendMessage("");
            }
        }

        sender.sendMessage(Lang.get("command.help.suggestion"));
    }

    private String getDefaultUnknownCommandMessage() {
        File file = new File("spigot.yml");
        if (!file.exists()) {
            return null;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml.getString("messages.unknown-command");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (ConfigurationOptions.General.HIDE_COMMAND_ENABLED && !sender.hasPermission(ConfigurationOptions.General.HIDE_BYPASS_PERMISSION)) {
            String message = ConfigurationOptions.General.HIDE_COMMAND_MESSAGE;
            if (message.equalsIgnoreCase("SPIGOT_DEFAULT")) {
                message = getDefaultUnknownCommandMessage();
            }
            if (message == null) {
                message = "Unknown command. Type \"/help\" for help.";
            }
            sender.sendMessage(message);
            return true;
        }

        if (args.length == 0) {
            showInfo(sender);
            return true;
        }

        String firstArgument = args[0];
        Optional<Map.Entry<String, CommandImpl>> commandImplEntryOptional = this.commands
                .entrySet()
                .stream()
                .filter(commandLabel -> commandLabel.getKey().equals(firstArgument))
                .findFirst();
        String[] implArgs = new String[args.length - 1];
        if (implArgs.length > 0) {
            System.arraycopy(args, 1, implArgs, 0, implArgs.length);
        }

        try {
            Map.Entry<String, CommandImpl> commandImplEntry = commandImplEntryOptional.orElseThrow(() -> new CommandNotFoundException(firstArgument));
            CommandImpl commandImpl = commandImplEntry.getValue();

            if (!commandImpl.isExecutableByConsole() && sender instanceof ConsoleCommandSender) {
                sender.sendMessage(Lang.getFormatted("command.no.console"));
                return true;
            }

            if (commandImpl.getPermission() != null
                    && !commandImpl.getPermission().isEmpty()
                    && !sender.hasPermission(commandImpl.getPermission())
                    && !sender.hasPermission("honey.*")) {
                sender.sendMessage(Lang.getFormatted("command.no.permission"));
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (this.cooldownManager.hasCooldown(player, commandImpl)) {
                    sender.sendMessage(
                            Lang.getFormatted("command.cooldown", this.cooldownManager.getFormattedCooldown(player, commandImpl)));
                    return true;
                }
                this.cooldownManager.setCooldown(player, commandImpl);
            }
            commandImpl.perform(sender, implArgs);
        } catch (CommandNotFoundException ex) {
            showInfo(sender);
        }

        return true;
    }
}
