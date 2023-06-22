package me.alek.honey.command;

import org.bukkit.command.CommandSender;

public interface CommandImpl {

    String getName();

    String getUsage();

    String getDescription();

    String getPermission();

    boolean isExecutableByConsole();

    int getCooldown();

    void perform(CommandSender sender);
}
