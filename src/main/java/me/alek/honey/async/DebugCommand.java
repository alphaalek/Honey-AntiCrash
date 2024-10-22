package me.alek.honey.async;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        WorkerPool.ASYNC_PACKET_DECODER_POOL.sendDebug(sender);
        return true;
    }
}
