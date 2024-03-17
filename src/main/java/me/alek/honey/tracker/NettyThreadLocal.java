package me.alek.honey.tracker;

import me.alek.honey.utility.Array;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class NettyThreadLocal<T> {

    private final Array<T> seperatedQueues = new Array<>();
    private final Supplier<T> defaultValue;

    public NettyThreadLocal(Supplier<T> defaultValue) {
        this.defaultValue = defaultValue;
    }

    public NettyThreadLocal() {
        this(null);
    }

    public T get(int thread) {
        if (thread == -1) return null;

        return seperatedQueues.get(thread, defaultValue);
    }

    public void forEach(Consumer<T> callback) {
        for (T val : seperatedQueues) {
            callback.accept(val);
        }
    }

    public synchronized void clear() {
        seperatedQueues.clear();
    }

    public boolean set(int thread, T value) {
        if (thread == -1) return false;

        return seperatedQueues.set(thread, value);
    }

    public synchronized boolean remove(int thread) {
        return seperatedQueues.set(thread, null);
    }

    public int getAnyThread(Predicate<T> filter) {
        for (int i = 0; i < seperatedQueues.size(); i++) {
            if (filter.test(seperatedQueues.get(i))) return i;
        }
        return -1;
    }

    public int getSize() {
        return seperatedQueues.size();
    }
}
