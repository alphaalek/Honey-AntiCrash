package me.alek.honey.command;

import me.alek.honey.HoneyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public interface CommandImpl {

    default Optional<HoneyPlayer> getPlayerBy(String arg) {
        return Bukkit.getOnlinePlayers()
                .stream()
                .filter(p -> p.getName().equalsIgnoreCase(arg))
                .findFirst()
                .map(p -> HoneyPlayer.getPlayer(p.getUniqueId()));
    }

    String getName();

    default String getUsage() {
        return "/honey " + getName();
    }

    String getDescription();

    String getPermission();

    boolean isExecutableByConsole();

    default int getCooldown() {
        return 0;
    }

    void perform(CommandSender sender, String[] args);
}
