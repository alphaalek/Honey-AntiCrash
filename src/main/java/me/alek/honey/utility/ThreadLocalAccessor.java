package me.alek.honey.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ThreadLocalAccessor<T> {

    private final Map<Thread, T> seperatedQueues = new HashMap<>();
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

    public T get() {
        return get(Thread.currentThread());
    }

    public void clear() {
        seperatedQueues.clear();
    }

    public void set(Thread thread, T value) {
        seperatedQueues.put(thread, value);
    }

    public void set(T value) {
        set(Thread.currentThread(), value);
    }

    public void remove(Thread thread) {
        seperatedQueues.remove(thread);
    }

    public void remove() {
        remove(Thread.currentThread());
    }

    public Thread getAnyQueuedThread(Predicate<T> filter) {
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
