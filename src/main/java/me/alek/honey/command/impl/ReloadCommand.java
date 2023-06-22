package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandImpl {

    public ReloadCommand(CommandManager commandManager) {
        commandManager.registerCommand(this, false);
    }

    @Override
    public void perform(CommandSender sender) {
        sender.sendMessage("§8[§6Honey§8] §7Reloader pluginnet.");
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
        return "Reloader pluginnet.";
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
