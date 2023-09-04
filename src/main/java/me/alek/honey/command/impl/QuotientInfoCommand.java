package me.alek.honey.command.impl;

import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import org.bukkit.command.CommandSender;

public class QuotientInfoCommand implements CommandImpl {

    public QuotientInfoCommand(CommandManager manager) {
        manager.registerCommand(this);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        sender.sendMessage(" §7The Server Thread Quotient is based on the amount of work the server thread has to do " +
                "from packets sent by the player. It's evaluated by taking the average of total time without that player in the specific " +
                "interval, and finding the quotient between this filtered average and the time of work by that player in that interval. " +
                "The Netty Thread Quotient is based on the same thing but on the netty threads instead, and they are both shown in the " +
                "development data which can be seen when a player is violated by a honey check. (on hover) The values are always precise, " +
                "although they can be pretty misleading if the player count is low, and they don't always show if a player is heavy on the server.");
    }

    @Override
    public String getName() {
        return "stq";
    }

    @Override
    public String getUsage() {
        return "/honey stq";
    }

    @Override
    public String getDescription() {
        return "See information about the STQ and NTQ";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean isExecutableByConsole() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 0;
    }
}
