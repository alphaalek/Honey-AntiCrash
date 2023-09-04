package me.alek.honey.tracker.impl;

import me.alek.honey.tracker.AbstractOldAndCurrentHandleTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerNettyPacketHandleTracker extends AbstractOldAndCurrentHandleTracker {

    public PlayerNettyPacketHandleTracker(JavaPlugin plugin) {
        super(plugin, 120L, 5, 12, 30);
        update();
    }
}
