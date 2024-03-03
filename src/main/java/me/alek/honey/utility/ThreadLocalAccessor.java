package me.alek.honey.utility;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ThreadLocalAccessor<T> {

    private final Map<Thread, T> seperatedQueues = new ConcurrentHashMap<>();
    private final Supplier<T> defaultValue;

    public ThreadLocalAccessor(Supplier<T> defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ThreadLocalAccessor() {
        this(null);
    }

    public T get(Thread thread) {
        if (thread == null) return null;

        return seperatedQueues.computeIfAbsent(thread, (d) -> defaultValue == null ? null : defaultValue.get());
    }

    public void forEach(BiConsumer<? super Thread, ? super T> callback) {
        seperatedQueues.forEach(callback);
    }

    public void forEach(Consumer<T> callback) {
        seperatedQueues.forEach((k, v) -> callback.accept(v));
    }

    public T get() {
        return get(Thread.currentThread());
    }

    public Collection<T> getAll() {
        return seperatedQueues.values();
    }

    public synchronized void clear() {
        seperatedQueues.clear();
    }

    public synchronized void set(Thread thread, T value) {
        if (value == null)
            seperatedQueues.remove(thread);
        else
            seperatedQueues.put(thread, value);
    }

    public void set(T value) {
        set(Thread.currentThread(), value);
    }

    public synchronized T remove(Thread thread) {
        return seperatedQueues.remove(thread);
    }

    public T remove() {
        return remove(Thread.currentThread());
    }

    public Thread getAnyThread(Predicate<T> filter) {
        for (Thread thread : seperatedQueues.keySet()) {
            T value = get(thread);
            if (filter.test(value))
                return thread;
        }
        return null;
    }

    public int getSize() {
        return seperatedQueues.size();
    }
}
