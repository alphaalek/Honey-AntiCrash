package me.alek.honey.netty.modelwrappers.nbt;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NBTRoot extends NBTCompound {

    private HashMap<NBTBase<?, ?>, List<NBTBase<?, ?>>> ownerMap;
    private int totalElements = -2; // for at se bort fra root & compound root

    public NBTRoot() {
        super(null, null, null);
    }

    public void registerSub(NBTBase<?, ?> owner, NBTBase<?, ?> sub) {
        // da registerSub bliver kaldt i NBTBase (super constructor), er ownerMap ikke initialized endnu
        // hvis den eks. havde været på class level
        if (ownerMap == null) {
            ownerMap = new HashMap<>();
        }
        if (!ownerMap.containsKey(owner)) {
            ownerMap.put(owner, new ArrayList<>());
        }
        totalElements++;
        Bukkit.broadcastMessage("register sub " + sub.getType() + " in owner " + owner.getType() + " in root " + owner.getRoot().getType());
        ownerMap.get(owner).add(sub);
    }

    public int getTotalElements() {
        return totalElements;
    }

}
