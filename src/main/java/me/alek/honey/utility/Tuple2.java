package me.alek.honey.utility;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class Tuple2<T1, T2> implements Iterable<Object>, Serializable, Cloneable {

    private static final long serialVersionUID = -3518082018884860684L;

    final T1 t1;
    final T2 t2;

    protected Tuple2(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getT1() {
        return t1;
    }

    public T2 getT2() {
        return t2;
    }

    public <R> Tuple2<R, T2> mapT1(Function<T1, R> mapper) {
        return new Tuple2<>(mapper.apply(t1), t2);
    }

    public <R> Tuple2<T1, R> mapT2(Function<T2, R> mapper) {
        return new Tuple2<>(t1, mapper.apply(t2));
    }

    public Object get(int index) {
        switch (index) {
            case 0:
                return t1;
            case 1:
                return t2;
            default:
                return null;
        }
    }

    public List<Object> toList() {
        return Arrays.asList(toArray());
    }

    public Object[] toArray() {
        return new Object[]{t1, t2};
    }

    @Override
    public Iterator<Object> iterator() {
        return Collections.unmodifiableList(toList()).iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        if (t1 == null) {
            if (tuple2.t1 != null) {
                return false;
            }
        }
        if (t2 == null) {
            if (tuple2.t2 != null) {
                return false;
            }
        }
        if (t1 != null && t2 != null) {
            return t1.equals(tuple2.t1) && t2.equals(tuple2.t2);
        }
        return t1 == tuple2.t1 && t2 == tuple2.t2;
    }

    @Override
    public int hashCode() {
        int result = size();
        if (t1 != null) {
            result = 31 * result + t1.hashCode();
        }
        if (t2 != null) {
            result = 31 * result + t2.hashCode();
        }
        return result * result ^ (result >>> 7);
    }

    public int size() {
        return 2;
    }

    @Override
    public final String toString() {
        return "1=" + t1 + ",2=" + t2;
    }

    @Override
    public Tuple2<T1, T2> clone() {
        try {
            return (Tuple2<T1, T2>) super.clone();
        } catch (Exception ex) {
            throw new AssertionError();
        }
    }
}
