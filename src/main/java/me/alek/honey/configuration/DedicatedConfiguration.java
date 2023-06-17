package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class DedicatedConfiguration implements Configuration {

    public DedicatedConfiguration() {
        HoneyPlugin.get().saveDefaultConfig();
        loadFile();
    }

    public void loadFile() {
        FileConfiguration configuration = HoneyPlugin.get().getConfig();
        ConfigurationOptions.setFile(configuration);

        ConfigurationOptions.load();
    }

    public void reload() {
        HoneyPlugin.get().reloadConfig();
        loadFile();
    }

}
