package me.alek.honey.netty.nbt;

import me.alek.honey.configuration.ConfigurationOptions;

import java.util.*;

public class NBTRoot {

    public static NBTRoot createNewDefault() {
        return new NBTRoot(ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES);
    }

    private Map<NBTBase<?, ?>, List<NBTBase<?, ?>>> ownerMap;
    private Map<NBTBase<?, ?>, Integer> distanceToTailMap;
    private Map<NBTType, List<NBTBase<?, ?>>> nbtTypeLookup;
    private int maxDistanceToTail;
    private int totalElements;

    private final int maximumElementsWithin;

    public NBTRoot(int maximumElementsWithin) {
        this.maximumElementsWithin = maximumElementsWithin;
    }

    public void unsubscribe(NBTBase<?, ?> sub) {
        if (sub.getOwner() != null && ownerMap.get(sub.getOwner()) != null) {
            ownerMap.get(sub.getOwner()).remove(sub);
            if (ownerMap.get(sub.getOwner()).isEmpty()) {
                distanceToTailMap.put(sub.getOwner(), 0);
            }
        }

        if (sub.getType() != null && nbtTypeLookup.get(sub.getType()) != null) {
            nbtTypeLookup.get(sub.getType()).remove(sub);
        }

        totalElements--;
        // TODO: auto-generate max distance to tail instead of using an int, because we don't know which path it chose originally,
        //  so we can't update without iterating over all elements within the root once again
    }

    public void subscribe(NBTBase<?, ?> owner, NBTBase<?, ?> sub) {
        // because this method is being called in the NBTBase super constructor, the ownerMap and distanceToRootMap
        // are not initialized yet if they have been on class level (only counts for instances assigned from NBTRoot)
        if (owner == null || sub == null) {
            return;
        }
        if (ownerMap == null || distanceToTailMap == null) {
            ownerMap = new HashMap<>();
            distanceToTailMap = new HashMap<>();
            nbtTypeLookup = new EnumMap<>(NBTType.class);
        }

        // do the subscription
        if (!ownerMap.containsKey(owner)) {
            ownerMap.put(owner, new ArrayList<>());
        }
        if (!owner.isNull() && !sub.isNull()) {
            totalElements++;

            // add them to the lookup
            nbtTypeLookup.computeIfAbsent(sub.getType(), (base) -> new ArrayList<>());
            nbtTypeLookup.get(sub.getType()).add(sub);

            // because nbt parsing run starts from the outside, I use distance to tail instead of distance to root
            if (!distanceToTailMap.containsKey(sub)) {
                distanceToTailMap.put(sub, 0);
                distanceToTailMap.put(owner, 1);
            } else {
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
                acquiredSub.add((T) sub);
            }
        }
        return acquiredSub;
    }

    public <T extends NBTBase<?, ?>> List<T> getAllElementsAs(NBTType type) {
        return (List<T>) nbtTypeLookup.computeIfAbsent(type, (nbtType) -> new ArrayList<>());
    }

    public <T> List<NBTElement<T>> getAllPrimitiveElementsAs(NBTType type) {
        return (List<NBTElement<T>>) (List<?>) nbtTypeLookup.computeIfAbsent(type, (nbtType) -> new ArrayList<>());
    }

    public int getDistanceToTail(NBTBase<?, ?> element) {
        return distanceToTailMap.getOrDefault(element, -1);
    }

    public int getDistanceToRoot(NBTBase<?, ?> element) {
        NBTBase<?, ?> owner = element;
        int counter = 0;
        while (owner != owner.getOwner()) {
            counter++;
            owner = owner.getOwner();
        }
        return counter;
    }

    public int getMaxDistanceBetweenTail() {
        return maxDistanceToTail;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public int getMaximumElementsWithin() {
        return maximumElementsWithin;
    }

}
