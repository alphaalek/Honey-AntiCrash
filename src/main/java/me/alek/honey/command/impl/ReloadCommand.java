package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandImpl {

    public ReloadCommand(CommandManager commandManager) {
        commandManager.registerCommand(this, false);
    }

    @Override
    public void perform(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + "§7 Reloading the configuration...");
        HoneyPlugin.get().reload();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getUsage() {
        return "/honey reload";
    }

    @Override
    public String getDescription() {
        return "Reloads the configuration.";
    }

    @Override
    public String getPermission() {
        return "honey.command.reload";
    }

    @Override
    public boolean isExecutableByConsole() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 3;
    }
}
