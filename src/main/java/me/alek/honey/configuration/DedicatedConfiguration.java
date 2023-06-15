package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class DedicatedConfiguration implements Configuration {

    private final FileConfiguration configuration;

    public DedicatedConfiguration() {
        HoneyPlugin.get().saveDefaultConfig();
        this.configuration = HoneyPlugin.get().getConfig();

        ConfigurationOptions.setFile(configuration);

        Bukkit.getLogger().info(HoneyPlugin.get().getConfig().getInt("block-place.vls-amount") + "");
        ConfigurationOptions.load();
    }

}
