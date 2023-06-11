package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class DedicatedConfiguration implements Configuration {

    private final ConfigurationOptions options;
    private final FileConfiguration configuration;

    public DedicatedConfiguration() {
        HoneyPlugin.get().saveDefaultConfig();
        this.configuration = HoneyPlugin.get().getConfig();

        options = new ConfigurationOptions(configuration);
    }

    public ConfigurationOptions getOptions() {
        return options;
    }
}
