package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReloadCommand implements CommandImpl {

    private final AtomicBoolean reloading = new AtomicBoolean();

    @Override
    public synchronized void perform(CommandSender sender, String[] args) {
        if (reloading.get()) {
            sender.sendMessage(CommandManager.PREFIX + "§c The plugin is already reloading!");
            return;
        }
        sender.sendMessage(CommandManager.PREFIX + "§7 Reloading the plugin...");

        reloading.set(true);
        HoneyPlugin.get().getPlugin().getServer().getScheduler().runTaskAsynchronously(HoneyPlugin.get().getPlugin(), () -> {
            HoneyPlugin.get().reload();
            sender.sendMessage(CommandManager.PREFIX + "§7 Reload done.");
            reloading.set(false);
        });
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
