package me.alek.honey.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;

public class DispenserListener implements Listener {

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        Block dispenser = event.getBlock();
        World world = dispenser.getWorld();

        if (dispenser.getY() >= world.getMaxHeight() - 1 || dispenser.getY() < 0) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Bukkit.broadcastMessage("block break");
        try {
            Thread.sleep(600);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
