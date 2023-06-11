package me.alek.honey.netty.packet.structure;

import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.netty.api.packet.PacketModifier;
import me.alek.honey.netty.api.packet.PacketStructure;

import java.lang.reflect.Field;
import java.util.*;

public class InternalPacketStructure<T> implements PacketStructure<T> {

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

    private final Class<?> nmsClass;
    private Map<Integer, FieldAccessor<?>> DEFAULT_ACCESSORS;
    private Map<Integer, FieldAccessor<T>> ACQUIRED_ACCESSORS;
    private final Map<Class<?>, PacketStructure<?>> SUB_TYPE_CACHE = new HashMap<>();

    public InternalPacketStructure(Class<?> nmsClass) {
        this.nmsClass = nmsClass;
        setDefaultFields();
    }

    public InternalPacketStructure(
            Class<?> nmsClass,
            Class<?> type,
            Map<Integer, FieldAccessor<?>> accessors
    ) {
        this.nmsClass = nmsClass;
        setDefaultFields();
        ACQUIRED_ACCESSORS = getAcquiredFields(accessors.values(), type);
    }

    private void setDefaultFields() {
        final Map<Integer, FieldAccessor<?>> defaults = new HashMap<>();

        int index = 0;
        Class<?> clazz = nmsClass;

        while (GENERAL_PACKET_CLASS.isAssignableFrom(clazz)) {
            setDefaultFields(index, defaults, clazz);
            clazz = clazz.getSuperclass();
        }
        DEFAULT_ACCESSORS = defaults;
    }

    private Map<Integer, FieldAccessor<?>> setDefaultFields(
            int prevIndex,
            Map<Integer, FieldAccessor<?>> defaults,
            Class<?> nmsClass
    ) {
        for (Field field : nmsClass.getDeclaredFields()) {
            FieldAccessor<?> accessor = new FieldAccessor<>(field);
            defaults.put(prevIndex++, accessor);
        }
        return defaults;
    }

    private Map<Integer, FieldAccessor<T>> getAcquiredFields(Collection<FieldAccessor<?>> accessors, Class<?> type) {
        int index = 0;
        Map<Integer, FieldAccessor<T>> acquired = new HashMap<>();
        for (FieldAccessor<?> accessor : accessors) {
            if (type != accessor.getType()) {
                continue;
            }
            acquired.put(index, (FieldAccessor<T>) accessor);
            index++;
        }
        return acquired;
    }

    public <R> PacketStructure<R> withType(Class<R> clazz) {
        if (clazz == null) {
             return null;
        }
        PacketStructure<R> packetStructure = (PacketStructure<R>) SUB_TYPE_CACHE.get(clazz);
        if (packetStructure == null) {
            packetStructure = new InternalPacketStructure<>(nmsClass, clazz, DEFAULT_ACCESSORS);
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

    public PacketStructureModifier<T> withTarget(Object rawPacket) {
        return new PacketStructureModifier<>(rawPacket);
    }

    private class PacketStructureModifier<R> implements PacketModifier<R> {

        private final Object handle;

        public PacketStructureModifier(Object handle) {
            this.handle = handle;
        }

        public PacketModifier<R> write(int index, R value) {
            if (!has(index)) {
                return this;
            }
            FieldAccessor<R> accessor = (FieldAccessor<R>) ACQUIRED_ACCESSORS.get(index);
            accessor.set(handle, value);
            return this;
        }

        public R read(int index) {
            if (!has(index)) {
                return null;
            }
            FieldAccessor<R> accessor = (FieldAccessor<R>) ACQUIRED_ACCESSORS.get(index);
            return accessor.get(handle);
        }

        @Override
        public List<R> readValues() {
            final List<R> list = new ArrayList<>();
            for (FieldAccessor<T> accessor : ACQUIRED_ACCESSORS.values()) {
                list.add(((FieldAccessor<R>)accessor).get(handle));
            }
            return list;
        }

        @Override
        public boolean has(int index) {
            return ACQUIRED_ACCESSORS.containsKey(index);
        }
    }
}
