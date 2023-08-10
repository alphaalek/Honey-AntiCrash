package me.alek.honey.utility;

public class MutableTuple2<T1, T2> extends Tuple2<T1, T2> {

    public MutableTuple2(T1 t1, T2 t2) {
        super(t1, t2);
    }

    public void setT1(T1 value) { t1 = value; }

    public void setT2(T2 value) { t2 = value; }
}
