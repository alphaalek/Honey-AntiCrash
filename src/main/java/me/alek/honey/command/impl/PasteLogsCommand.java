package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.sql.SQLManagerWrapper;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class PasteLogsCommand implements CommandImpl {

    private final SQLManagerWrapper sqlHandler = HoneyPlugin.get().getSQLManager();

    public PasteLogsCommand(CommandManager commandManager) {
        commandManager.registerCommand(this);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sqlHandler.isEnabled()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cYou don't have MySQL enabled in the configuration.");
            return;
        }
        if (!sqlHandler.isConnectionClosed()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cConnection is not established with the MySQL database. Try again later or fix the connection values.");
            return;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cOh! This feature is not done yet!");
    }

    @Override
    public String getName() {
        return "pastelogs";
    }

    @Override
    public String getUsage() {
        return "/honey pastelogs";
    }

    @Override
    public String getDescription() {
        return "Paste the violation logs of a player.";
    }

    @Override
    public String getPermission() {
        return "honey.command.pastelogs";
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
