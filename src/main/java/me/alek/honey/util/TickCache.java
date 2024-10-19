package me.alek.honey.util;

import me.alek.honey.HoneyPlugin;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class TickCache<T> {

    public static final IntSupplier tickSupplier = () -> HoneyPlugin.get().getExecutionQueueProxy().getCurrentExecutionTick();

    private T reference;
    private int lastTick;

    public T get(Supplier<T> or) {
        if (tickSupplier.getAsInt() == lastTick) {
            return reference != null ? reference : (reference = or.get());
        }

        lastTick = tickSupplier.getAsInt();
        return reference = or.get();
    }
}
