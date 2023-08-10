package me.alek.honey.utility;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.services.NotificationService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker {

    private static final String UPDATE_CHECK_URL = "https://raw.githubusercontent.com/alphaalek/Honey-AntiCrash/master/version";

    private final String version;
    private final String latestVersion;

    public UpdateChecker() {
        this.version = HoneyPlugin.get().getDescription().getVersion();
        this.latestVersion = getLatestVersion();
    }

    public String getLatestVersion() {
        try {
            URL url = new URL(UPDATE_CHECK_URL);
            InputStream inputStream = url.openConnection().getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            return reader.readLine();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public boolean isOldVersion() {
        // just returning false because the server can't open connection to the GitHub link and therefore don't know the latest version.
        if (latestVersion.equals("")) return false;

        return !version.equals(latestVersion);
    }

    public void sendWarning(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(NotificationService.PREFIX + " §7Found an update: v" + latestVersion + ". Download:");
                player.sendMessage("§ahttps://github.com/alphaalek/Honey-AntiCrash/releases/");
            }
        }.runTaskLater(HoneyPlugin.get(), 40L);
    }

}
