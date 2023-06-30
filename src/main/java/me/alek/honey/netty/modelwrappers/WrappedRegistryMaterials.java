package me.alek.honey.netty.modelwrappers;

import javax.annotation.Nullable;

public class WrappedRegistryMaterials<K, V> extends WrappedObject<WrappedRegistryMaterials<K, V>> {

    private final Class<K> class1;
    private final Class<V> class2;

    public WrappedRegistryMaterials(@Nullable Object handle, Class<K> class1, Class<V> class2) {
        super(handle);
        this.class1 = class1;
        this.class2 = class2;
    }

    public V getAtIndex(int index) {
        return getModifier(class2).invokeMethod("a", 0, index);
    }

    public int getIndexFor(V value) {
        return getModifier(int.class).invokeMethod("b", 0, value);
    }
}
