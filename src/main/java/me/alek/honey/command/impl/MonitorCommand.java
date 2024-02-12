package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class MonitorCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(CommandManager.PREFIX + " §cUsage: /honey monitor <server/player name>");
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
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(CommandManager.PREFIX + "§cYou have to choose a valid player name.");
            return;
        }
        HoneyPlugin.get().getMonitorManager().setupPlayerMonitor(sender, target);
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
        return "Monitor either server performance or player impact.";
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
