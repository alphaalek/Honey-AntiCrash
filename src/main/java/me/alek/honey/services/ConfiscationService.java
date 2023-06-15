package me.alek.honey.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ConfiscationService {

    public void confiscate(Player player, ItemStack item) {
        Bukkit.broadcastMessage("Confiscated from " + player.getName() + " " + item);
    }
}
