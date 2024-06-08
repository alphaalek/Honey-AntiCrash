package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.lang.Lang;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {
        String outdatedVersion = HoneyPlugin.get().isUsingOldVersion() ? Lang.getFormatted("command.outdated", HoneyPlugin.get().getLatestVersion()) : "";
        String version = HoneyPlugin.get().getPlugin().getDescription().getVersion();

        sender.sendMessage(Lang.getFormatted("command.byline", version, outdatedVersion));
        for (CommandImpl command : HoneyPlugin.get().getCommandManager().getCommands()) {
            sender.sendMessage(" §8" + command.getUsage() + "  §7" + command.getDescription());
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
        return Lang.get("command.desc.help");
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
