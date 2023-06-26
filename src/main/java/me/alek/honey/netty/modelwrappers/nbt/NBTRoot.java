package me.alek.honey.netty.modelwrappers.nbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NBTRoot extends NBTCompound {

    private HashMap<NBTBase<?, ?>, List<NBTBase<?, ?>>> ownerMap;
    private HashMap<NBTBase<?, ?>, Integer> distanceToTailMap;
    private int maxDistanceToTail = 0;
    private int totalElements = 0;

    public NBTRoot() {
        super(null, null, null);
    }

    public void registerSub(NBTBase<?, ?> owner, NBTBase<?, ?> sub) {
        // da registerSub bliver kaldt i NBTBase (super constructor), er ownerMap & distanceToRootMap
        // ikke initialized endnu hvis de eks. havde været på class level (gælder kun for NBTRoot)
        if (ownerMap == null || distanceToTailMap == null) {
            ownerMap = new HashMap<>();
            distanceToTailMap = new HashMap<>();
        }
        if (!ownerMap.containsKey(owner)) {
            ownerMap.put(owner, new ArrayList<>());
        }
        if (owner != null && sub != null && owner.getHandle() != null && sub.getHandle() != null) {
            totalElements++;

            if (!distanceToTailMap.containsKey(sub)) {
                distanceToTailMap.put(sub, 0);
                distanceToTailMap.put(owner, 1);
            }
            else {
                distanceToTailMap.put(owner, distanceToTailMap.get(sub) + 1);
            }
            int ownerDistance = distanceToTailMap.get(owner);
            if (ownerDistance > maxDistanceToTail) {
                maxDistanceToTail = ownerDistance;
            }
        }
    }


    public List<NBTBase<?, ?>> getElementsFor(NBTBase<?, ?> owner) {
        return ownerMap.getOrDefault(owner, new ArrayList<>());
    }

    public <T extends NBTBase<?, ?>> List<T> getElementsFor(NBTBase<?, ?> owner, NBTType type) {
        final ArrayList<T> acquiredSub = new ArrayList<>();
        for (NBTBase<?, ?> sub : getElementsFor(owner)) {
            if (sub.getType() == type) {
                // laver bound til den NBTBase
                acquiredSub.add((T) sub);
            }
        }
        return acquiredSub;
    }

    public int getDistanceToTailBTBase(NBTBase<?, ?> element) {
        return distanceToTailMap.getOrDefault(element, -1);
    }

    public int getMaxDistanceBetweenTail() {
        return maxDistanceToTail;
    }

    public int getTotalElements() {
        return totalElements;
    }

}
