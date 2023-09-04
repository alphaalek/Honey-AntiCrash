package me.alek.honey.tracker.impl;

import me.alek.honey.tracker.AbstractOldAndCurrentHandleTracker;

public class PlayerNettyPacketHandleTracker extends AbstractOldAndCurrentHandleTracker {

    public PlayerNettyPacketHandleTracker() {
        super(100L, 5, 10, 30);
        update();
    }
}
