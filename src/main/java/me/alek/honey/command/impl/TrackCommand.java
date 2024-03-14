package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.tracker.markable.MarkableHandleTracker;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;

public class TrackCommand implements CommandImpl {

    private static final DecimalFormat FORMATTER = new DecimalFormat("##.##");

    @Override
    public void perform(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(CommandManager.PREFIX + " §cUsage: /honey track <players/checks>");
            return;
        }

        switch (args[0]) {
            case "players":
                WideTempQueuedHandleTracker serverThreadHandleTracker = HoneyPlugin.get().getServerThreadHandleTracker();

                if (serverThreadHandleTracker.getTrackerSize() == 0) {
                    sender.sendMessage(CommandManager.PREFIX + " §cNo data to be shown yet!");
                    return;
                }
                sender.sendMessage(CommandManager.PREFIX + "§7 Sending information about the space occupation of server thread by players...");

                for (HoneyPlayer player : serverThreadHandleTracker.getTrackersOrdered()) {

                    double tenSeconds = serverThreadHandleTracker.getTimeToAverageWithoutQuotient(player, Timespan.TEN_SECOND, true);
                    double fifteenSeconds = serverThreadHandleTracker.getTimeToAverageWithoutQuotient(player, Timespan.FIFTEEN_SECOND, true);
                    double thirtySeconds = serverThreadHandleTracker.getTimeToAverageWithoutQuotient(player, Timespan.HALF_MINUTE, true);
                    double sixtySeconds = serverThreadHandleTracker.getTimeToAverageWithoutQuotient(player, Timespan.MINUTE, true);

                    sender.sendMessage("§e" + player.name + ": §7" + FORMATTER.format(tenSeconds * 100) + "% §8(10s) §7" + FORMATTER.format(fifteenSeconds * 100) + "% §8(15s) §7" + FORMATTER.format(thirtySeconds * 100) + "% §8(30s) §7" + FORMATTER.format(sixtySeconds * 100) + "% §8(60s)");
                }
                sender.sendMessage("§f");
                sender.sendMessage(" §8What is this showing?");
                sender.sendMessage(" §7Type §e/honey stq§7 to see more information.");

                break;
            case "checks":
                MarkableHandleTracker<Double, String> checkTimeHandleTracker = HoneyPlugin.get().getCheckHandleTracker();

                if (checkTimeHandleTracker.getTrackerSize() == 0) {
                    sender.sendMessage(CommandManager.PREFIX + " §cNo data to be shown yet!");
                    return;
                }
                sender.sendMessage(CommandManager.PREFIX + "§7 Sending information about the work of checks...");

                for (String trackName : checkTimeHandleTracker.getTrackersOrdered()) {

                    double timeToTotalQuotient = checkTimeHandleTracker.getTimeToTotalQuotient(trackName);
                    double timeToAverageQuotient = checkTimeHandleTracker.getAverageTimePerToAverageQuotient(trackName);

                    sender.sendMessage("§e" + trackName + ": §7" + FORMATTER.format(timeToTotalQuotient * 100) + "% §8(time of total)§7, " + FORMATTER.format(timeToAverageQuotient * 100) + "% §8(time/op. of avg.)");
                }
                sender.sendMessage("§7Remember, that all this work is done on the netty threads, so it wont affect the server thread at all!");
                break;
            default:
                sender.sendMessage(CommandManager.PREFIX + " §cUsage: /honey track <players/checks>");
        }
    }
    @Override
    public String getName() {
        return "track";
    }

    @Override
    public String getUsage() {
        return "/honey track";
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
