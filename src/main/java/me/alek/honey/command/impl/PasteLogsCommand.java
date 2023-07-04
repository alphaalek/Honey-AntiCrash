package me.alek.honey.command.impl;

import me.alek.honey.command.CommandImpl;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.command.CommandSender;

public class PasteLogsCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender) {
        sender.sendMessage(ConfigurationOptions.General.PREFIX + " §cØv! Denne kommando er ikke helt færdig endnu!");
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
        return "Paste logs af alle violations fra en spiller.";
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
