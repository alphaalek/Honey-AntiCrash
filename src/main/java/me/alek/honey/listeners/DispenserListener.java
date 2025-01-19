package me.alek.honey.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

public class DispenserListener implements Listener {

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (event.getBlock().getY() >= event.getBlock().getWorld().getMaxHeight() - 1) {
            event.setCancelled(true);
        }
    }
}
