package me.alek.honey.netty.structure;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.netty.structure.converters.SetConverter;
import me.alek.honey.util.reflection.*;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class ReflectStructure<T, C> {

    private static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = new HashMap<Class<?>, Class<?>>(){{
        put(int.class, Integer.class);
        put(double.class, Double.class);
        put(float.class, Float.class);
        put(long.class, Long.class);
        put(char.class, Character.class);
        put(byte.class, Byte.class);
        put(boolean.class, Boolean.class);
    }};

    public static Object copyStructure(Object from, Object to) {
        ReflectStructure<Object, Object> structure = CachedReflectStructureProvider.acquireStructure(from.getClass())
                .withoutStatic().without(NMS.getCraftMetaItemClass());

        Iterator<FieldAccessor<Object>> fromIterator = structure.withTarget(from)
                .getAccessors().iterator();
        Iterator<FieldAccessor<Object>> toIterator = structure.withTarget(to)
                .getAccessors().iterator();

        while (toIterator.hasNext() && fromIterator.hasNext()) {
            toIterator.next().set(to, fromIterator.next().get(from));
        }

        return to;
    }

    private final Map<Class<?>, ReflectStructure<?, ?>> SUB_TYPE_CACHE = new HashMap<>();
    private final List<Class<?>> disallowClasses = new ArrayList<>();
    private final Class<?> handle;
    private final Class<?> type;
    private boolean withStatic = true;
    private boolean withSuperClass = true;
    private JavaConverter<T, C> converter;
    private Map<Integer, FieldAccessor<?>> DEFAULT_ACCESSORS;
    private Map<Integer, MethodInvoker<?>> DEFAULT_INVOKERS;
    private Map<Integer, FieldAccessor<T>> ACQUIRED_ACCESSORS;
    private Map<Integer, MethodInvoker<T>> ACQUIRED_INVOKERS;

    public ReflectStructure(Class<?> delegate) {
        this.handle = delegate;
        this.type = null;
        initDefaults();
        initAcquired(null);
    }

    public ReflectStructure(
            Class<?> delegate,
            Class<?> type,
            JavaConverter<T, C> converter
    ) {
        this.handle = delegate;
        this.type = type;
        this.converter = converter;

        initDefaults();
        initAcquired(type);
    }

    private synchronized void initDefaults() {
        DEFAULT_INVOKERS = new HashMap<>();
        DEFAULT_ACCESSORS = new HashMap<>();
        setDefault(DEFAULT_INVOKERS, false);
        setDefault(DEFAULT_ACCESSORS, true);
    }

    private synchronized void initAcquired(Class<?> type) {
        ACQUIRED_ACCESSORS = getAcquiredAccessors(type);
        ACQUIRED_INVOKERS = getAcquiredInvokers(type);
    }

    public synchronized ReflectStructure<T, C> withoutStatic() {
        if (this.withStatic) {
            this.withStatic = false;
            resetIf(this::disallowedMember);
        }
        this.withStatic = false;
        return this;
    }

    public synchronized ReflectStructure<T, C> withoutSuperClass() {
        if (this.withSuperClass) {
            this.withSuperClass = false;
            resetIf(this::disallowedClass);
        }
        this.withSuperClass = false;
        return this;
    }

    public synchronized ReflectStructure<T, C> without(Class<?> handle) {
        if (!this.disallowClasses.contains(handle)) {
            this.disallowClasses.add(handle);
            resetIf(this::disallowedClass);
        }
        this.disallowClasses.add(handle);
        return this;
    }

    private void resetIf(Predicate<IReflectionMember<?>> member) {
        int defaultAccessorsSize = DEFAULT_ACCESSORS.size();
        int acquiredAccessorsSize = ACQUIRED_ACCESSORS.size();
        DEFAULT_ACCESSORS.values().removeIf(member);
        ACQUIRED_ACCESSORS.values().removeIf(member);

        int defaultInvokersSize = DEFAULT_INVOKERS.size();
        int acquiredInvokersSize = ACQUIRED_INVOKERS.size();
        DEFAULT_INVOKERS.values().removeIf(member);
        ACQUIRED_INVOKERS.values().removeIf(member);

        if (defaultAccessorsSize != DEFAULT_ACCESSORS.size()) {
            DEFAULT_ACCESSORS = new HashMap<>();
            setDefault(DEFAULT_ACCESSORS, true);
        }
        if (acquiredAccessorsSize != ACQUIRED_ACCESSORS.size()) {
            ACQUIRED_ACCESSORS = getAcquiredAccessors(type);
        }

        if (defaultInvokersSize != DEFAULT_INVOKERS.size()) {
            DEFAULT_INVOKERS = new HashMap<>();
            setDefault(DEFAULT_INVOKERS, false);
        }
        if (acquiredInvokersSize != ACQUIRED_INVOKERS.size()) {
            ACQUIRED_INVOKERS = getAcquiredInvokers(type);
        }
    }

    public boolean isStaticFieldsAllowed() {
        return withStatic;
    }

    public boolean isSuperClassAllowed() {
        return withSuperClass;
    }

    private boolean disallowedClass(IReflectionMember<?> member) {
        if (!withSuperClass
                && !member.getDeclaringClass().equals(this.handle)) {
            return true;
        }

        if (!disallowClasses.isEmpty()
                && disallowClasses.contains(member.getDeclaringClass())) {
            return true;
        }

        return false;
    }

    private boolean disallowedMember(IReflectionMember<?> member) {
        return !withStatic
                && Modifier.isStatic(member.getModifiers());
    }

    private void setDefault(
            Map<Integer, ? extends IReflectionMember<?>> defaults,
            boolean isFields
    ) {
        AtomicInteger index = new AtomicInteger();
        Class<?> clazz = handle;

        while (clazz != null && !clazz.equals(Object.class)) {
            setDefaultSpecific(index, defaults, clazz, isFields);
            for (Class<?> interfaceType : clazz.getInterfaces()) {
                setDefaultSpecific(index, defaults, interfaceType, isFields);
            }
            clazz = clazz.getSuperclass();
        }
    }

    private boolean exists(Map<Integer, ? extends IReflectionMember<?>> defaults, IReflectionMember<?> member) {
        for (IReflectionMember<?> defaultMember : defaults.values()) {
            if (Objects.equals(defaultMember, member)) {
                return true;
            }
        }

        return false;
    }

    private synchronized void setDefaultSpecific(
            AtomicInteger index,
            Map<Integer, ? extends IReflectionMember<?>> defaults,
            Class<?> clazz,
            boolean isFields
    ) {
        Object[] handles = (isFields)
                ? Reflection.getDeclaredFields(clazz)
                : Reflection.getDeclaredMethods(clazz);

        for (Object handle : handles) {
            IReflectionMember<?> member = (isFields)
                    ? new FieldAccessor<>((Field) handle)
                    : new MethodInvoker<>((Method) handle);

            if (disallowedMember(member)
                    || disallowedClass(member)
                    || exists(defaults, member)) {
                continue;
            }

            defaults.put(index.getAndIncrement(), castReflectionMember(member));
        }
    }

    private Map<Integer, FieldAccessor<T>> getAcquiredAccessors(Class<?> type) {
        return (Map<Integer, FieldAccessor<T>>) getAcquired(type, new HashMap<Integer, FieldAccessor<T>>(), DEFAULT_ACCESSORS);
    }

    private Map<Integer, MethodInvoker<T>> getAcquiredInvokers(Class<?> type) {
        return (Map<Integer, MethodInvoker<T>>) getAcquired(type, new HashMap<Integer, MethodInvoker<T>>(), DEFAULT_INVOKERS);
    }

    private synchronized Map<Integer, ? extends IReflectionMember<T>> getAcquired(
            Class<?> targetType,
            Map<Integer, ? extends IReflectionMember<T>> acquired,
            Map<Integer, ? extends IReflectionMember<?>> defaults
    ) {
        int index = 0;
        for (IReflectionMember<?> member : defaults.values()) {
            if (disallowedMember(member)
                    || disallowedClass(member)
                    || targetType != null && !targetType.isAssignableFrom(member.getType())) {
                continue;
            }

            acquired.put(index, castReflectionMember(member));
            index++;
        }

        return acquired;
    }

    private <R extends IReflectionMember<?>> R castReflectionMember(IReflectionMember<?> member) {
        return (R) member;
    }

    public <R> ReflectStructure<R, R> withWeakType(Class<?> clazz) {
        return (ReflectStructure<R, R>) withType(clazz);
    }

    public <R> ReflectStructure<R, R> withWeakTypeWithoutCache(Class<?> clazz) {
        return (ReflectStructure<R, R>) withTypeWithoutCache(clazz);
    }

    public <R, D> ReflectStructure<R, D> withWeakType(Class<?> clazz, JavaConverter<R, D> converter) {
        return withSpecificType(clazz, converter, true);
    }

    public <R, D> ReflectStructure<R, D> withWeakTypeWithoutCache(Class<?> clazz, JavaConverter<R, D> converter) {
        return withSpecificType(clazz, converter, false);
    }

    public <R> ReflectStructure<R, R> withType(Class<R> clazz) {
        return withSpecificType(clazz, null, true);
    }

    public <R> ReflectStructure<R, R> withTypeWithoutCache(Class<R> clazz) {
        return withSpecificType(clazz, null, false);
    }

    public <R, D> ReflectStructure<R, D> withType(Class<R> clazz, JavaConverter<R, D> converter) {
        return withSpecificType(clazz, converter, true);
    }

    public <R, D> ReflectStructure<R, D> withTypeWithoutCache(Class<R> clazz, JavaConverter<R, D> converter) {
        return withSpecificType(clazz, converter, false);
    }

    public <R, D, V> ReflectStructure<R, D> withSpecificType(Class<V> clazz, JavaConverter<R, D> converter, boolean withCache) {
        if (withCache) {
            return (ReflectStructure<R, D>) SUB_TYPE_CACHE.computeIfAbsent(clazz, d -> withSpecificType(clazz, converter, false));
        } else {
            return new ReflectStructure<>(handle, clazz, converter);
        }
    }

    public ReflectStructure<Integer, Integer> withInts() {
        return withSpecificType(int.class, null, true);
    }

    public ReflectStructure<Short, Short> withShorts() {
        return withSpecificType(short.class, null, true);
    }

    public ReflectStructure<Byte, Byte> withBytes() {
        return withSpecificType(byte.class, null, true);
    }

    public ReflectStructure<Long, Long> withLongs() {
        return withSpecificType(long.class, null, true);
    }

    public ReflectStructure<Double, Double> withDoubles() {
        return withSpecificType(double.class, null, true);
    }

    public ReflectStructure<Float, Float> withFloats() {
        return withSpecificType(float.class, null, true);
    }

    public ReflectStructure<Boolean, Boolean> withBooleans() {
        return withSpecificType(boolean.class, null, true);
    }

    public ReflectStructure<Void, Void> withVoids() {
        return withSpecificType(void.class, null, true);
    }

    public ReflectStructure<String, String> withStrings() {
        return withSpecificType(String.class, null, true);
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

    public IStructureModifier<C> withTarget(Object handle, Object... params) {
        return new StructureModifier(handle, converter, params);
    }

    @SuppressWarnings("unchecked")
    private class StructureModifier implements IStructureModifier<C> {

        private final Object handle;
        private final JavaConverter<T, C> converter;
        private final Object[] params;

        public StructureModifier(Object handle, JavaConverter<T, C> converter, Object... params) {
            this.handle = handle;
            this.converter = converter;
            this.params = params;
        }

        public ReflectStructure<?, C> getStructure() {
            return ReflectStructure.this;
        }

        @Override
        public Object getHandle() {
            return handle;
        }

        @Override
        public JavaConverter<T, C> getConverter() {
            return converter;
        }

        @Override
        public Collection<C> readFieldValues() {
            return readFieldEntries().values();
        }

        @Override
        public Map<String, C> readFieldEntries() {
            Map<String, C> entries = new HashMap<>();
            for (FieldAccessor<T> accessor : ACQUIRED_ACCESSORS.values()) {
                entries.put(accessor.getName(), get(accessor));
            }

            return entries;
        }

        @Override
        public boolean hasField(int index) {
            return ACQUIRED_ACCESSORS.containsKey(index);
        }

        @Override
        public boolean hasField(String name, int index) {
            int specificIndex = 0;
            for (FieldAccessor<T> accessor : ACQUIRED_ACCESSORS.values()) {
                if (accessor.getName().equals(name) && index == specificIndex++) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public FieldAccessor<C> getField(int index) {
            if (!hasField(index)) {
                return null;
            }

            return (FieldAccessor<C>) ACQUIRED_ACCESSORS.get(index);
        }

        public <O> FieldAccessor<C> getField(AtomicReference<FieldAccessor<O>> fieldReference, String name, int index) {
            return getField(fieldReference, (i, field) -> field.getName().equals(name) && i.getAndIncrement() == index);
        }

        public <O> FieldAccessor<C> getField(AtomicReference<FieldAccessor<O>> fieldReference, BiPredicate<AtomicInteger, FieldAccessor<?>> filter) {
            FieldAccessor<O> cachedAccessor;
            if (fieldReference != null && (cachedAccessor = fieldReference.get()) != null) {
                return (FieldAccessor<C>) cachedAccessor;
            }

            AtomicInteger index = new AtomicInteger();
            for (FieldAccessor<T> accessor : ACQUIRED_ACCESSORS.values()) {
                if (filter.test(index, accessor)) {

                    if (fieldReference != null) {
                        fieldReference.set((FieldAccessor<O>) accessor);
                    }

                    return (FieldAccessor<C>) accessor;
                }
            }

            return null;
        }

        private void set(FieldAccessor<T> accessor, C value) {
            if (accessor == null) {
                return;
            }

            if (converter != null) {
                accessor.set(handle, converter.convertDeclaring(value));
            } else {
                accessor.set(handle, (T) value);
            }
        }

        @Override
        public IStructureModifier<C> writeField(int index, C value) {
            set((FieldAccessor<T>) getField(index), value);
            return this;
        }

        @Override
        public <O> IStructureModifier<C> writeField(AtomicReference<FieldAccessor<O>> fieldReference, String name, int index, C value) {
            set((FieldAccessor<T>) getField(fieldReference, name, index), value);
            return this;
        }

        @Override
        public <O> IStructureModifier<C> writeField(AtomicReference<FieldAccessor<O>> fieldReference, BiPredicate<AtomicInteger, FieldAccessor<?>> filter, C value) {
            set((FieldAccessor<T>) getField(fieldReference, filter), value);
            return this;
        }

        private C get(FieldAccessor<T> accessor) {
            if (accessor == null) {
                return null;
            }

            if (converter != null) {
                return converter.convertDelegate(accessor.get(handle), params);
            } else {
                return (C) accessor.get(handle);
            }
        }

        @Override
        public <K, V, R> C readFieldAndApplyMapping(int index) {
            if (!hasField(index)) {
                return null;
            }

            if (converter == null) { // what should it apply to?
                return readField(index);
            }

            FieldAccessor<T> accessor = ACQUIRED_ACCESSORS.get(index);
            if (accessor != null) {
                T originalValue = accessor.get(handle);

                if (converter instanceof MapConverter && originalValue instanceof Map) {
                    MapConverter<K, V, R> mapConverter = (MapConverter<K, V, R>) converter;
                    Map<K, V> declaring = new HashMap<>((Map<K, V>) originalValue);
                    accessor.set(handle, (T) declaring);
                    mapConverter.apply(declaring);

                    if (mapConverter.isInvalid()) {
                        return null;
                    }
                }

                if (converter instanceof ListConverter && originalValue instanceof List) {
                    ListConverter<V, R> listConverter = (ListConverter<V, R>) converter;
                    List<V> declaring = new ArrayList<>((List<V>) originalValue);
                    accessor.set(handle, (T) declaring);
                    listConverter.apply(declaring);

                    if (listConverter.isInvalid()) {
                        return null;
                    }
                }

                if (converter instanceof SetConverter && originalValue instanceof Set) {
                    SetConverter<V, R> setConverter = (SetConverter<V, R>) converter;
                    Set<V> declaring = new HashSet<>((Set<V>) originalValue);
                    accessor.set(handle, (T) declaring);
                    setConverter.apply(declaring);

                    if (setConverter.isInvalid()) {
                        return null;
                    }
                }

                return converter.convertDelegate(originalValue, params);
            }

            return null;
        }

        @Override
        public <O> C readField(AtomicReference<FieldAccessor<O>> fieldReference, String name, int index) {
            return get((FieldAccessor<T>) getField(fieldReference, name, index));
        }

        @Override
        public <O> C readField(AtomicReference<FieldAccessor<O>> fieldReference, BiPredicate<AtomicInteger, FieldAccessor<?>> filter) {
            return get((FieldAccessor<T>) getField(fieldReference, filter));
        }

        @Override
        public C readField(int index) {
            return get((FieldAccessor<T>) getField(index));
        }

        private C invoke(MethodInvoker<T> invoker, Object... params) throws Exception {
            if (invoker == null) {
                return null;
            }

            if (converter != null) {
                return converter.convertDelegate(invoker.invokeWithException(handle, params), params);
            } else {
                return (C) invoker.invokeWithException(handle, params);
            }
        }

        @Override
        public <O> C invokeMethod(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Object... params) {
            try {
                return invokeMethodWithException(methodReference, filter, params);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Reflection error (method invoke): " + ex.getMessage());
            }
        }

        @Override
        public <O> C invokeMethod(AtomicReference<MethodInvoker<O>> methodReference, int index, Object... params) {
            try {
                return invokeMethodWithException(methodReference, index, params);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Reflection error (method invoke): " + ex.getMessage());
            }
        }

        @Override
        public <O> C invokeMethod(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Object... params) {
            try {
                return invokeMethodWithException(methodReference, name, index, params);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Reflection error (method invoke): " + ex.getMessage());
            }
        }

        @Override
        public <O> C invokeMethodWithException(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Object... params) throws Exception {
            return invoke((MethodInvoker<T>) getMethod(methodReference, filter, () -> getParamClasses(params)), params);
        }

        @Override
        public boolean hasMethod(int index) {
            return ACQUIRED_INVOKERS.containsKey(index);
        }

        @Override
        public boolean hasMethod(String name, int index) {
            int specificIndex = 0;
            for (MethodInvoker<T> accessor : ACQUIRED_INVOKERS.values()) {
                if (accessor.getName().equals(name) && index == specificIndex++) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public <O> MethodInvoker<C> getMethod(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Supplier<Class<?>[]> params) {
            return getMethod(methodReference, (i, invoker) -> invoker.getName().equals(name) && i.getAndIncrement() == index, params);
        }

        @Override
        public <O> MethodInvoker<C> getMethod(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Class<?>... params) {
            return getMethod(methodReference, name, index, () -> params);
        }

        @Override
        public <O> MethodInvoker<C> getMethod(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Supplier<Class<?>[]> params) {
            MethodInvoker<O> cachedInvoker;
            if (methodReference != null && (cachedInvoker = methodReference.get()) != null) {
                return (MethodInvoker<C>) cachedInvoker;
            }

            AtomicInteger index = new AtomicInteger();
            Class<?>[] paramClasses = params.get();
            for (MethodInvoker<T> invoker : ACQUIRED_INVOKERS.values()) {
                if (matchParams(paramClasses, getWrapperClasses(invoker.getHandle().getParameterTypes()))
                        && filter.test(index, invoker)) {

                    if (methodReference != null) {
                        methodReference.set((MethodInvoker<O>) invoker);
                    }

                    return (MethodInvoker<C>) invoker;
                }
            }

            return null;
        }

        @Override
        public <O> MethodInvoker<C> getMethod(AtomicReference<MethodInvoker<O>> methodReference, BiPredicate<AtomicInteger, MethodInvoker<?>> filter, Class<?>... params) {
            return getMethod(methodReference, filter, () -> params);
        }

        public <O> MethodInvoker<C> getMethod(AtomicReference<MethodInvoker<O>> methodReference, int index, Supplier<Class<?>[]> params) {
            MethodInvoker<O> cachedInvoker;
            if (methodReference != null && (cachedInvoker = methodReference.get()) != null) {
                return (MethodInvoker<C>) cachedInvoker;
            }

            if (!hasMethod(index)) {
                return null;
            }

            MethodInvoker<O> invoker = (MethodInvoker<O>) ACQUIRED_INVOKERS.get(index);
            if (!matchParams(params.get(), getWrapperClasses(invoker.getHandle().getParameterTypes()))) {
                return null;
            }

            if (methodReference != null) {
                methodReference.set(invoker);
            }

            return (MethodInvoker<C>) invoker;
        }

        @Override
        public <O> MethodInvoker<C> getMethod(AtomicReference<MethodInvoker<O>> methodReference, int index, Class<?>... params) {
            return getMethod(methodReference, index, () -> params);
        }

        private boolean matchParams(Class<?>[] params, Class<?>[] methodParams) {
            if (params.length != methodParams.length) {
                return false;
            }

            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    continue;
                }

                if (!methodParams[i].isAssignableFrom(params[i])) {
                    Bukkit.getLogger().info("Doesn't match " + methodParams[i] + " " + params[i]);
                    return false;
                }
            }

            return true;
        }

        private Class<?>[] getParamClasses(Object... params) {
            return Arrays.stream(params).filter(Objects::nonNull).map(Object::getClass).toArray(Class[]::new);
        }

        private Class<?>[] getWrapperClasses(Class<?>[] classes) {
            return Arrays.stream(classes).map(clazz -> WRAPPER_CLASSES.getOrDefault(clazz, clazz)).toArray(Class[]::new);
        }

        @Override
        public <O> C invokeMethodWithException(AtomicReference<MethodInvoker<O>> methodReference, int index, Object... params) throws Exception {
            return invoke((MethodInvoker<T>) getMethod(methodReference, index, () -> getParamClasses(params)), params);
        }

        @Override
        public <O> C invokeMethodWithException(AtomicReference<MethodInvoker<O>> methodReference, String name, int index, Object... params) throws Exception {
            return invoke((MethodInvoker<T>) getMethod(methodReference, name, index, () -> getParamClasses(params)), params);
        }

        @Override
        public int fieldSize() {
            return ACQUIRED_ACCESSORS.size();
        }

        @Override
        public int methodSize() {
            return ACQUIRED_INVOKERS.size();
        }

        @Override
        public <O> Collection<FieldAccessor<O>> getAccessors() {
            return (Collection<FieldAccessor<O>>) (Collection<?>) ACQUIRED_ACCESSORS.values();
        }

        @Override
        public <O> Collection<MethodInvoker<O>> getInvokers() {
            return (Collection<MethodInvoker<O>>) (Collection<?>) ACQUIRED_INVOKERS.values();
        }
    }
}
