package me.alek.honey.util;

import me.alek.honey.HoneyPlugin;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class TickCache<T> {

    public static final IntSupplier TICK = () -> HoneyPlugin.get().getExecutionQueueProxy().getCurrentExecutionTick();

    private T reference;
    private int lastTick;

    public T get(Supplier<T> or) {
        if (TICK.getAsInt() == this.lastTick) {
            return this.reference != null ? this.reference : (this.reference = or.get());
        }

        this.lastTick = TICK.getAsInt();
        return this.reference = or.get();
    }
}
