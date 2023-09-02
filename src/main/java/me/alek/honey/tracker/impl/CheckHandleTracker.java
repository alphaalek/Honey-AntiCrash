package me.alek.honey.tracker.impl;

import me.alek.honey.tracker.AbstractRegistryMarkableHandleTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class CheckHandleTracker extends AbstractRegistryMarkableHandleTracker<String> {

    public CheckHandleTracker(JavaPlugin plugin) {
        super(plugin, 200L);
        update();
    }
}
