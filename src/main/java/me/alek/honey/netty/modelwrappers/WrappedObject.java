package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.netty.structure.converters.JavaConverter;

public abstract class WrappedObject<T> {

    private final Object handle;
    private ReflectStructure<Object, Object> reflectStructure;

    public WrappedObject(Object handle) {
        this.handle = handle;

        if (handle != null) {
            this.reflectStructure = CachedReflectStructureProvider.acquireStructure(handle.getClass());
        }
    }

    public Object getHandle() {
        return handle;
    }

    public ReflectStructure<Object, Object> getReflectStructure() {
        return reflectStructure;
    }

    public <R> IStructureModifier<R> getModifier(Class<R> clazz) {
        return reflectStructure.withType(clazz).withTarget(handle);
    }

    public <T, R> IStructureModifier<R> getModifier(Class<T> clazz, JavaConverter<T, R> converter) {
        return reflectStructure.withType(clazz, converter).withTarget(handle);
    }
}
