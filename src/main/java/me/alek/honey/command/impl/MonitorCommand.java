package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.Notifications;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class MonitorCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Notifications.PREFIX + " §cUsage: /honey monitor <server/player name>");
            return;
        }
        if (args[0].equalsIgnoreCase("server")) {
            HoneyPlugin.get().getMonitorManager().setupServerMonitor(sender);
            return;
        }
        if (args[0].equalsIgnoreCase("stop")) {
            HoneyPlugin.get().getMonitorManager().removeIfHasMonitor(sender, true);
            return;
        }
        Optional<HoneyPlayer> target = getPlayerBy(args[0]);
        if (!target.isPresent()) {
            sender.sendMessage(Lang.getFormatted("unknown.player"));
            return;
        }

        HoneyPlugin.get().getMonitorManager().setupPlayerMonitor(sender, target.get());
    }

    @Override
    public String getName() {
        return "monitor";
    }

    @Override
    public String getUsage() {
        return "/honey monitor";
    }

    @Override
    public String getDescription() {
        return Lang.get("command.desc.monitor");
    }

    @Override
    public String getPermission() {
        return "honey.command.monitor";
    }

    @Override
    public boolean isExecutableByConsole() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 1;
    }
}
