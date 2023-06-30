package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class WrappedItemStack extends ItemStack {

    private final Object handle;
    private NBTCompound tag;

    public WrappedItemStack(Object handle, Material material) {
        super(material);
        this.handle = handle;
    }

    public WrappedItemStack(Object handle, Material material, int amount, short data) {
        super(material, amount, (short) data);
        this.handle = handle;
        this.tag = NBTFactory.fromItem(this);
    }

    public void setTag(NBTCompound nbt) {
        tag = nbt;
    }

    public boolean hasTag() {
        return tag != null;
    }

    public NBTCompound getTag() {
        return tag;
    }


}
