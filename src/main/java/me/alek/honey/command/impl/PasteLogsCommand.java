package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.tracker.HandleTracker;
import me.alek.honey.tracker.MarkableHandleTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class PasteLogsCommand implements CommandImpl {

    public PasteLogsCommand(CommandManager commandManager) {
        commandManager.registerCommand(this, false);
    }

    @Override
    public void perform(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cOh! This feature is not done yet!");

        HandleTracker<Double, Player> serverThreadHandleTracker = HoneyPlugin.get().getServerThreadHandleTracker();
        DecimalFormat formatter = new DecimalFormat("##.####");

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + "§7 Sending information about how much each player take up server thread...");

        for (Player player : serverThreadHandleTracker.getTrackersOrdered()) {

            double timeToTotalQuotient = serverThreadHandleTracker.getTimeToTotalQuotient(player);
            double timeToAverageQuotient = serverThreadHandleTracker.getTimeToAverageQuotient(player);

            sender.sendMessage("§e" + player.getName() + ": §7" + formatter.format(timeToTotalQuotient * 100) + "% §8(to total)§7, " + formatter.format(timeToAverageQuotient * 100) + "% §8(to average)");
        }
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
        return "Paste the violation logs of a player.";
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
