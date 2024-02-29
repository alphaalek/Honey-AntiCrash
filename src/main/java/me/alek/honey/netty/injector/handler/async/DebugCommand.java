package me.alek.honey.netty.injector.handler.async;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {

        WorkerPool<Object> pool = WorkerPool.ASYNC_PACKET_DECODER_POOL;
        pool.sendDebug(sender);

        return true;
    }
}
