package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class DedicatedConfiguration implements Configuration {

    private boolean misConfigured;

    public DedicatedConfiguration() {
        HoneyPlugin.get().saveDefaultConfig();

        misConfigured = loadFile();
    }

    public boolean loadFile() {
        FileConfiguration configuration = HoneyPlugin.get().getConfig();
        ConfigurationOptions.setFile(configuration);

        return ConfigurationOptions.load();
    }

    public void reload() {
        HoneyPlugin.get().reloadConfig();
        misConfigured = loadFile();
    }

    @Override
    public boolean isMisConfigured() {
        return misConfigured;
    }

}
