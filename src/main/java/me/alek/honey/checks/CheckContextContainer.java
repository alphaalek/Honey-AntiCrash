package me.alek.honey.checks;

import io.netty.buffer.ByteBuf;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheckContextContainer {

    private final Map<Class<?>, ContainerStructure<?>> map = new ConcurrentHashMap<>();

    public void putSerializer(ByteBuf serializer) {
        putValue(ByteBuf.class, serializer);
    }

    public void putItem(ItemStack itemStack) {
        putValue(ItemStack.class, itemStack);
    }

    public void putMap(Map<?, ?> map) {
        putValue(Map.class, map);
    }

    public void putChunk(Chunk chunk) {
        putValue(Chunk.class, chunk);
    }

    public void putLocation(Location location) {
        putValue(Location.class, location);
    }


    public ByteBuf getSerializer(int index) {
        return getValue(ByteBuf.class, index);
    }

    public ItemStack getItem(int index) {
        return getValue(ItemStack.class, index);
    }

    public Map<?, ?> getMap(int index) {
        return getValue(Map.class, index);
    }

    public Chunk getChunk(int index) {
        return getValue(Chunk.class, index);
    }

    public Location getLocation(int index) {
        return getValue(Location.class, index);
    }

    public synchronized <T> void putValue(Class<T> clazz, T value) {
        ContainerStructure<T> structure = getStructure(clazz);
        if (structure == null) {
            map.put(clazz, new ContainerStructure<>());
            putValue(clazz, value);
            return;
        }
        structure.add(value);
    }

    public synchronized <T> T getValue(Class<T> clazz, int index) {
        ContainerStructure<T> structure = getStructure(clazz);
        if (structure != null) {
            return structure.get(index);
        }
        return null;
    }

    public <T> ContainerStructure<T> getStructure(Class<T> clazz) {
        return (ContainerStructure<T>) map.get(clazz);
    }

    public final static class ContainerStructure<T> {

        private final List<T> values = new ArrayList<>();

        public void add(T value) {
            values.add(value);
        }

        public T get(int index) {
            if (!has(index)) {
                return null;
            }
            return values.get(index);
        }

        public boolean has(int index) {
            return values.size() > index;
        }

        public List<T> getValues() {
            return values;
        }
    }
}
