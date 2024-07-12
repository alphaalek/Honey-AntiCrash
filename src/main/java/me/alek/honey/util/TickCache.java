package me.alek.honey.util;

import java.util.function.Supplier;

public class TickCache<T> {

    private T reference;
    private int lastTick;

    public T get(int currentTick, Supplier<T> or) {
        if (currentTick == lastTick) {
            return reference != null ? reference : (reference = or.get());
        }

        lastTick = currentTick;
        return reference = or.get();
    }
}
