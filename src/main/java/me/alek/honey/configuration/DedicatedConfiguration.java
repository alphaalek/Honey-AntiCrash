package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class DedicatedConfiguration implements Configuration {

    private boolean misConfigured;

    public DedicatedConfiguration() {
        HoneyPlugin.get().getPlugin().saveDefaultConfig();

        misConfigured = loadFile();
    }

    public boolean loadFile() {
        FileConfiguration configuration = HoneyPlugin.get().getPlugin().getConfig();
        ConfigurationOptions.setFile(configuration);

        return ConfigurationOptions.load();
    }

    public void reload() {
        HoneyPlugin.get().getPlugin().reloadConfig();
        misConfigured = loadFile();
    }

    @Override
    public boolean isMisconfigured() {
        return misConfigured;
    }

}
