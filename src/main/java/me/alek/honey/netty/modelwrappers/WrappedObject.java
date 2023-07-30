package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;

public abstract class WrappedObject<T> {

    private final Object handle;
    private ReflectStructure<Object, ?> reflectStructure;

    public WrappedObject(Object handle) {
        this.handle = handle;

        if (handle != null) {
            this.reflectStructure = ReflectStructureCache.acquireStructure(handle.getClass());
        }
    }

    public Object getHandle() {
        return handle;
    }

    public ReflectStructure<Object, ?> getReflectStructure() {
        return reflectStructure;
    }

    public <R> IStructureModifier<R> getModifier(Class<R> clazz) {
        return reflectStructure.withType(clazz).withTarget(handle);
    }
}
