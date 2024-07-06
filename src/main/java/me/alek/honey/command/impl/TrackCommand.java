package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packet.PacketStateTimings;
import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.TimedHandleTracker;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.validation.types.HoneyValidation;
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
                TimedHandleTracker<Double, HoneyPlayer> tracker = HoneyPlugin.get().getExecutionQueueProxy();
                if (tracker.getTrackerSize() == 0) {
                    sender.sendMessage(Lang.getFormatted("track.no.data"));
                    return;
                }

                sender.sendMessage(Lang.getFormatted("track.players"));
                for (HoneyPlayer player : tracker.getTrackersOrdered()) {
                    double fiveSeconds = tracker.getTimeToAverageWithoutQuotient(player, Timespan.FIVE_SECOND, true);
                    double tenSeconds = tracker.getTimeToAverageWithoutQuotient(player, Timespan.TEN_SECOND, true);
                    double fifteenSeconds = tracker.getTimeToAverageWithoutQuotient(player, Timespan.FIFTEEN_SECOND, true);
                    double thirtySeconds = tracker.getTimeToAverageWithoutQuotient(player, Timespan.HALF_MINUTE, true);

                    sender.sendMessage("§e" + player.name + ": §7" + FORMATTER.format(fiveSeconds * 100) + "% §8(5s) §7" + FORMATTER.format(tenSeconds * 100) + "% §8(10s) §7" + FORMATTER.format(fifteenSeconds * 100) + "% §8(15s) §7" + FORMATTER.format(thirtySeconds * 100) + "% §8(30s) §7");
                }

                sender.sendMessage("§f");
                sender.sendMessage("§7Average: " + format(tracker.getAverage(Timespan.FIVE_SECOND)) + " §8(5s) §7" + "§7" + format(tracker.getAverage(Timespan.TEN_SECOND)) + " §8(10s) §7" + format(tracker.getAverage(Timespan.FIFTEEN_SECOND)) + " §8(15s) §7" + format(tracker.getAverage(Timespan.HALF_MINUTE)) + " §8(30s)");

                break;
            case "checks":
                MarkableHandleTracker<Double, HoneyValidation<?>> checkTracker = HoneyPlugin.get().getValidationHandleTracker();
                if (checkTracker.getTrackerSize() == 0) {
                    sender.sendMessage(Lang.getFormatted("track.no.data"));
                    return;
                }

                sender.sendMessage(Lang.getFormatted("track.checks"));
                for (HoneyValidation<?> trackName : checkTracker.getTrackersOrdered()) {
                    double timeToTotalQuotient = checkTracker.getTimeToTotalQuotient(trackName);
                    double timeToAverageQuotient = checkTracker.getAverageTimePerToAverageQuotient(trackName);

                    sender.sendMessage("§e" + trackName + ": §7" + FORMATTER.format(timeToTotalQuotient * 100) + "% §8(time of total)§7, " + FORMATTER.format(timeToAverageQuotient * 100) + "% §8(avg. time/op.)");
                }
                break;
            default:
                sender.sendMessage(CommandManager.PREFIX + " §cUsage: /honey track <players/checks>");
        }
    }

    private String format(double nanos) {
        return NumberUtils.format(nanos * PacketStateTimings.NANOS_TO_MILLIS_MULTIPLIER, 2) + "ms";
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
        return Lang.get("command.desc.track");
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
