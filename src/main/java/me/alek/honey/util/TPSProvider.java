package me.alek.honey.util;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.SparkTPSProvider;
import me.alek.honey.tracker.Timespan;
import org.bukkit.Bukkit;

public class TPSProvider {

    public static String getTPS(Timespan timespan) {
        double value = -1;
        Bukkit.broadcastMessage(HoneyPlugin.get().getPlugin().getServer().getPluginManager().isPluginEnabled("spark") + "");
        if (SparkTPSProvider.SparkValidator.VALIDATOR.has()) {
            value = SparkTPSProvider.getTPS(timespan);
        }

        if (value == -1) {
            value = HoneyPlugin.get().getTPSTracker().evaluateTPS(timespan);
        }

        value = NumberConversions.format(value);
        return (value > 20 ? "§a*" : "") + (value < 12 ? "§c" : value < 17 ? "§e" : "§a") + Math.min(20, value);
    }
}
