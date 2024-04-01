package me.alek.honey.utility;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Supplier;

public class Array<T> implements Iterable<T> {

    private Object[] values = new Object[0];

    private void resolve(int index) {
        if (index >= values.length) {
            synchronized (this) {
                if (index >= values.length) {
                    Object[] copy = values;
                    values = new Object[index + 1];
                    System.arraycopy(copy, 0, values, 0, copy.length);
                }
            }
        }
    }

    private void resolveDown() {
        synchronized (this) {
            if (values[values.length - 1] != null) {
                return;
            }

            int last = 0;
            for (int i = values.length - 1; i >= 0; i--) {
                if (values[i] != null) {
                    last = i;
                }
            }

            Object[] copy = new Object[last + 1];
            System.arraycopy(values, 0, copy, 0, copy.length);
            values = copy;
        }
    }

    public Object[] toRealArray() {
        return values;
    }

    public T get(int index) {
        return get(index, null);
    }

    public T get(int index, Supplier<T> or) {
        resolve(index);
        if (values[index] == null && or != null) {
            return (T) (values[index] = or.get());
        }

        return (T) values[index];
    }

    public synchronized void add(T val) {
        int index = size();
        resolve(index);
        values[index] = val;
    }

    public boolean has(int index) {
        if (index >= values.length) return false; // avoid resolving

        return values[index] != null;
    }

    public void clear() {
        values = new Object[0];
    }

    public int size() {
        return values.length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public synchronized boolean set(int index, T val) {
        resolve(index);

        boolean present = values[index] != null;
        values[index] = val;

        return present;
    }

    public synchronized boolean remove(int index) {
        if (index >= values.length) return false;

        boolean removed = set(index, null);
        resolveDown();

        return removed;
    }

    @Override
    public Iterator<T> iterator() {
        return (Iterator<T>) Arrays.asList(values).iterator();
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
