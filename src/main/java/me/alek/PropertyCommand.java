package me.alek;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PropertyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.isOp() || strings.length == 0) return false;

        commandSender.sendMessage(System.getProperty(strings[0]));
        return false;
    }
}
