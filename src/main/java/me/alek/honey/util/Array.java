package me.alek.honey.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Array<T> implements Iterable<T> {

    private Object[] data = new Object[0];

    public static <T> Array<T> from(Object[] data) {
        Array<T> array = new Array<>();
        array.data = Arrays.copyOf(data, data.length);
        return array;
    }

    public static <T> Array<T> from(T data) {
        Array<T> array = new Array<>();
        array.set(0, data);
        return array;
    }

    public static <T> Array<T> fromData(T... data) {
        return from(data);
    }

    public static <T, R> Array<T> from(Array<R> array, Function<R, Array<T>> mapper) {
        Array<T> newArray = new Array<>();
        for (R val : array) {
            newArray.addAll(mapper.apply(val));
        }
        return newArray;
    }

    public static <T> Array<T> empty() {
        return new Array<>();
    }

    public Array<T> copy() {
        return Array.from(data);
    }

    public Array<T> with(Predicate<? super T> filter) {
        return Array.from(
                stream().filter(filter).toArray(Object[]::new));
    }

    public Array<T> without(Predicate<? super T> filter) {
        return with(d -> !filter.test(d));
    }

    public <R> Array<R> as(Function<? super T, ? extends R> mapper) {
        return Array.from(
                with(Objects::nonNull)
                        .stream()
                        .map(mapper)
                        .toArray(Object[]::new));
    }

    public Stream<T> stream() {
        return Arrays.stream((T[]) data);
    }

    public Array<T> from(int from, int to) {
        return Array.from(Arrays.copyOfRange(data, from, to));
    }

    public Array<T> from(int from) {
        return from(from, data.length);
    }

    public Array<T> compact() {
        return Array.from(stream().filter(Objects::nonNull).toArray());
    }

    private void resolve(int index) {
        if (index >= data.length) {
            synchronized (this) {
                if (index >= data.length) {
                    Object[] copy = data;
                    data = new Object[index + 1];
                    System.arraycopy(copy, 0, data, 0, copy.length);
                }
            }
        }
    }

    private void resolveDown() {
        synchronized (this) {
            if (data[data.length - 1] != null) {
                return;
            }

            int last = 0;
            for (int i = data.length - 1; i >= 0; i--) {
                if (data[i] != null) {
                    last = i;
                }
            }

            Object[] copy = new Object[last + 1];
            System.arraycopy(data, 0, copy, 0, copy.length);
            data = copy;
        }
    }

    public Object[] toRealArray() {
        return data;
    }

    public T[] toRealArray(IntFunction<T[]> func) {
        return Arrays.stream(data).toArray(func);
    }

    public T poll(int index) {
        T val = get(index);
        if (has(index)) {
            set(index, null);
        }

        return val;
    }

    public T get(int index) {
        return get(index, null);
    }

    public T getFirst() {
        return get(0);
    }

    public T getLast() {
        return isEmpty() ? null : get(data.length - 1);
    }

    public T get(int index, Supplier<T> or) {
        if (index < 0
                || index >= data.length && or == null) {
            return null;
        }

        resolve(index);
        if (data[index] == null && or != null) {
            return (T) (data[index] = or.get());
        }

        return (T) data[index];
    }

    public synchronized void move(int start, int by) {
        resolve(this.data.length + by - 1);
        Object[] data = new Object[this.data.length];
        System.arraycopy(this.data, 0, data, 0, start);
        System.arraycopy(this.data, start, data, start + by, data.length - start - by);
        this.data = data;
    }

    public synchronized void insert(int at, T val) {
        move(at, 1);
        data[at] = val;
    }

    public synchronized void replace(int at, Iterable<T> values) {
        int i = 0;
        for (T val : values) {
            set(at + i++, val);
        }
    }

    public synchronized void add(T val) {
        int index = data.length;
        resolve(index);
        data[index] = val;
    }

    public void addAll(T... values) {
        addAll(Arrays.asList(values));
    }

    public void addAll(Iterable<T> iterable) {
        for (T val : iterable) {
            add(val);
        }
    }

    public T addAndGet(T val) {
        add(val);
        return val;
    }

    public boolean has(int index) {
        if (index >= data.length) return false; // avoid resolving

        return data[index] != null;
    }

    public void clear() {
        data = new Object[0];
    }

    public int size() {
        return data.length;
    }

    public boolean isEmpty() {
        return data.length == 0;
    }

    public synchronized boolean set(int index, T val) {
        resolve(index);

        boolean present = data[index] != null;
        data[index] = val;

        return present;
    }

    public synchronized boolean remove(int index) {
        if (index >= data.length || index < 0) return false;

        boolean removed = set(index, null);
        resolveDown();

        return removed;
    }

    public boolean contains(T val) {
        if (isEmpty()) {
            return false;
        }

        if (data.length == 1) {
            return Objects.equals(val, get(0));
        }

        for (T elem : this) {
            if (Objects.equals(val, elem)) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean removeLast() {
        return remove(data.length - 1);
    }

    @Override
    public Iterator<T> iterator() {
        return (Iterator<T>) Arrays.asList(data).iterator();
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }
}
