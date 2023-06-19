package me.alek.honey.netty.packet.structure;

import me.alek.honey.netty.packet.structure.converters.TwinConverter;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.netty.api.packet.IStructureModifier;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.*;

public class ReflectStructure<T, C> {

    private final static Map<Class<?>, Class<?>> WRAPPER_CLASS_CACHE = new HashMap<Class<?>, Class<?>>(){{
        put(double.class, Double.class);
        put(long.class, Long.class);
        put(int.class, Integer.class);
        put(short.class, Short.class);
        put(float.class, Float.class);
        put(byte.class, Byte.class);
        put(boolean.class, Boolean.class);
    }};
    private final static Class<?> GENERAL_PACKET_CLASS = Reflection.getClass("{nms}.Packet");

    private final Class<?> handle;
    private TwinConverter<T, C> converter;
    private Map<Integer, FieldAccessor<?>> DEFAULT_ACCESSORS;
    private Map<Integer, FieldAccessor<T>> ACQUIRED_ACCESSORS;
    private final Map<Class<?>, ReflectStructure<?, ?>> SUB_TYPE_CACHE = new HashMap<>();

    // default structure
    public ReflectStructure(Class<?> delegate) {
        this.handle = delegate;
        setDefaultFields();
    }

    // specific structure
    public ReflectStructure(
            Class<?> delegate,
            Class<?> type
    ) {
        this(delegate, type, null);
    }

    // specific structure
    public ReflectStructure(
            Class<?> delegate,
            Class<?> type,
            TwinConverter<T, C> converter
    ) {
        this.handle = delegate;
        this.converter = converter;
        setDefaultFields();
        ACQUIRED_ACCESSORS = getAcquiredFields(type);
    }

    private void setDefaultFields() {
        final Map<Integer, FieldAccessor<?>> defaults = new HashMap<>();

        int index = 0;
        Class<?> clazz = handle;

        while (clazz != null && clazz != Object.class) {
            setDefaultFields(index, defaults, clazz);
            clazz = clazz.getSuperclass();
        }
        DEFAULT_ACCESSORS = defaults;
    }

    private Map<Integer, FieldAccessor<?>> setDefaultFields(
            int prevIndex,
            Map<Integer, FieldAccessor<?>> defaults,
            Class<?> clazz
    ) {
        for (Field field : clazz.getDeclaredFields()) {
            FieldAccessor<?> accessor = new FieldAccessor<>(field);
            defaults.put(prevIndex++, accessor);
        }
        return defaults;
    }

    private Map<Integer, FieldAccessor<T>> getAcquiredFields(Class<?> type) {
        int index = 0;
        Map<Integer, FieldAccessor<T>> acquired = new HashMap<>();
        for (FieldAccessor<?> accessor : DEFAULT_ACCESSORS.values()) {
            if (type != accessor.getType()) {
                continue;
            }
            Bukkit.getLogger().info("putting " + accessor.getField().getName() + " in " + handle + " " + " type " + type.getSimpleName());
            acquired.put(index, (FieldAccessor<T>) accessor);
            index++;
        }
        return acquired;
    }

    public <R> ReflectStructure<R, R> withType(Class<R> clazz) {
        // R og D er den samme i generic context
        return withType(clazz, null);
    }


    public <R, D> ReflectStructure<R, D> withType(Class<R> clazz, TwinConverter<R, D> converter) {
        if (clazz == null) {
             return null;
        }
        ReflectStructure<R, D> packetStructure = (ReflectStructure<R, D>) SUB_TYPE_CACHE.get(clazz);
        if (packetStructure == null) {
            packetStructure = new ReflectStructure<>(handle, clazz, converter);
            SUB_TYPE_CACHE.put(clazz, packetStructure);
        }
        return packetStructure;
    }

    public Collection<FieldAccessor<T>> getAccessors() {
        return ACQUIRED_ACCESSORS.values();
    }

    public Collection<FieldAccessor<?>> getDefaultAccessors() {
        return DEFAULT_ACCESSORS.values();
    }

    public IStructureModifier<C> withTarget(Object rawPacket) {
        return new StructureModifier(rawPacket, converter);
    }

    private class StructureModifier implements IStructureModifier<C> {

        private final Object handle;
        private final TwinConverter<T, C> converter;

        public StructureModifier(Object handle, TwinConverter<T, C> converter) {
            this.handle = handle;
            this.converter = converter;
        }

        @Override
        public IStructureModifier<C> write(int index, C value) {
            if (!has(index)) {
                return this;
            }
            final FieldAccessor<T> accessor = ACQUIRED_ACCESSORS.get(index);
            if (converter != null) {
                accessor.set(handle, converter.convertDeclaring(value));
            }
            else {
                // T & C er den samme i generic context
                accessor.set(handle, (T) value);
            }
            return this;
        }

        @Override
        public C read(int index) {
            if (!has(index)) {
                return null;
            }
            final FieldAccessor<T> accessor = ACQUIRED_ACCESSORS.get(index);
            if (converter != null) {
                return converter.convertDelegate(accessor.get(handle));
            }
            else {
                // T & C er den samme i generic context
                return (C) accessor.get(handle);
            }
        }

        @Override
        public List<C> readValues() {
            final List<C> list = new ArrayList<>();
            for (int i = 0; i < ACQUIRED_ACCESSORS.size(); i++) {
               list.add(read(i));
            }
            return list;
        }

        @Override
        public boolean has(int index) {
            return ACQUIRED_ACCESSORS.containsKey(index);
        }

        @Override
        public int size() {
            return ACQUIRED_ACCESSORS.size();
        }
    }
}
