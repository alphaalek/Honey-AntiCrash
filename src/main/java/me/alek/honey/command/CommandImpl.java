package me.alek.honey.command;

import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.command.CommandSender;

public interface CommandImpl {

    String getName();

    String getUsage();

    String getDescription();

    String getPermission();

    boolean isExecutableByConsole();

    int getCooldown();

    void perform(CommandSender sender, String[] args);
}
