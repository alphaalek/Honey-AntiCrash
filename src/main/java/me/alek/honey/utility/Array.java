package me.alek.honey.utility;

import java.util.function.Supplier;

public class Array<T> {

    private Object[] values = new Object[0];
    private final Object lock = new Object();

    public T get(int index, Supplier<T> or) {
        if (index >= values.length) {
            synchronized (lock) {
                if (index >= values.length) {
                    Object[] copy = values;
                    values = new Object[index + 1];
                    System.arraycopy(copy, 0, values, 0, copy.length);
                }
            }
        }
        if (values[index] == null && or != null) {
            return (T) (values[index] = or.get());
        }

        return (T) values[index];
    }
}
