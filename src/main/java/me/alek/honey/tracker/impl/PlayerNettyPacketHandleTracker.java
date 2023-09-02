package me.alek.honey.tracker.impl;

import me.alek.honey.tracker.AbstractOldAndCurrentHandleTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerNettyPacketHandleTracker extends AbstractOldAndCurrentHandleTracker {

    public PlayerNettyPacketHandleTracker(JavaPlugin plugin) {
        super(plugin, 41L, 2, 5, 15);
        update();
    }
}
