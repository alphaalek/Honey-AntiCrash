package me.alek.honey.netty.packet.structure;

import me.alek.honey.netty.packet.structure.converters.TwinConverter;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.utility.reflect.ReflectToucher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    private final Map<Class<?>, ReflectStructure<?, ?>> SUB_TYPE_CACHE = new HashMap<>();
    private final Class<?> handle;
    private TwinConverter<T, C> converter;
    private Map<Integer, FieldAccessor<?>> DEFAULT_ACCESSORS;
    private Map<Integer, MethodInvoker<?>> DEFAULT_INVOKERS;
    private Map<Integer, FieldAccessor<T>> ACQUIRED_ACCESSORS;
    private Map<Integer, MethodInvoker<T>> ACQUIRED_INVOKERS;

    public ReflectStructure(Class<?> delegate) {
        this.handle = delegate;
        initDefaults();
    }

    public ReflectStructure(
            Class<?> delegate,
            Class<?> type,
            TwinConverter<T, C> converter
    ) {
        this.handle = delegate;
        this.converter = converter;

        initDefaults();
        initAcquired(type);
    }

    public void initDefaults() {
        DEFAULT_INVOKERS = new HashMap<>();
        DEFAULT_ACCESSORS = new HashMap<>();
        setDefault(DEFAULT_INVOKERS, false);
        setDefault(DEFAULT_ACCESSORS, true);
    }

    public void initAcquired(Class<?> type) {
        ACQUIRED_ACCESSORS = getAcquiredAccessors(type);
        ACQUIRED_INVOKERS = getAcquiredInvokers(type);
    }

    public void setDefault(
            Map<Integer, ? extends ReflectToucher<?>> defaults,
            boolean isFields
    ) {
        int index = 0;
        Class<?> clazz = handle;

        while (clazz != null && clazz != Object.class) {
            setDefaultSpecific(index, defaults, clazz, isFields);
            clazz = clazz.getSuperclass();
        }
    }

    public Map<Integer, ? extends ReflectToucher<?>> setDefaultSpecific(
            int prevIndex,
            Map<Integer, ? extends ReflectToucher<?>> defaults,
            Class<?> clazz,
            boolean isFields
    ) {
        Object[] handles = (isFields) ? clazz.getDeclaredFields() : clazz.getDeclaredMethods();
        for (Object handle : handles) {
            ReflectToucher<?> toucher = (isFields) ? new FieldAccessor<>((Field) handle) : new MethodInvoker<>((Method) handle);
            defaults.put(prevIndex++, castReflectToucher(toucher));
        }
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, FieldAccessor<T>> getAcquiredAccessors(Class<?> type) {
        return (Map<Integer, FieldAccessor<T>>) getAcquired(type, new HashMap<Integer, FieldAccessor<T>>(), DEFAULT_ACCESSORS);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, MethodInvoker<T>> getAcquiredInvokers(Class<?> type) {
        return (Map<Integer, MethodInvoker<T>>) getAcquired(type, new HashMap<Integer, MethodInvoker<T>>(), DEFAULT_INVOKERS);
    }

    private Map<Integer, ? extends ReflectToucher<T>> getAcquired(
            Class<?> targetType,
            Map<Integer, ? extends ReflectToucher<T>> acquiredMap,
            Map<Integer, ? extends ReflectToucher<?>> defaultMap
    ) {
        int index = 0;
        for (ReflectToucher<?> reflectToucher : defaultMap.values()) {
            if (targetType != reflectToucher.getType()) {
                continue;
            }
            acquiredMap.put(index, castReflectToucher(reflectToucher));
            index++;
        }
        return acquiredMap;
    }

    @SuppressWarnings("unchecked")
    private <R extends ReflectToucher<?>> R castReflectToucher(ReflectToucher<?> reflectToucher) {
        return (R) reflectToucher;
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

    public Collection<MethodInvoker<T>> getInvokers() {
        return ACQUIRED_INVOKERS.values();
    }

    public Collection<FieldAccessor<?>> getDefaultAccessors() {
        return DEFAULT_ACCESSORS.values();
    }

    public Collection<MethodInvoker<?>> getDefaultInvokers() {
        return DEFAULT_INVOKERS.values();
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
        public IStructureModifier<C> writeField(int index, C value) {
            if (!hasField(index)) {
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
        public C readField(int index) {
            if (!hasField(index)) {
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
        public C invokeMethod(int index, Object... params) {
            if (!hasMethod(index)) {
                return null;
            }
            final MethodInvoker<T> invoker = ACQUIRED_INVOKERS.get(index);
            if (converter != null) {
                return converter.convertDelegate(invoker.invoke(handle, params));
            }
            else {
                // T & C er den samme i generic context
                return (C) invoker.invoke(handle, params);
            }
        }

        @Override
        public List<C> readFieldValues() {
            final List<C> list = new ArrayList<>();
            for (int i = 0; i < ACQUIRED_ACCESSORS.size(); i++) {
               list.add(readField(i));
            }
            return list;
        }

        @Override
        public boolean hasField(int index) {
            return ACQUIRED_ACCESSORS.containsKey(index);
        }

        @Override
        public boolean hasMethod(int index) {
            return ACQUIRED_INVOKERS.containsKey(index);
        }

        @Override
        public int fieldSize() {
            return ACQUIRED_ACCESSORS.size();
        }

        @Override
        public int methodSize() {
            return ACQUIRED_INVOKERS.size();
        }
    }
}
