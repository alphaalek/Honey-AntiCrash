package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.services.NotificationService;
import org.bukkit.command.CommandSender;

public class NotifyCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {
        NotificationService notificationService = HoneyPlugin.get().getNotificationService();

        String toggledString;
        if (notificationService.isSubscribed(sender)) {
            toggledString = Lang.get("notify.off");
            notificationService.untoggle(sender);
        } else {
            toggledString = Lang.get("notify.on");
            notificationService.toggle(sender);
        }
        sender.sendMessage(Lang.getFormatted("notify.toggled", toggledString));
    }

    @Override
    public String getName() {
        return "notify";
    }

    @Override
    public String getUsage() {
        return "/honey notify";
    }

    @Override
    public String getDescription() {
        return Lang.get("command.desc.notify");
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

