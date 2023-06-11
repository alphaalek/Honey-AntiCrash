package me.alek.honey.configuration;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigurationOptions {

    private final FileConfiguration file;

    public ConfigurationOptions(FileConfiguration file) {
        this.file = file;
    }

    public List<String> getProhibitedTabCompleteCommands() {
        return file.getStringList("prohibited-tab-complete-commands");
    }

    public List<String> getProhibitedCommands() {
        return file.getStringList("prohibited-commands");
    }

}
