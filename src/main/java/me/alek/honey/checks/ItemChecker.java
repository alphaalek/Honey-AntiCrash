package me.alek.honey.checks;

import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import org.bukkit.inventory.ItemStack;

public class ItemChecker {

    public boolean checkItem(ItemStack item) {
        NBTCompound nbt = NBTFactory.fromItem(item);
        return false;
    }
}
