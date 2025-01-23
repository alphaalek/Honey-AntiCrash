package me.alek.honey.util;

import java.util.function.Supplier;

public class LazyInit<T> {

    private T val;
    private boolean initialized;
    private final Supplier<T> function;

    public LazyInit(Supplier<T> function) {
        this.function = function;
    }

    public T get() {
        if (!this.initialized) {
            this.initialized = true;
            this.val = this.function.get();
        }
        return this.val;
    }

    public boolean initialized() {
        return this.initialized;
    }
}
