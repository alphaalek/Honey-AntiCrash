package me.alek.honey.async;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String @NotNull [] strings) {
        WorkerPool.ASYNC_PACKET_DECODER_POOL.sendDebug(sender);
        return true;
    }
}
