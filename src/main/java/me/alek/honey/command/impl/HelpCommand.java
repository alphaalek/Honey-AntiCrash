package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandImpl {

    public HelpCommand(CommandManager commandManager) {
        commandManager.registerCommand(this, true);
    }

    @Override
    public void perform(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6Honey v1.0.0. Lavet af Alek05.");
        for (CommandImpl command : HoneyPlugin.get().getCommandManager().getCommands()) {
            sender.sendMessage(" §a" + command.getUsage() + " > §7" + command.getDescription());
        }
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getUsage() {
        return "/honey help";
    }

    @Override
    public String getDescription() {
        return "Sender kommandoer du kan bruge.";
    }

    @Override
    public String getPermission() {
        return "";
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
