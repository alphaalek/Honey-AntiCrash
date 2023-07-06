package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.services.NotificationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class NotificationToggleCommand implements CommandImpl {

    public NotificationToggleCommand(CommandManager commandManager) {
        commandManager.registerCommand(this, false);
    }

    @Override
    public void perform(CommandSender sender) {
        NotificationService notificationService = HoneyPlugin.get().getNotificationService();

        String toggledString;
        if (notificationService.isSubscribed(sender)) {
            toggledString = "§cfra";
            notificationService.untoggle(sender);
        } else {
            toggledString = "§atil";
            notificationService.toggle(sender);
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §7Du har nu slået notifikationer " + toggledString + "§7.");
    }

    @Override
    public String getName() {
        return "notifications";
    }

    @Override
    public String getUsage() {
        return "/honey notifications";
    }

    @Override
    public String getDescription() {
        return "Toggle notifications on/off for you.";
    }

    @Override
    public String getPermission() {
        return ConfigurationOptions.General.NOTIFY_PERMISSION;
    }

    @Override
    public boolean isExecutableByConsole() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 1;
    }
}

