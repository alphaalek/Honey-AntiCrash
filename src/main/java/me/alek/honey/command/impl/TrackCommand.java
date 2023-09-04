package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.StatisticHandleTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class TrackCommand implements CommandImpl {

    private static final DecimalFormat FORMATTER = new DecimalFormat("##.##");

    public TrackCommand(CommandManager manager) {
        manager.registerCommand(this);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cUsage: /honey track <players/checks>");
            return;
        }

        switch (args[0]) {
            case "players":
                StatisticHandleTracker<Double, Player> serverThreadHandleTracker = HoneyPlugin.get().getServerThreadHandleTracker();

                if (serverThreadHandleTracker.getOldTrackerSize() == 0) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cNo data to be shown yet!");
                    return;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + "§7 Sending information about the space occupation of server thread by players...");

                for (Player player : serverThreadHandleTracker.getTrackersOrdered()) {

                    double fiveSeconds = serverThreadHandleTracker.getStatistic(player, 0);
                    double fifteenSeconds = serverThreadHandleTracker.getStatistic(player, 1);
                    double thirtySeconds = serverThreadHandleTracker.getStatistic(player, 2);
                    double sixtySeconds = serverThreadHandleTracker.getStatistic(player, 3);

                    sender.sendMessage("§e" + player.getName() + ": §7" + FORMATTER.format(fiveSeconds * 100) + "% §8(5s) §7" + FORMATTER.format(fifteenSeconds * 100) + "% §8(10s) §7" + FORMATTER.format(thirtySeconds * 100) + "% §8(30s) §7" + FORMATTER.format(sixtySeconds * 100) + "% §8(60s)");
                }
                sender.sendMessage("");
                sender.sendMessage(" §8What is this showing?");
                sender.sendMessage(" §7Type §e/honey stq§7 to see information.");

                break;
            case "checks":
                MarkableHandleTracker<Double, String> checkTimeHandleTracker = HoneyPlugin.get().getCheckHandleTracker();

                if (checkTimeHandleTracker.getTrackerSize() == 0) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cNo data to be shown yet!");
                    return;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + "§7 Sending information about the work of checks...");

                for (String trackName : checkTimeHandleTracker.getTrackersOrdered()) {

                    double timeToTotalQuotient = checkTimeHandleTracker.getTimeToTotalQuotient(trackName);
                    double timeToAverageQuotient = checkTimeHandleTracker.getTimeToAverageQuotient(trackName);

                    sender.sendMessage("§e" + trackName + ": §7" + FORMATTER.format(timeToTotalQuotient * 100) + "% §8(of total)§7, " + FORMATTER.format(timeToAverageQuotient * 100) + "% §8(to average)");
                }
                sender.sendMessage("§7Remember, that all this work is done on the netty threads, so it wont affect the server thread at all!");
                break;
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigurationOptions.General.PREFIX) + " §cUsage: /honey track <players/checks>");
        }
    }
    @Override
    public String getName() {
        return "track";
    }

    @Override
    public String getUsage() {
        return "/honey track <players/checks>";
    }

    @Override
    public String getDescription() {
        return "Track performance of checks and players";
    }

    @Override
    public String getPermission() {
        return "honey.command.track";
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
