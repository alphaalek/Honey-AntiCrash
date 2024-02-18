package me.alek.honey;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class HoneyPluginInitializer extends JavaPlugin {
    private static final String UPDATE_CHECK_URL = "https://raw.githubusercontent.com/alphaalek/Honey-AntiCrash/master/version";
    private final String latestVersion = getLatestVersion();
    private final HoneyAPI plugin = new HoneyPlugin(this,
            getDescription().getVersion(), !latestVersion.isEmpty() ? latestVersion : getDescription().getVersion());

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

    @Override
    public void onEnable() {
        plugin.onEnable();
    }

    @Override
    public void onDisable() {
        plugin.onDisable();
    }
}
