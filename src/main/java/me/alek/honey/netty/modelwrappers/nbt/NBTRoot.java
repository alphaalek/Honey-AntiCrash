package me.alek.honey.netty.modelwrappers.nbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NBTRoot extends NBTCompound {

    private final HashMap<NBTBase<?, ?>, List<NBTBase<?, ?>>> ownerMap = new HashMap<>();

    public NBTRoot(Object rawNms) {
        super(rawNms, null, null);
    }

    public void registerSub(NBTBase<?, ?> owner, NBTBase<?, ?> sub) {
        if (!ownerMap.containsKey(owner)) {
            ownerMap.put(owner, new ArrayList<>());
        }
        ownerMap.get(owner).add(sub);
    }
}
