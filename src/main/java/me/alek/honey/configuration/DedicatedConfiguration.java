package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class DedicatedConfiguration implements Configuration {

    private boolean misConfigured;

    public DedicatedConfiguration() {
        HoneyPlugin.get().getPlugin().saveDefaultConfig();

        loadFile();
    }

    public void loadFile() {
        FileConfiguration configuration = HoneyPlugin.get().getPlugin().getConfig();
        ConfigurationOptions.setFile(configuration);

        this.misConfigured = ConfigurationOptions.load();
    }

    public void reload() {
        HoneyPlugin.get().getPlugin().reloadConfig();
        loadFile();
    }

    @Override
    public boolean isMisconfigured() {
        return this.misConfigured;
    }
}
