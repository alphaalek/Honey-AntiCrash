package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.JavaConverter;

public abstract class WrappedObject<T> {

    private final Object handle;
    private final ReflectStructure<Object, Object> reflectStructure;

    public WrappedObject(Object handle) {
        this.handle = handle;

        if (handle != null) {
            this.reflectStructure = ReflectStructure.Provider.acquireStructure(handle.getClass());
        } else {
            this.reflectStructure = null;
        }
    }

    public Object getHandle() {
        return handle;
    }

    public ReflectStructure<Object, Object> getReflectionStructure() {
        return reflectStructure;
    }

    public <R> IStructureModifier<R> getModifier(Class<R> clazz) {
        return reflectStructure.withType(clazz).withTarget(handle);
    }

    public <R> IStructureModifier<R> getModifier(Class<T> clazz, JavaConverter<T, R> converter) {
        return reflectStructure.withType(clazz, converter).withTarget(handle);
    }
}
