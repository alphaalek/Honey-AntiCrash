package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.tracker.MarkableHandleTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;

public class PerformanceMonitorCommand implements CommandImpl {

    public PerformanceMonitorCommand(CommandManager manager) {
        manager.registerCommand(this, false);
    }

    @Override
    public void perform(CommandSender sender) {
        MarkableHandleTracker<Double, String> checkTimeHandleTracker = HoneyPlugin.get().getCheckHandleTracker();
        DecimalFormat formatter = new DecimalFormat("##.####");

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + "§7 Sending information about the work of checks...");

        for (String trackName : checkTimeHandleTracker.getTrackersOrdered()) {

            double timeToTotalQuotient = checkTimeHandleTracker.getTimeToTotalQuotient(trackName);
            double timeToAverageQuotient = checkTimeHandleTracker.getTimeToAverageQuotient(trackName);

            sender.sendMessage("§e" + trackName + ": §7" + formatter.format(timeToTotalQuotient * 100) + "% §8(to total)§7, " + formatter.format(timeToAverageQuotient * 100) + "% §8(to average)");
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + "§7 All this work is done in the netty threads, so it wont affect the server thread at all...");
    }

    @Override
    public String getName() {
        return "performance";
    }

    @Override
    public String getUsage() {
        return "/honey performance";
    }

    @Override
    public String getDescription() {
        return "Monitor performance of the checks";
    }

    @Override
    public String getPermission() {
        return "honey.command.performance";
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
